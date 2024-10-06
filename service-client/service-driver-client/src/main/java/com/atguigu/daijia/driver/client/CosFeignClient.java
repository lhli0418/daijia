package com.atguigu.daijia.driver.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.awt.color.ICC_Profile;

@FeignClient(value = "service-driver")
public interface CosFeignClient {


    /**
     * 上传
     * @param file
     * @param path
     * @return
     */
    @PostMapping(value = "/cos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)//字节类型上传
    Result<CosUploadVo> uploadFile(@RequestPart("file") MultipartFile file, @RequestParam("path") String path);
}