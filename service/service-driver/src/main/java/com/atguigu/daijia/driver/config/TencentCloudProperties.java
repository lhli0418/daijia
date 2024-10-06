package com.atguigu.daijia.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ClassName: TencentCloudProperties
 * package: com.atguigu.daijia.driver.config
 * Description:
 *
 * @Author lh
 * @Create 2024/10/6 17:12
 * @Version 1.0
 */
@Component
@Data
@ConfigurationProperties(prefix = "tencent.cloud")
public class TencentCloudProperties {

    private String secretId;
    private String secretKey;
    private String region;
    private String bucketPrivate;
}
