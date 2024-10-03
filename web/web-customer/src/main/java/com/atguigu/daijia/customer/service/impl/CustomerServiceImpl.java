package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {

    //注入远程调用接口
    @Autowired
    private CustomerInfoFeignClient customerInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public String login(String code) {
        // 1.用code进行远程调用,获取用户id对应响应码
        Result<Long> loginResult = customerInfoFeignClient.login(code);
        Integer codeResult = loginResult.getCode();

        // 2.判断如果返回失败了,返回错误提示
        // 200正常
        if (codeResult != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 3.获取用户id
        Long customerId = loginResult.getData();

        // 4.判断返回id是否为空,如果为空,返回错位提示
        if (customerId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 5.生成token字符串 UUID / jwt工具包
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        // 6.把用户id放入Redis,设置过期时间
        // key:token value:customerId
//        redisTemplate.opsForValue().set(token,customerId.toString(),30,
//                TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX+token,
                                        customerId.toString(),
                                        RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                                        TimeUnit.SECONDS);

        // 7.返回token
        return token;
    }
}
