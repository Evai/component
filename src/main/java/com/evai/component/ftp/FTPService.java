package com.evai.component.ftp;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.net.ftp.FTPFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author crh
 * @date 2019-08-07
 * @description
 */
public interface FTPService {
    /**
     * 是否已连接
     *
     * @return
     */
    boolean isConnected();

    /**
     * 上传文件
     *
     * @param remotePath     远程文件目录
     * @param remoteFileName 远程文件名
     * @param localFile      本地文件
     */
    boolean upload(String remotePath, String remoteFileName, String localFile);

    /**
     * 上传文件
     *
     * @param remoteFile 远程文件
     * @param localFile  本地文件
     */
    boolean upload(String remoteFile, String localFile);

    /**
     * 下载文件
     *
     * @param remoteFile 远程文件
     * @param localFile  本地文件
     * @param consumer   下载进度
     */
    boolean download(String remoteFile, String localFile, Consumer<Long> consumer);

    /**
     * 下载文件
     *
     * @param remoteFile 远程文件
     * @param localFile  本地文件
     */
    boolean download(String remoteFile, String localFile);

    /**
     * 下载文件
     *
     * @param remoteFile 远程文件
     * @return 字节数组
     */
    InputStream downloadInputStream(String remoteFile);

    /**
     * 下载文件
     *
     * @param remoteFile 远程文件
     * @param consumer   下载进度
     * @return
     */
    InputStream downloadInputStream(String remoteFile, Consumer<Long> consumer);

    /**
     * 删除文件
     *
     * @param remotePath     要删除文件所在目录
     * @param remoteFileName 要删除的文件名
     */
    boolean delete(String remotePath, String remoteFileName);

    /**
     * 删除文件
     *
     * @param remoteFile 远程文件
     * @return 字节数组
     */
    boolean delete(String remoteFile);

    /**
     * 列出当前目录所有文件
     *
     * @param remotePath 要列出的目录
     * @return
     */
    default List<FTPFile> listFTPFiles(String remotePath) {
        return Collections.emptyList();
    }

    /**
     * 列出当前目录所有文件
     *
     * @param remotePath 要列出的目录
     * @return
     */
    default List<ChannelSftp.LsEntry> listSFTPFiles(String remotePath) {
        return Collections.emptyList();
    }

}
