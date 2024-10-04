package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;

public interface CustomerService {


    /**
     * 微信小程序登录授权
     * @param code
     * @return 返回token令牌
     */
    String login(String code);

    CustomerLoginVo getCustomerLoginInfo(Long customerId);
}
