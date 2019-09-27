package com.evai.component.cache;

import com.evai.component.cache.enums.KeyFormat;
import lombok.Data;

/**
 * @author crh
 * @date 2019-07-12
 * @description 缓存keyName默认全局配置参数
 */
@Data
public class CacheKeyConfig {
    private String keyNamePrefix;
    private String keyNameSuffix;
    private KeyFormat keyNameFormat;
}
