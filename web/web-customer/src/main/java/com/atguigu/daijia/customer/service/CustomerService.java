package com.atguigu.daijia.customer.service;

public interface CustomerService {


    /**
     * 微信小程序登录授权
     * @param code
     * @return 返回token令牌
     */
    String login(String code);
}
