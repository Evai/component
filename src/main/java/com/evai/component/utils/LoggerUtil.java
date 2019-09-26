package com.evai.component.utils;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

/**
 * @author crh
 * @date 2019-08-08
 * @description
 */
@UtilityClass
public class LoggerUtil {

    public String getMethodName(int index) {
        return Thread
                .currentThread()
                .getStackTrace()[index + 1].getMethodName();
    }

    public void info(Logger logger, String format, Object... arguments) {
        logger.info(getMethodName(2) + " -> " + format, arguments);
    }

    public void warn(Logger logger, String format, Object... arguments) {
        logger.warn(getMethodName(2) + " -> " + format, arguments);
    }

    public void debug(Logger logger, String format, Object... arguments) {
        logger.debug(getMethodName(2) + " -> " + format, arguments);
    }

    public void error(Logger logger, Throwable cause) {
        logger.error(getMethodName(2) + " -> ", cause);
    }

    public void error(Logger logger, String format, Object... arguments) {
        logger.error(getMethodName(2) + " -> " + format, arguments);
    }

}
