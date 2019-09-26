package com.evai.component.ftp;

import lombok.Getter;
import lombok.Setter;

/**
 * @author crh
 * @date 2019-08-08
 * @description
 */
@Getter
@Setter
public abstract class AbstractFTPFactory implements FTPService {

    protected String host;
    protected int port;
    protected String username;
    protected String password;
    protected String privateKey;

    /**
     * 连接
     */
    protected abstract void connect();

    /**
     * 断开连接
     */
    protected abstract void disconnect();

    public static SFTPClient createSFTPClient() {
        return new SFTPClient();
    }

    public static FTPClient createFTPClient() {
        return new FTPClient();
    }

}
