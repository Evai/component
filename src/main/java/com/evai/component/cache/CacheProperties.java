package com.evai.component.cache;

import com.evai.component.cache.enums.KeyFormat;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author crh
 * @date 2019-07-12
 * @description 缓存默认全局配置参数
 */
@Data
@ConfigurationProperties("component.cache")
public class CacheProperties {
    private String keyNamePrefix;
    private String keyNameSuffix;
    private KeyFormat keyFormat;
    private Integer[] expired;
    private Integer asyncSeconds;

}
