package com.evai.component.cache;

import com.evai.component.cache.enums.KeyFormat;
import lombok.Data;

/**
 * @author crh
 * @date 2019-07-12
 * @description 自定义缓存默认实体类参数
 */
@Data
public class CacheKeyBean {
    private String keyNamePrefix;
    private String keyNameSuffix;
    private KeyFormat keyNameFormat;
    private int[] expired;
    private int asyncSeconds;
    private int lockSeconds;
}
