package io.github.heathensoft.guide.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resources asset loader
 * Frederik Dahl 12/7/2024
 */
public class Resources {


    /** read file to direct buffer (big endian) */
    public static ByteBuffer readToBuffer(String resource, int size) throws IOException {
        ByteBuffer result;
        try (InputStream is = resourceStream(resource)){
            try (ReadableByteChannel byteChannel = Channels.newChannel(is)){
                result = ByteBuffer.allocateDirect(Math.max(128,size));
                result.order(ByteOrder.BIG_ENDIAN);
                while (true) {
                    int bytes = byteChannel.read(result);
                    if (bytes == -1) break;
                    if (result.remaining() == 0) {
                        size = result.capacity() * 2;
                        ByteBuffer b = BufferUtils.createByteBuffer(size);
                        b.order(ByteOrder.BIG_ENDIAN);
                        result = b.put(result.flip());
                    }
                }
            }
        } return MemoryUtil.memSlice(result.flip());
    }

    public static List<String> asLines(String resource) throws IOException {
        return asLines(resource, StandardCharsets.UTF_8);
    }

    public static List<String> asLines(String resource, Charset charset) throws IOException {
        List<String> result;
        try (InputStream input_stream = resourceStream(resource)) {
            InputStreamReader reader = new InputStreamReader(input_stream,charset);
            Stream<String> stream = new BufferedReader(reader).lines();
            result = stream.collect(Collectors.toList());
        } return result;
    }

    public static String asString(String resource) throws IOException {
        return asString(resource, StandardCharsets.UTF_8);
    }

    public static String asString(String resource, Charset charset) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream input_stream = resourceStream(resource)){
            InputStreamReader reader = new InputStreamReader(input_stream,charset);
            BufferedReader bufferedReader = new BufferedReader(reader); String line;
            while ((line = bufferedReader.readLine()) != null)
                builder.append(line).append(System.lineSeparator());
        } return builder.toString();
    }

    private static InputStream resourceStream(String resource) throws IOException {
        InputStream input_stream = getURL(resource).openStream();
        if (input_stream != null) return input_stream;
        throw new IOException("resource: " + resource + "not found");
    }

    private static URL getURL(String resource) throws IOException {
        List<ClassLoader> classLoaders = classLoaders();
        for (ClassLoader classLoader : classLoaders) {
            URL url = classLoader.getResource(resource);
            if (url != null) return url;
        } throw new IOException("resource: " + resource + "not found");
    }

    private static List<ClassLoader> classLoaders() {
        List<ClassLoader> list = new ArrayList<>(2);
        list.add(Thread.currentThread().getContextClassLoader());
        list.add(Resources.class.getClassLoader());
        return list;
    }
}
