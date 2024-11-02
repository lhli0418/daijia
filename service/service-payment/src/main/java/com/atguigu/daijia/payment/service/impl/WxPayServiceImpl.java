package com.atguigu.daijia.payment.service.impl;



import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.service.RabbitService;
import com.atguigu.daijia.common.util.RequestUtils;
import com.atguigu.daijia.driver.client.DriverAccountFeignClient;
import com.atguigu.daijia.model.entity.payment.PaymentInfo;
import com.atguigu.daijia.model.enums.TradeType;
import com.atguigu.daijia.model.form.driver.TransferForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.order.OrderRewardVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.payment.config.WxPayV3Properties;
import com.atguigu.daijia.payment.mapper.PaymentInfoMapper;
import com.atguigu.daijia.payment.service.WxPayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.atguigu.daijia.payment.config.WxPayV3Properties;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private WxPayV3Properties wxPayV3Properties;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private DriverAccountFeignClient driverAccountFeignClient;

    /**
     * 
     * @param paymentInfoForm
     * @return
     */
    @Override
    public WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm) {

        try {
            // 1.添加支付记录到支付表里
            // 判断：如果表中存在订单支付记录，不需要添加
            PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, paymentInfoForm.getOrderNo()));
            if(null == paymentInfo) {
                paymentInfo = new PaymentInfo();
                BeanUtils.copyProperties(paymentInfoForm, paymentInfo);
                paymentInfo.setPaymentStatus(0);
                paymentInfoMapper.insert(paymentInfo);
            }
            // 2.创建微信支付使用对象 构建service
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(wxPayV3Properties.getConfig()).build();

            // 3.request.setXxx(val)设置所需参数，具体参数可见Request定义
            PrepayRequest request = new PrepayRequest();
            Amount amount = new Amount();
            // 金额
