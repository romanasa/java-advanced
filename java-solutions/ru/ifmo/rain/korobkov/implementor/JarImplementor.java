package ru.ifmo.rain.korobkov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JarImplementor extends Implementor implements JarImpler {

    /**
     * Compile generated java sources
     *
     * @param root root of files
     * @param jarFile target <var>.jar</var> file
     * @throws ImplerException if compile was unsuccessful
     */
	private void compileClass(Path root, Path jarFile) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }
        final List<String> args = new ArrayList<>();
        args.add(jarFile.toString());
        args.add("-cp");
        args.add(root + File.pathSeparator + getClassPath());
        final int exitCode = compiler.run(null, null, null, args.toArray(String[]::new));
        if (exitCode != 0) {
            throw new ImplerException("Error code: " + exitCode + " during compilation");
        }
    }

    /**
     * Get string of Class's path
     * @return {@link java.lang.String} class path
     * @throws ImplerException if path can't be converted
     */
    private String getClassPath() throws ImplerException {
        try {
            return Path.of(Implementor.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Creates manifest with needed params
     *
     * @return {@link java.util.jar.Manifest}
     * @see Manifest
     */
    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }

    /**
     * Writes generated sources to <var>.jar</var> file
     *
     * @param className path of generated sources
     * @param root <var>.jar</var> root directory
     * @param jarPath target <var>.jar</var> path.
     * @throws ImplerException if can't write <var>.jar</var> file
     * @see Path
     */
    private void dumpJar(Path className, Path root, Path jarPath) throws ImplerException {
        className = className.normalize();
        root = root.normalize();
        jarPath = jarPath.normalize();

        Path classFile = root.resolve(className);

        Manifest manifest = createManifest();
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            out.putNextEntry(new ZipEntry(className.toString().replace('\\', '/')));
            Files.copy(classFile, out);
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Generates interface or class and dumps it into <var>.jar</var> file
     *
     * @param token type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if token can't be implemented
     *
     * @see info.kgeorgiy.java.advanced.implementor.JarImpler
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path temp = Paths.get(System.getProperty("java.io.tmpdir"));
        temp = temp.normalize();
        jarFile = jarFile.normalize();

        Implementor implementor = new Implementor();
        implementor.implement(token, temp);

        try {
            compileClass(temp, Implementor.getFilePath(token, temp, FileExtension.JAVA));
            dumpJar(Implementor.getFilePath(token, Paths.get("./"), FileExtension.CLASS), temp, jarFile);
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Entry point of the program. Command line arguments are processed here
     * <p>
     * Usage:
     * <ul>
     * <li>{@code java -cp . -p . -m ru.ifmo.rain.korobkov.implementor/
     * ru.ifmo.rain.korobkov.implementor.JarImplementor -jar <code>classname</code> <code>path</code>}</li>
     * </ul>
     *
     * @param args command line arguments.
     * @see Implementor
     */
    public static void main(String[] args) {
        JarImplementor implementor = new JarImplementor();

        if (args == null || args.length != 3 || !"-jar".equals(args[0]) || args[1] == null || args[2] == null) {
            System.out.println("Usage: java -cp . -p . -m ru.ifmo.rain.korobkov.implementor/" +
                    "ru.ifmo.rain.korobkov.implementor.JarImplementor -jar <class name> <path>");
            return;
        }

        try {
            implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
//            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
