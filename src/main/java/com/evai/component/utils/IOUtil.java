package com.evai.component.utils;

import lombok.experimental.UtilityClass;

import java.io.*;
import java.nio.charset.Charset;
import java.util.stream.Stream;

/**
 * @author crh
 * @date 2019-08-08
 * @description
 */
@UtilityClass
public class IOUtil {

    public Stream<String> readLines(InputStream inputStream, Charset charset) {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset));
        try {
            return br
                    .lines()
                    .onClose(() -> closeQuietly(br));
        } catch (Exception e) {
            closeQuietly(br);
            throw e;
        }
    }

    public void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
