package com.atguigu.daijia.dispatch.xxl.job;

import com.alibaba.nacos.common.utils.ExceptionUtil;
import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ClassName: JobHandler
 * package: com.atguigu.daijia.dispatch.xxl.job
 * Description:
 *
 * @Author lh
 * @Create 2024/10/16 19:06
 * @Version 1.0
 */
@Component
@Slf4j
public class JobHandler {

    @Autowired
    private NewOrderService newOrderService;

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;
    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler(){
        log.info("新订单调度任务方法：{}", XxlJobHelper.getJobId());
        // xxlJobLog存放订单job方法的具体信息 包括执行时间 任务id 任务状态
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();
        // 存放job状态 0 失败 1 成功
        try {
            // 执行方法
            newOrderService.executeTask(xxlJobLog.getJobId());
            xxlJobLog.setStatus(1);//成功
        } catch (Exception e) {
            xxlJobLog.setStatus(0);//失败
            xxlJobLog.setError(ExceptionUtil.getAllExceptionMsg(e));
            log.error("定时任务执行失败，任务id为：{}", XxlJobHelper.getJobId());
        } finally {
            //耗时
            long times = System.currentTimeMillis() - startTime;
            xxlJobLog.setTimes(times);
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }

}
