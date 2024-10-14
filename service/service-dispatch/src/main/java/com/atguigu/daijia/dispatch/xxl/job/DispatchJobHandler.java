package com.atguigu.daijia.dispatch.xxl.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ClassName: DispatchJobHandler
 * package: com.atguigu.daijia.dispatch.xxl.job
 * Description:
 *
 * @Author lh
 * @Create 2024/10/14 12:42
 * @Version 1.0
 */

@Component
@Slf4j
public class DispatchJobHandler {

    @XxlJob("firstJobHandler")
    public void  firstJobHandler() {
        log.info("xxl-job项目集成测试");
        System.out.println("DispatchJobHandler.firstJobHandler");
    }
}
