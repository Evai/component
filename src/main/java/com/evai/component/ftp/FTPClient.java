package com.evai.component.ftp;

import com.evai.component.utils.LoggerUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;


/**
 * @author Evai
 * @date 12/4/17
 */
@Slf4j
public class FTPClient extends AbstractFTPFactory {

    /**
     * 本地字符编码
     */
    private static String LOCAL_CHARSET = "GBK";

    /**
     * FTP协议里面，规定文件名编码为iso-8859-1
     */
    private static Charset SERVER_CHARSET = StandardCharsets.ISO_8859_1;

    private String host;
    private int port;
    private String username;
    private String password;

    private org.apache.commons.net.ftp.FTPClient ftpClient;

    public FTPClient() {
    }

    public FTPClient(String username, String password, String host, int port) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    /**
     * 初始化ftp服务器
     */
    @Override
    protected void connect() {
        ftpClient = new org.apache.commons.net.ftp.FTPClient();
        ftpClient.setControlEncoding("UTF-8");
        ftpClient.setConnectTimeout(10000);
        try {
            //连接ftp服务器
            ftpClient.connect(host, port);
            //登录ftp服务器
            boolean isLogin = ftpClient.login(username, password);
            if (isLogin) {
                log.info("连接ftp服务器【{}:{}】成功", host, port);
            } else {
                log.info("连接ftp服务器失败");
            }
        } catch (Exception e) {
            log.error("连接ftp服务器【{}:{}】失败", host, port, e);
        }
    }

    @Override
    public void disconnect() {
        try {
            ftpClient.logout();
            ftpClient.disconnect();
            log.info("关闭ftp服务器【{}:{}】成功", host, port);
        } catch (IOException e) {
            log.info("关闭ftp服务器【{}:{}】失败", host, port);
        }
    }

    /**
     * 创建目录(有则切换目录，没有则创建目录)
     *
     * @param path
     * @return
     */
    private boolean makeDir(String path) {
        try {
            if (StringUtils.isEmpty(path)) {
                throw new NoSuchFieldException("文件目录不能为空");
            }
            //            //目录编码，解决中文路径问题
            //            dir = new String(dir.getBytes("GBK"), "iso-8859-1");
            //尝试切入目录
            if (ftpClient.changeWorkingDirectory(path)) {
                return true;
            }
            String[] dirs = path.split("/");
            //循环生成子目录
            for (String dir : dirs) {
                if (null == dir || "".equals(dir)) {
                    continue;
                }
                dir += "/" + dir;
                //进不去目录，说明该目录不存在
                if (!ftpClient.changeWorkingDirectory(dir)) {
                    //创建目录
                    if (!ftpClient.makeDirectory(dir)) {
                        //如果创建文件目录失败，则返回
                        log.info("创建文件目录【{}】失败", dir);
                        return false;
                    }
                }
            }
            //将目录切换至指定路径
            return ftpClient.changeWorkingDirectory(path);
        } catch (Exception e) {
            LoggerUtil.error(log, "创建目录失败【{}】", path, e);
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        if (ftpClient == null) {
            return false;
        }
        return ftpClient.isConnected();
    }

    /**
     * 向FTP服务器上传文件
     *
     * @param remotePath FTP服务器文件存放路径
     * @param remoteFile 上传到FTP服务器上的文件名
     * @param localFile  本地文件路径
     * @return
     */
    @Override
    public boolean upload(String remotePath, String remoteFile, String localFile) {
        InputStream is = null;
        try {
            //切换到上传目录
            if (!this.makeDir(remotePath)) {
                return false;
            }
            //设置上传文件的类型为二进制类型
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            //上传文件
            File file = new File(localFile);
            is = new FileInputStream(file);
            return ftpClient.storeFile(remoteFile, is);
        } catch (Exception e) {
            LoggerUtil.error(log, e);
            IOUtils.closeQuietly(is);
            return false;
        }
    }

    @Override
    public boolean upload(String remoteFile, String localFile) {
        int index = remoteFile.lastIndexOf("/") + 1;
        String remotePath = remoteFile.substring(0, index);
        String remoteFileName = remoteFile.substring(index);
        return upload(remotePath, remoteFileName, localFile);
    }

    @Override
    public boolean download(String remoteFile, String localFile, Consumer<Long> consumer) {
        int index = remoteFile.lastIndexOf("/") + 1;
        String remotePath = remoteFile.substring(0, index);
        String remoteFileName = remoteFile.substring(index);
        OutputStream os = null;
        boolean flag = false;
        try {
            // 切换到FTP服务器目录
            if (!ftpClient.changeWorkingDirectory(remotePath)) {
                throw new FileNotFoundException("指定目录不存在: " + remotePath);
            }
            FTPFile[] fs = ftpClient.listFiles();
            for (FTPFile file : fs) {
                if (file
                        .getName()
                        .equals(remoteFileName)) {
                    os = new FileOutputStream(localFile);
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    flag = ftpClient.retrieveFile(file.getName(), os);
                    break;
                }
            }
        } catch (IOException e) {
            LoggerUtil.error(log, e);
        } finally {
            IOUtils.closeQuietly(os);
        }
        return flag;
    }

    @Override
    public boolean download(String remoteFile, String localFile) {
        return download(remoteFile, localFile, null);
    }

    @Override
    public InputStream downloadInputStream(String remoteFile) {
        return downloadInputStream(remoteFile, null);
    }

    @Override
    public InputStream downloadInputStream(String remoteFile, Consumer<Long> consumer) {
        int index = remoteFile.lastIndexOf("/") + 1;
        String remotePath = remoteFile.substring(0, index);
        String remoteFileName = remoteFile.substring(index);
        try {
            return ftpClient.retrieveFileStream(remotePath + remoteFileName);
        } catch (IOException e) {
            LoggerUtil.error(log, e);
        }
        return null;
    }

    @Override
    public boolean delete(String directory, String deleteFile) {
        try {
            return ftpClient.deleteFile(directory + deleteFile);
        } catch (IOException e) {
            LoggerUtil.error(log, e);
            return false;
        }
    }

    @Override
    public boolean delete(String remoteFile) {
        int index = remoteFile.lastIndexOf("/") + 1;
        String remotePath = remoteFile.substring(0, index);
        String remoteFileName = remoteFile.substring(index);
        return delete(remotePath, remoteFileName);
    }

    @Override
    public List<FTPFile> listFTPFiles(String remotePath) {
        try {
            return Lists.newArrayList(ftpClient.listFiles(remotePath));
        } catch (IOException e) {
            LoggerUtil.error(log, e);
        }
        return Collections.emptyList();
    }

    public static void main(String[] args) throws FileNotFoundException {
        FTPClient ftpClient = new FTPClient();
        //        ftpClient.downloadFile("aaadqwe","qewq.txt", "/Users/crh/Desktop/aaa.txt");
        FileInputStream in = new FileInputStream(new File("/Users/crh/Desktop/test.txt"));

    }

}
