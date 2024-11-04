package com.atguigu.daijia.order.handle;

import com.atguigu.daijia.order.service.OrderInfoService;
import io.lettuce.core.RedisClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ClassName: RedisDelayHandle
 * package: com.atguigu.daijia.order.handle
 * Description:
 *
 * @Author lh
 * @Create 2024/11/3 18:21
 * @Version 1.0
 */
@Slf4j
@Component
public class RedisDelayHandle {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void listener(){
        new Thread(() -> {
            while (true) {
                // 获取阻塞队列
                RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque("queue_cancel");
                try {
                    // 从阻塞队列中获取订单id
                    String orderId = blockingDeque.take();
                    // 订单Id 不为空的时候，调用取消订单方法
                    if (!StringUtils.isEmpty(orderId)) {
                        log.info("接收延时队列成功，订单id：{}", orderId);
                        orderInfoService.orderCancel(Long.parseLong(orderId));
                    }
                } catch (InterruptedException e) {
                    log.error("接收延时队列失败");
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
