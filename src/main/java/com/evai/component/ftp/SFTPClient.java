package com.evai.component.ftp;

import com.evai.component.utils.IOUtil;
import com.evai.component.utils.LoggerUtil;
import com.jcraft.jsch.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author crh
 */
@Slf4j
public class SFTPClient extends AbstractFTPFactory {

    private ChannelSftp sftp;

    private Session session;

    /**
     * 构造基于密码认证的sftp对象
     */
    public SFTPClient(String username, String password, String host, int port) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    /**
     * 构造基于秘钥认证的sftp对象
     */
    public SFTPClient(String username, String host, int port, String privateKey) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.privateKey = privateKey;
    }

    public SFTPClient() {
    }

    /**
     * 连接sftp服务器
     */
    @Override
    protected void connect() {
        if (isConnected()) {
            return;
        }
        try {
            JSch jsch = new JSch();
            if (privateKey != null) {
                // 设置私钥
                jsch.addIdentity(privateKey);
            }

            session = jsch.getSession(username, host, port);

            if (password != null) {
                session.setPassword(password);
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");

            session.setConfig(config);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect(10000);

            sftp = (ChannelSftp) channel;
            log.info("连接sftp服务器【{}:{}】成功", host, port);
        } catch (Exception e) {
            log.error("连接sftp服务器【{}:{}】失败", host, port, e);
        }
    }

    /**
     * 关闭连接 server
     */
    @Override
    protected void disconnect() {
        if (this.isConnected()) {
            sftp.disconnect();
        }
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
        log.info("关闭sftp服务器【{}:{}】成功", host, port);
    }


    @Override
    public boolean isConnected() {
        if (sftp == null) {
            return false;
        }
        return sftp.isConnected();
    }

    @Override
    public boolean upload(String remotePath, String remoteFileName, String localFile) {
        try {
            sftp.cd(remotePath);
        } catch (SftpException e) {
            //目录不存在，则创建文件夹
            String[] dirs = remotePath.split("/");
            for (String dir : dirs) {
                if (null == dir || "".equals(dir)) {
                    continue;
                }
                dir += "/" + dir;
                try {
                    sftp.cd(dir);
                } catch (SftpException ex) {
                    try {
                        sftp.mkdir(dir);
                        sftp.cd(dir);
                    } catch (SftpException exc) {
                        LoggerUtil.error(log, e);
                    }
                }
            }
        }
        // 上传文件
        InputStream is = null;
        try {
            File file = new File(localFile);
            is = new FileInputStream(file);
            sftp.put(is, remoteFileName, new UploadProgressMonitor().setFile(file));
            return true;
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
        try {
            sftp.cd(remotePath);
            File file = new File(localFile);
            sftp.get(remoteFileName, new FileOutputStream(file), new DownLoadProgressMonitor()
                    .setFile(file)
                    .setConsumer(consumer));
            return true;
        } catch (Exception e) {
            LoggerUtil.error(log, e);
            return false;
        }
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
            sftp.cd(remotePath);
            return sftp.get(remoteFileName, new DownLoadProgressMonitor().setConsumer(consumer));
        } catch (Exception e) {
            LoggerUtil.error(log, e);
        }
        return null;
    }


    @Override
    public boolean delete(String remotePath, String remoteFileName) {
        try {
            sftp.cd(remotePath);
            sftp.rm(remoteFileName);
            return true;
        } catch (SftpException e) {
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
    @SuppressWarnings("unchecked")
    public List<ChannelSftp.LsEntry> listSFTPFiles(String remotePath) {
        try {
            return new ArrayList<>(sftp.ls(remotePath));
        } catch (SftpException e) {
            LoggerUtil.error(log, e);
        }
        return Collections.emptyList();
    }

    @NoArgsConstructor
    @Accessors(chain = true)
    class DownLoadProgressMonitor implements SftpProgressMonitor {
        @Setter
        @Getter
        private File file;
        /**
         * 总进度
         */
        @Getter
        private long progress;
        @Setter
        @Getter
        private Consumer<Long> consumer;

        @Override
        public void init(int op, String src, String dest, long max) {
            log.info("开始下载文件 -> 远程路径: {}, 本地路径: {}, 文件大小: {} bytes", src, file == null ? null : file.getAbsolutePath(), max);
        }

        @Override
        public boolean count(long count) {
            progress = progress + count;
            if (consumer != null) {
                consumer.accept(progress);
            }
            return count > 0;
        }

        @Override
        public void end() {
            log.info("文件下载完成 -> 本地路径: {}", file == null ? null : file.getAbsolutePath());
        }
    }

    @NoArgsConstructor
    @Accessors(chain = true)
    class UploadProgressMonitor implements SftpProgressMonitor {
        @Getter
        @Setter
        private File file;
        /**
         * 总进度
         */
        @Getter
        private long progress;
        @Setter
        @Getter
        private Consumer<Long> consumer;

        @Override
        public void init(int op, String src, String dest, long max) {
            log.info("开始上传文件 -> 本地路径: {}, 远程路径: {}, 文件总大小: {} bytes", file == null ? null : file.getAbsolutePath(), dest, file == null ? null : file.length());
        }

        @Override
        public boolean count(long count) {
            progress = progress + count;
            if (consumer != null) {
                consumer.accept(progress);
            }
            return count > 0;
        }

        @Override
        public void end() {
            log.info("文件上传完成 -> 远程路径: {}", file == null ? null : file.getAbsolutePath());
        }
    }


    public static void main(String[] args) {
        SFTPClient sftp = new SFTPClient();
        sftp.setHost("xxx.xxx.xx.x");
        sftp.setPort(21);
        sftp.setUsername("user");
        sftp.setPassword("password");
        FTPTemplate ftpTemplate = new FTPTemplate(sftp);
        String remoteFile = "/test.txt";
        ftpTemplate.execute(ftpService -> {
            final long _1MB = 1000000L;
            AtomicLong count = new AtomicLong(1);
            InputStream inputStream = ftpService.downloadInputStream(remoteFile, p -> {
                if (p / _1MB >= count.get()) {
                    count.getAndIncrement();
                    log.info("当前下载进度: {} bytes", p);
                }
            });
            Stream<String> stream = IOUtil.readLines(inputStream, Charset.forName("gb18030"));
            stream.count();
        });
    }
}