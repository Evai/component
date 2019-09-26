package com.evai.component.utils;

import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.Map;

/**
 * @author Evai
 * @date 2019-08-05
 * description
 */
@UtilityClass
public class AssertUtil {

    @SuppressWarnings("unchecked")
    public <E extends Throwable> void doThrow(Throwable e) throws E {
        throw (E) e;
    }


}
