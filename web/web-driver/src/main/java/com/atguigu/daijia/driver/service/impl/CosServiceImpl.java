package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.CosFeignClient;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Autowired
    private CosFeignClient cosFeignClient;
    /**
     * 上传司机信息
     * @param file
     * @param path
     * @return 返回上传vo对象
     */
    @Override
    public CosUploadVo upload(MultipartFile file, String path) {

        Result<CosUploadVo> cosUploadVoResult = cosFeignClient.uploadFile(file,path);
        return cosUploadVoResult.getData();
    }
}
