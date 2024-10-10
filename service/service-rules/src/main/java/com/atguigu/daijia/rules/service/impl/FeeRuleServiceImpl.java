package com.atguigu.daijia.rules.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.rules.mapper.FeeRuleMapper;
import com.atguigu.daijia.rules.service.FeeRuleService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class FeeRuleServiceImpl implements FeeRuleService {

    @Autowired
    private KieContainer kieContainer;

    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm) {
        // 封装传入对象
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setDistance(calculateOrderFeeForm.getDistance());
        feeRuleRequest.setStartTime(new DateTime(calculateOrderFeeForm.getStartTime()).toString("HH:mm:ss"));
        feeRuleRequest.setWaitMinute(calculateOrderFeeForm.getWaitMinute());
        log.info("传入参数：{}", JSON.toJSONString(feeRuleRequest));

        // 开启会话
        KieSession kieSession = kieContainer.newKieSession();

        // 封装返回对象
        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        kieSession.setGlobal("feeRuleResponse",feeRuleResponse);

        // 设置订单对象
        kieSession.insert(feeRuleRequest);

        // 触发规则
        kieSession.fireAllRules();

        // 终止会话
        kieSession.dispose();

        log.info("计算结果：{}", JSON.toJSONString(feeRuleResponse));

        // 封装返回对象
        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        BeanUtils.copyProperties(feeRuleResponse,feeRuleResponseVo);
        return feeRuleResponseVo;
    }
}
