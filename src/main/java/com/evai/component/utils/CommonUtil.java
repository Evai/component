package com.evai.component.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Created by Evai ON 2018/8/25.
 */
@Slf4j
public class CommonUtil {

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final String MD5 = "MD5";
    private static final String SHA_256 = "SHA-256";

    /**
     * 文件MD5
     *
     * @param file
     * @return
     */
    public static String getFileMD5(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            FileChannel channel = in.getChannel();
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            return getFileMD5(buffer);
        } catch (IOException e) {
            log.error("getFileMD5 error", e);
        }
        return null;
    }

    /**
     * 文件MD5
     *
     * @param byteBuffer
     * @return
     */
    private static String getFileMD5(ByteBuffer byteBuffer) {
        String s = null;
        try {
            MessageDigest md = MessageDigest.getInstance(MD5);
            md.update(byteBuffer);
            // encode 的计算结果是一个 128 位的长整数，
            byte[] tmp = md.digest();
            // 每个字节用 16 进制表示的话，使用两个字符，
            char[] str = new char[16 * 2];
            // 所以表示成 16 进制需要 32 个字符, 表示转换结果中对应的字符位置
            int k = 0;
            // 从第一个字节开始，对 encode 的每一个字节
            for (int i = 0; i < 16; i++) {
                // 转换成 16 进制字符的转换
                byte byte0 = tmp[i];
                // 取字节中高 4 位的数字转换, >>>
                str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
                // 逻辑右移，将符号位一起右移, 取字节中低 4 位的数字转换
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }
            // 换后的结果转换为字符串
            s = new String(str).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            //
        }
        return s;
    }

    public static String stringToMD5(String origin, String salt) {
        return getStringMD5(origin + salt);
    }

    public static String stringToMD5(String origin) {
        return getStringMD5(origin);
    }

    /**
     * MD5加密
     *
     * @param data
     * @return
     */
    public static String getStringMD5(String data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(MD5);
        } catch (NoSuchAlgorithmException e) {
            log.error("getStringMD5 error", e);
            return "";
        }
        byte[] md5Bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        for (byte item : md5Bytes) {
            sb.append(Integer.toHexString(item & 255 | 256), 1, 3);
        }

        return sb
                .toString()
                .toUpperCase();
    }

    /**
     * sha256加密
     *
     * @param str
     * @return
     */
    public static String SHA256(String str) {
        MessageDigest md;
        String encodeStr = "";
        try {
            md = MessageDigest.getInstance(SHA_256);
            md.update(str.getBytes(StandardCharsets.UTF_8));
            encodeStr = byte2Hex(md.digest());
        } catch (Exception e) {
            //
        }
        return encodeStr.toUpperCase();
    }

    public static String randomUUID() {
        return UUID
                .randomUUID()
                .toString()
                .replaceAll("-", "");
    }

    /**
     * 生成用户登录token
     *
     * @param userId
     * @param salt
     * @return
     */
    public static String generateUserToken(Long userId, String salt) {
        return SHA256(randomUUID() + userId + salt);
    }

    /**
     * 生成日志流水号
     *
     * @param randomStr
     * @return
     */
    public static String generateSerialNo(String randomStr) {
        return SHA256(randomUUID() + randomStr);
    }

    private static String byte2Hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        String temp;
        for (byte aByte : bytes) {
            temp = Integer.toHexString(aByte & 0xFF);
            if (temp.length() == 1) {
                builder.append("0");
            }
            builder.append(temp);
        }
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void doThrow(Throwable e) throws E {
        throw (E) e;
    }

    public static void main(String[] args) throws Exception {
        /*System.out.println( CommonUtil.stringToMD5("1qazxsw2!."));
        File file = new File("C:\\Users\\linzi\\work.rar");
        System.out.println(getFileMD5(file));*/
        long start = System.currentTimeMillis();
        System.out.println(stringToMD5("111111", "72e7c635-87ff-4e41-93ae-0c3e910a2b8a"));
        System.out.println(System.currentTimeMillis() - start);
    }

}
