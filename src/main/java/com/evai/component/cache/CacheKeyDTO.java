package com.evai.component.cache;

import lombok.Data;

import java.util.Map;

/**
 * @author Evai
 * @date 2019-06-23
 * description 缓存key传输对象
 */
@Data
public class CacheKeyDTO {
    /**
     * 主键key，存放实体类对象
     */
    private String primaryKey;

    /**
     * 索引key，存主键id，关联到主键key的实体类
     */
    private String indexKey;

    /**
     * 参数键值对
     */
    private Map<String, Object> paramMap;
}
