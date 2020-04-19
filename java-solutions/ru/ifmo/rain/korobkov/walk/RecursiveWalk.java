package ru.ifmo.rain.korobkov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class RecursiveWalk {

    private final Path inputPath;
    private final Path outputPath;

    RecursiveWalk(final String input, final String output) throws WalkerException {
        inputPath = getPath(input);
        outputPath = getPath(output);
        final Path parent = outputPath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (final IOException e) {
                throw new WalkerException("Can't create output file's folder: ", e);
            }
        }
    }

    private Path getPath(final String name) throws WalkerException {
        try {
            return Paths.get(name);
        } catch (final InvalidPathException e) {
            throw new WalkerException("Invalid file name: " + name, e);
        }
    }

    private void walk() throws WalkerException {
        try (final BufferedReader reader = Files.newBufferedReader(inputPath)) {
            try (final BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                String curPath;
                final FileVisitor visitor = new FileVisitor(writer);
                try {
                    while ((curPath = reader.readLine()) != null) {
                        try {
                            try {
                                Files.walkFileTree(Paths.get(curPath), visitor);
                            } catch (final InvalidPathException e) {
                                visitor.write(0, curPath);
                            }
                        } catch (final IOException e) {
                            throw new WalkerException("Error in writing hash: ", e);
                        }
                    }
                } catch (final IOException e) {
                    throw new WalkerException("Error in reading: ", e);
                }
            } catch (final IOException e) {
                throw new WalkerException("Can't open output file: ", e);
            }
        } catch (final IOException e) {
            throw new WalkerException("Can't open input file: ", e);
        }
    }

    public static void main(final String[] args) {
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                throw new WalkerException("Usage: RecursiveWalk <input file> <output file>");
            }
            final RecursiveWalk walker = new RecursiveWalk(args[0], args[1]);
            walker.walk();
        } catch (final WalkerException e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
        }
    }
}
