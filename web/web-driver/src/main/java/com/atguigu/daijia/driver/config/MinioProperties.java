package com.atguigu.daijia.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: MinioProperties
 * package: com.atguigu.daijia.driver.config
 * Description:
 *
 * @Author lh
 * @Create 2024/10/23 1:30
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "minio") // 读取配置文件 minio为前缀
@Data
public class MinioProperties {

    private String endpointUrl;
    private String accessKey;
    private String secreKey;
    private String bucketName;
}
