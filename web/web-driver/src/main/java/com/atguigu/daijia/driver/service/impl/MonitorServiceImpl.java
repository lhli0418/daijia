package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.client.CiFeignClient;
import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import com.atguigu.daijia.model.vo.order.TextAuditingVo;
import com.atguigu.daijia.order.client.OrderMonitorFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorServiceImpl implements MonitorService {

    @Autowired
    private OrderMonitorFeignClient orderMonitorFeignClient;

    @Autowired
    private CiFeignClient ciFeignClient;
    @Autowired
    private FileService fileService;
    /**
     * 上传录音,并保存订单监控记录数据
     * @param file
     * @param orderMonitorForm
     * @return
     */
    @Override
    public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) {
        // 上传录音文件 minio
        String uploadUrl = fileService.upload(file);
        log.info("uploadUrl: {}",uploadUrl);

        // 保存订单监控记录数据
        OrderMonitorRecord orderMonitorRecord = new OrderMonitorRecord();
        orderMonitorRecord.setOrderId(orderMonitorForm.getOrderId());
        orderMonitorRecord.setFileUrl(uploadUrl);
        orderMonitorRecord.setContent(orderMonitorForm.getContent());

        // 记录审核结果 mongo
        TextAuditingVo textAuditingVo = ciFeignClient.textAuditing(orderMonitorForm.getContent()).getData();
        orderMonitorRecord.setResult(textAuditingVo.getResult());
        orderMonitorRecord.setKeywords(textAuditingVo.getKeywords());

        orderMonitorFeignClient.saveMonitorRecord(orderMonitorRecord);

        //更新订单监控统计
        OrderMonitor orderMonitor = orderMonitorFeignClient.getOrderMonitor(orderMonitorForm.getOrderId()).getData();
        int fileNum = orderMonitor.getFileNum() + 1;
        orderMonitor.setFileNum(fileNum);
        //审核结果: 0（审核正常），1 （判定为违规敏感文件），2（疑似敏感，建议人工复核）。
        if("3".equals(orderMonitorRecord.getResult())) {
            int auditNum = orderMonitor.getAuditNum() + 1;
            orderMonitor.setAuditNum(auditNum);
        }
        orderMonitorFeignClient.updateOrderMonitor(orderMonitor);
        return true;
    }
}
