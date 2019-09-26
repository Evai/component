package com.evai.component.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.evai.component.utils.JacksonUtil;
import com.evai.component.utils.SerializeUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: crh
 * @date: 2018/12/24
 * @description:
 */
@Data
public class BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    protected Long id;

    @TableField(value = "create_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    protected Date createTime;

    @TableField(value = "update_time", update = "now()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    protected Date updateTime;

    /**
     * 1 删除
     */
    @TableField(value = "is_delete", select = false)
    @TableLogic(value = "0", delval = "1")
    protected Integer isDelete;

    public byte[] serialize() {
        return SerializeUtil.serialize(this);
    }

    public BaseEntity unSerialize(byte[] bytes) {
        return (BaseEntity) SerializeUtil.unSerialize(bytes);
    }

    public String toJSON() {
        return JacksonUtil.objToString(this);
    }
}
