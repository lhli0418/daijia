package com.atguigu.daijia.payment.receiver;

import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.payment.service.WxPayService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ClassName: PaymentReceiver
 * package: com.atguigu.daijia.payment.receiver
 * Description:
 *
 * @Author lh
 * @Create 2024/11/1 12:08
 * @Version 1.0
 */
@Slf4j
@Component
public class PaymentReceiver {

    @Autowired
    private WxPayService wxPayService;

    /**
     *
     * 订单支付成功，处理支付回调
     * @param orderNo
     * @param message
     * @param channel
     * @throws IOException
     *      durable true 可持续化
     *      exchange 交换机
     *      key 路由key
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAY_SUCCESS, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER),
            key = {MqConst.ROUTING_PAY_SUCCESS}
    ))
    public void paySuccess(String orderNo, Message message, Channel channel) throws IOException {
        wxPayService.handleOrder(orderNo);
        // channel.basicAck方法用于确认已经接收并处理了消息
        // long deliveryTag 消息的唯一标识。每条消息都有自己的ID号，用于标识该消息在channel中的顺序。
        // 当消费者接收到消息后，需要调用channel.basicAck方法并传递deliveryTag来确认消息的处理
        // boolean multiple 是否批量确认消息，当传false时，只确认当前 deliveryTag对应的消息;
        // 当传true时，会确认当前及之前所有未确认的消息。
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
