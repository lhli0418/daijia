package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.customer.config.WxConfigOperator;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    @Autowired
    private CustomerLoginLogMapper customerLoginLogMapper;

    /**
     * 微信小程序登录接口
     * @param code 微信小程序端传入登录凭证
     * @return 用户id
     */
    @Override
    public Long login(String code) {
        // 1 获取code值,使用微信工具包对象,获取微信唯一标识openid
        String openid = null;
        try {
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(code);
            openid = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
        // 2 根据openid查询数据库,判断是否第一次登录
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerInfo::getWxOpenId,openid);

        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);
        // 3 如果第一次登录,添加信息到用户表
        if (customerInfo == null) {
           CustomerInfo customerInfo1 = new CustomerInfo();
            // 添加名字 根据时间戳生成名字
            customerInfo1.setNickname(String.valueOf(System.currentTimeMillis()));
            // 添加头像
            customerInfo1.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            // 添加openid
            customerInfo1.setWxOpenId(openid);
            customerInfoMapper.insert(customerInfo1);
        }
        // 4 记录登录日志信息

        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        customerLoginLogMapper.insert(customerLoginLog);
        // 5 返回用户id

        return customerInfo.getId();
    }

    /**
     * 根据用户id查询用户信息
     * @param customerId
     * @return
     */
    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);

        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        // beanUtilS.copyProperties() 对应信息自动拷贝
        BeanUtils.copyProperties(customerInfo,customerLoginVo);

        // 判断是否绑定手机号码，如果未绑定，小程序端发起绑定事件
        boolean isBindPhone = StringUtils.hasText(customerInfo.getPhone());

        customerLoginVo.setIsBindPhone(isBindPhone);
        return customerLoginVo;

    }
}
