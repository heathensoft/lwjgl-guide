package io.github.heathensoft.guide.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Frederik Dahl 12/7/2024
 */
public class ExternalFile {

    private final Path path;
    public ExternalFile(String path) { this(Path.of(path)); }
    public ExternalFile(String first, String ...more) { this(Path.of(first, more)); }
    public ExternalFile(Path path) { this.path = path; }

    /** read file to direct buffer */
    public ByteBuffer readToBuffer() throws IOException {
        if (!isFile()) throw new IOException("not a readable file: " + path.toString());
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            byte[] arr = new byte[(int)Files.size(path) + 128];
            int idx = 0;
            int data = inputStream.read();
            while (data != -1) {
                if (idx == arr.length) {
                    int len = (int)(arr.length * 1.5f);
                    arr = Arrays.copyOf(arr,len);
                } arr[idx++] = (byte)data;
                data = inputStream.read();
            } ByteBuffer buffer = ByteBuffer.allocateDirect(idx);
            buffer.put(arr,0,idx);
            return buffer.flip();
        }
    }

    public Stream<String> readLines(Charset charset) throws IOException {
        return Files.lines(path, charset);
    }

    public Stream<String> readLines() throws IOException {
        return Files.lines(path);
    }

    public List<String> readLinesToList(Charset charset) throws IOException {
        return readLines(charset).collect(Collectors.toList());
    }

    public List<String> readLinesToList() throws IOException {
        return readLines().collect(Collectors.toList());
    }

    public String asString(Charset charset) throws IOException {
        return Files.readString(path, charset);
    }

    public String asString() throws IOException {
        return Files.readString(path);
    }

    public void write(ByteBuffer source) throws IOException {
        try (SeekableByteChannel byteChannel = Files.newByteChannel(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)){
            byteChannel.write(source);
        }
    }

    public void write(byte[] bytes) throws IOException {
        Files.write(path,bytes);
    }

    public void write(String string, Charset charset) throws IOException {
        Files.writeString(path,string,charset);
    }

    public void write(String string) throws IOException {
        Files.writeString(path,string);
    }

    public void write(Iterable<? extends CharSequence> lines, Charset charset) throws IOException {
        Files.write(path,lines,charset);
    }

    public void write(Iterable<? extends CharSequence> lines) throws IOException {
        Files.write(path,lines);
    }

    public void append(String string, Charset charset) throws IOException {
        Files.writeString(path,string,charset,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    public void append(String string) throws IOException {
        Files.writeString(path,string,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    public void append(byte[] bytes) throws IOException {
        Files.write(path, bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    public void append(Iterable<? extends CharSequence> lines, Charset charset) throws IOException {
        Files.write(path,lines,charset,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    public void append(Iterable<? extends CharSequence> lines) throws IOException {
        append(lines, StandardCharsets.UTF_8);
    }

    public void append(ByteBuffer source) throws IOException {
        try (SeekableByteChannel byteChannel = Files.newByteChannel(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)){
            byteChannel.write(source);
        }
    }

    /**
     * create dir structure if not already exist
     * @throws IOException if the path exist as a file
     */
    public void createAsDir() throws IOException {
        if (isFile()) throw new FileAlreadyExistsException("dir exist as a file: " + path);
        if (!exist()) {
            Files.createDirectories(path);
        }
    }

    /**
     * create file
     * @param replace replace existing file
     * @throws IOException if the path exist as a dir
     */
    public void createAsFile(boolean replace) throws IOException {
        if (isFolder()) throw new FileAlreadyExistsException("file exist as a dir: " + path);
        if (!exist()) {
            Path parent_dir = path.getParent();
            if (parent_dir != null) {
                Files.createDirectories(parent_dir);
            } Files.createFile(path);
        } else if (replace) {
            Files.delete(path);
            Files.createFile(path);
        }
    }

    @SuppressWarnings("all")
    public void delete() throws IOException {
        if (exist()) { if (isFile()) Files.delete(path);
            else try (Stream<Path> stream = Files.walk(path)){
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    public long size() throws IOException {
        if (!exist()) return 0;
        if (isFile()) return Files.size(path);
        else { final long[] size = { 0L };
            try (Stream<Path> files = Files.walk(path)) {
                files.forEach(path -> {
                    if (Files.isRegularFile(path)) {
                        try { size[0] += Files.size(path);
                        } catch (IOException _) { /* */ }}
                }); return size[0];
            }
        }
    }

    /** copy this into dst */
    public ExternalFile copyTo(ExternalFile dst, boolean replace) throws IOException {
        if (!exist()) throw new IOException("nothing to copy");
        dst.createAsDir();
        ExternalFile copy = dst.resolve(name());
        if (isFolder()) { copy.createAsDir();
            assureStructure(path,copy.path);
            copyContent(path,copy.path,replace);
        } else if (copy.exist()) {
            if (copy.isFolder()) {
                throw new FileAlreadyExistsException("file exist as a dir: " + path);
            } if (replace) { copy.delete();
                Files.copy(path,copy.path); }
        } else Files.copy(path,copy.path);
        return copy;
    }


    public ExternalFile resolve(String other) throws InvalidPathException {
        return new ExternalFile(path.resolve(other));
    }

    public Path path() {
        return path;
    }

    public boolean exist() {
        return Files.exists(path);
    }

    public boolean isFolder() {
        return Files.isDirectory(path);
    }

    public boolean isFile() {
        return Files.isRegularFile(path);
    }

    public String name() {
        return path.getFileName().toString();
    }

    public String toString() {
        return path.toString();
    }

    private void assureStructure(final Path source, final Path target) throws IOException {
        final String tar_str = target.toString();
        final String src_str = source.toString();
        try (Stream<Path> stream = Files.walk(source)){
            stream.forEach(src_path -> {
                if (Files.isDirectory(src_path)) {
                    String sub_str = src_path.toString().substring(src_str.length());
                    Path new_dir = Path.of(tar_str,sub_str);
                    if (!Files.exists(new_dir)) {
                        try { Files.createDirectory(new_dir);
                        } catch (IOException _) { /* */ }
                    }
                }
            });
        }
    }

    private void copyContent(final Path source, final Path target, boolean replace) throws IOException {
        final String tar_str = target.toString();
        final String src_str = source.toString();
        try (Stream<Path> stream = Files.walk(source)){
            stream.forEach(src_path -> {
                if (Files.isRegularFile(src_path)) {
                    String sub_str = src_path.toString().substring(src_str.length());
                    Path new_dir = Path.of(tar_str,sub_str);
                    if (!Files.exists(new_dir)) {
                        try { Files.copy(src_path,new_dir);
                        } catch (IOException _) { /* */ }
                    } else if (replace) {
                        try { Files.copy(src_path,new_dir,
                                StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException _) { /* */ }
                    }
                }
            });
        }
    }

}
