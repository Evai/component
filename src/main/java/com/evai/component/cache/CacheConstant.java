package com.evai.component.cache;

/**
 * @author crh
 * @date 2019-06-17
 * @description 缓存常量
 */
public interface CacheConstant {
    /**
     * 名称分隔符
     */
    char COLON = ':';
    /**
     * 参数值连接符
     */
    char AND = '&';
    /**
     * 赋值符号
     */
    char ASSIGN = '=';
    /**
     * 参数变量表达式开头符
     */
    char EL = '#';
    /**
     * 参数名称连接符
     */
    String REG_PLUS = "\\+";
    /**
     * 表达式字段和值分隔符
     */
    String REG_DOT = "\\.";
    /**
     * 匹配0个或多个空格
     */
    String REG_SPACE = "\\s*";
    /**
     * 数据库为空值，缓存则存该值，避免一直查询数据库
     */
    String NULL = "null";
    /**
     * 更新数据库时缓存加锁的标识名称
     */
    String READ_LOCK = "readLock:";
    String WRITE_LOCK = "writeLock:";

    /**
     * 所有缓存前缀
     */
    String CACHE_PREFIX = "cacheAble:";

    /**
     * 主键字段名称
     */
    String PK = "id";

    int KEY_LEN = 2;

    int MD5_LEN = 32;

    long SECOND_OF_30 = 30L;

    long SECOND_OF_10 = 10L;

    long SECOND_OF_5 = 5L;

    interface LuaScript {
        /**
         * 自增锁次数并设置过期时间
         */
        String INCRBY_EXPIRE = "local current = redis.call('incrBy', KEYS[1], tonumber(ARGV[1])); redis.call('expire', KEYS[1], tonumber(ARGV[2])); return current";

        /**
         * 自减锁次数，如果小于等于0释放锁
         */
        String DECRBY_RELEASE = "local current = redis.call('decrBy', KEYS[1], tonumber(ARGV[1])); if current <= 0 then redis.call('del', KEYS[1]) end; return current";

        /**
         * 判断是否存在写锁，不存在则写入缓存
         */
        String SET_WITH_NOT_EXIST = "if (redis.call('exists', KEYS[1]) <= 0) then redis.call('setEx', KEYS[2], ARGV[1], ARGV[2]) return 1 else return 0 end";
    }

}
