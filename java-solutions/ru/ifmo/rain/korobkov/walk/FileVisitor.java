package ru.ifmo.rain.korobkov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileVisitor extends SimpleFileVisitor<Path> {
    private final int SIZE = 4096;

    private final BufferedWriter writer;
    private final byte[] buff = new byte[SIZE];
    public static final int FNV_32_PRIME = 0x01000193;

    FileVisitor(final BufferedWriter writer) {
        this.writer = writer;
    }

    protected FileVisitResult write(final int hash, final String file) throws IOException {
        writer.write(String.format("%08x %s%n", hash, file));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        int hval = 0x811c9dc5;
        try (final InputStream reader = Files.newInputStream(file)) {
            int cnt;
            while ((cnt = reader.read(buff)) != -1) {
                for (int i = 0; i < cnt; i++) {
                    hval *= FNV_32_PRIME;
                    hval ^= (buff[i] & 0xff);
                }
            }
        } catch (final IOException e) {
            hval = 0;
        }
        return write(hval, file.toString());
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        return write(0, file.toString());
    }
}