//            amount.setTotal(paymentInfoForm.getAmount().multiply(new BigDecimal(100)).intValue());
            amount.setTotal(1); // TODO 为了测试，支付1分钱
            request.setAmount(amount);
            request.setAppid(wxPayV3Properties.getAppid());
            request.setMchid(wxPayV3Properties.getMerchantId());
            //string[1,127]
            String description = paymentInfo.getContent();
            if(description.length() > 127) {
                description = description.substring(0, 127);
            }
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Properties.getNotifyUrl());
            request.setOutTradeNo(paymentInfo.getOrderNo());

            //获取用户信息
            Payer payer = new Payer();
            payer.setOpenid(paymentInfoForm.getCustomerOpenId());
            request.setPayer(payer);

            //是否指定分账，不指定不能分账
            SettleInfo settleInfo = new SettleInfo();
            settleInfo.setProfitSharing(true);
            request.setSettleInfo(settleInfo);

            // 4.调用微信支付 使用对象里面方法实现微信支付调用
            // 调用下单方法，得到应答
            // response包含了调起支付所需的所有参数，可直接用于前端调起支付
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            log.info("微信支付下单返回参数：{}", JSON.toJSONString(response));
            // 5.返回结果，封装到WxPrepayVo里面
            WxPrepayVo wxPrepayVo = new WxPrepayVo();
            BeanUtils.copyProperties(response, wxPrepayVo);
            wxPrepayVo.setTimeStamp(response.getTimeStamp());
            return wxPrepayVo;
        } catch (Exception e){
            e.printStackTrace();
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    /**
     * 支付成功，微信平台回调通知
     * @param request
     */
    @Transactional
    @Override
    public void wxnotify(HttpServletRequest request) {
        //1.回调通知的验签与解密
        //从request头信息获取参数
        //HTTP 头 Wechatpay-Signature
        // HTTP 头 Wechatpay-Nonce
        //HTTP 头 Wechatpay-Timestamp
        //HTTP 头 Wechatpay-Serial
        //HTTP 头 Wechatpay-Signature-Type
        //HTTP 请求体 body。切记使用原始报文，不要用 JSON 对象序列化后的字符串，避免验签的 body 和原文不一致。
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signature = request.getHeader("Wechatpay-Signature");
        String requestBody = RequestUtils.readData(request);
        log.info("wechatPaySerial：{}", wechatPaySerial);
        log.info("nonce：{}", nonce);
        log.info("timestamp：{}", timestamp);
        log.info("signature：{}", signature);
        log.info("requestBody：{}", requestBody);

        //2.构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(requestBody)
                .build();


        //3.初始化 NotificationParser
        NotificationParser parser = new NotificationParser(wxPayV3Properties.getConfig());
        //4.以支付通知回调为例，验签、解密并转换成 Transaction
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        log.info("成功解析：{}", JSON.toJSONString(transaction));
        if(null != transaction && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
            //5.处理支付业务
            this.handlePayment(transaction);
        }
    }

    /**
     * 支付状态查询
     * 微信支付调用成功与失败 不能够完全依据前端 success，fail 回调函数来进行判断 ,
     * 判断是否支付成功，由后端进行判断。因此在发起支付后，乘客端小程序要轮询查询后端是否支付成功。
     * @param orderNo
     * @return
     */
    @Override
    public Boolean queryPayStatus(String orderNo) {

        // 构建微信支付操作对象 service
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(wxPayV3Properties.getConfig()).build();

        // QueryOrderByOutTradeNoRequest对象设置参数
        QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
        // 商户号
        queryRequest.setMchid(wxPayV3Properties.getMerchantId());
        // 订单编号
        queryRequest.setOutTradeNo(orderNo);

        try {
            // 调用微信支付操作对象service中方法实现查询操作
            Transaction transaction = service.queryOrderByOutTradeNo(queryRequest);
            log.info(JSON.toJSONString(transaction));

            // 查询返回结果，根据结果判断
            if (null != transaction && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS){
                // 支付成功 调用其它支付成功后的业务逻辑
                this.handlePayment(transaction);
                return true;
            }
        } catch (ServiceException e){
            // API返回失败, 例如ORDER_NOT_EXISTS
            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
        }

        return false;
    }

    /**
     * 支付成功后续处理方法
     * @param transaction
     */
    public void handlePayment(Transaction transaction) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, transaction.getOutTradeNo()));
        if (paymentInfo.getPaymentStatus() == 1) {
            return;
        }

        //更新支付信息
        paymentInfo.setPaymentStatus(1);
        paymentInfo.setOrderNo(transaction.getOutTradeNo());
        paymentInfo.setTransactionId(transaction.getTransactionId());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString(transaction));
        paymentInfoMapper.updateById(paymentInfo);
        // 表示交易成功！

        // 使用消息队列
        // 发送端：发送mq消息，传递 订单编号
        // 接受端：获取订单编号，完成后续处理
        // 交换机 路由 订单编号
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, MqConst.ROUTING_PAY_SUCCESS, paymentInfo.getOrderNo());
    }

    /**
     * 乘客支付成功后续业务处理
     * @param orderNo
     */
    
    @Override
    public void handleOrder(String orderNo) {
        //1.更改订单支付状态
        orderInfoFeignClient.updateOrderPayStatus(orderNo);

        //2.处理系统奖励，打入司机账户
        OrderRewardVo orderRewardVo = orderInfoFeignClient.getOrderRewardFee(orderNo).getData();
        if(null != orderRewardVo.getRewardFee() && orderRewardVo.getRewardFee().doubleValue() > 0) {
            TransferForm transferForm = new TransferForm();
            transferForm.setTradeNo(orderNo);
            transferForm.setTradeType(TradeType.REWARD.getType());
            transferForm.setContent(TradeType.REWARD.getContent());
            transferForm.setAmount(orderRewardVo.getRewardFee());
            transferForm.setDriverId(orderRewardVo.getDriverId());
            driverAccountFeignClient.transfer(transferForm);
        }

        //3.TODO分账
    }
}
