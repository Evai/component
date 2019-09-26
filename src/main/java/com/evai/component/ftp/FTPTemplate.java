package com.evai.component.ftp;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author crh
 * @date 2019-08-08
 * @description
 */
public class FTPTemplate {

    private FTPService ftpService;
    private AbstractFTPFactory ftpFactory;

    public FTPTemplate(FTPService ftpService) {
        this.ftpService = ftpService;
        this.ftpFactory = (AbstractFTPFactory) ftpService;
    }

    public void execute(Consumer<FTPService> consumer) {
        ftpFactory.connect();
        try {
            consumer.accept(ftpService);
        } finally {
            ftpFactory.disconnect();
        }
    }

    public <R> R submit(Function<FTPService, R> fn) {
        ftpFactory.connect();
        try {
            return fn.apply(ftpService);
        } finally {
            ftpFactory.disconnect();
        }
    }
}
