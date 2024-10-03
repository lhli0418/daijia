package com.atguigu.daijia.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ClassName: WxConfigProperties
 * package: com.atguigu.daijia.customer.config
 * Description:
 *
 * @Author lh
 * @Create 2024/10/3 16:27
 * @Version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxConfigProperties {
    private String appId;
    private String secret;
}
