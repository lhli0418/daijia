package com.atguigu.daijia.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ClassName: WxMaProperties
 * package: com.atguigu.daijia.driver.config
 * Description:
 *
 * @Author lh
 * @Create 2024/10/5 15:49
 * @Version 1.0
 */
@Data
@Component
@ConfigurationProperties("wx.miniapp")
public class WxMaProperties {
    private String appId;
    private String secret;
}
