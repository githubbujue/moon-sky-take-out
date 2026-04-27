package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 高德地图配置属性
 */
@Component
@ConfigurationProperties(prefix = "sky.gaode")
@Data
public class GaodeProperties {

    private String key; // 高德WebAPI的key
    private String shopAddress; // 商家门店地址
}
