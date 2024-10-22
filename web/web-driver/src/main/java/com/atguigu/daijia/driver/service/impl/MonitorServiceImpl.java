package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
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
    private FileService fileService;
    /**
     * 上传录音,并保存订单监控记录数据
     * @param file
     * @param orderMonitorForm
     * @return
     */
    @Override
    public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) {
        // 上传录音文件
        String uploadUrl = fileService.upload(file);
        log.info("uploadUrl: {}",uploadUrl);

        // 保存订单监控记录数据
        OrderMonitorRecord orderMonitorRecord = new OrderMonitorRecord();
        BeanUtils.copyProperties(orderMonitorForm,orderMonitorRecord);
        orderMonitorRecord.setFileUrl(uploadUrl);
        orderMonitorFeignClient.saveMonitorRecord(orderMonitorRecord);

        return true;
    }
}
