package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.order.TextAuditingVo;

public interface CiService {

    /**
     * 封装图片审核接口
     * @param path
     * @return
     */
    Boolean imageAuditing(String path);

    TextAuditingVo textAuditing(String content);
}
