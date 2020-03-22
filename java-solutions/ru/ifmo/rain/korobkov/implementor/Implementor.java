package ru.ifmo.rain.korobkov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

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

public class Implementor implements Impler {

    private static final String GENERATED_CLASS_SUFFIX = "Impl";
    private static final String NEW_LINE = System.lineSeparator();
    private static final String SPACE = " ";
    private static final String COMMA = ",";
    private static final String DEFAULT_TAB = "    ";
    private static final String LEFT_CURVE_BRACKET = "{";
    private static final String RIGHT_CURVE_BRACKET = "}";
    private static final String LEFT_BRACKET = "(";
    private static final String RIGHT_BRACKET = ")";

    enum FileExtension {
        JAVA(".java"),
        CLASS(".class");

        private String value;

        FileExtension(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    private static class MethodWrapper {
        private static final int PRIME = 1031;
        private final Method method;

        Method getMethod() {
            return method;
        }

        MethodWrapper(Method method) {
            this.method = method;
        }

        @Override
        public int hashCode() {
            Parameter[] parameters = method.getParameters();

            int hashCode = Integer.hashCode(parameters.length) * PRIME + method.getName().hashCode();
            for (Parameter parameter : parameters) {
                hashCode = hashCode * PRIME + parameter.toString().hashCode();
            }
            return hashCode;
        }

        private boolean parametersEquals(Parameter[] a, Parameter[] b) {
            if (a.length != b.length) {
                return false;
            }
            for (int i = 0; i < a.length; i++) {
                if (!a[i].toString().equals(b[i].toString())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodWrapper)) {
                return false;
            }
            MethodWrapper that = (MethodWrapper) obj;
            return this.method.getName().equals(that.method.getName()) &&
                    parametersEquals(method.getParameters(), that.method.getParameters());
        }
    }

    private static String getClassName(Class<?> token) {
        return token.getSimpleName() + GENERATED_CLASS_SUFFIX;
    }


    private static Path addPackageToPath(Class<?> token, Path root) {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage()
                    .getName().replace('.', '/') + "/");
        }
        return root;
    }

    static Path getFilePath(Class<?> token, Path root, FileExtension extension) throws IOException {
        root = addPackageToPath(token, root);
        Files.createDirectories(root);
        return root.resolve(getClassName(token) + extension.getValue());
    }

    private static String escape(String text) {
        StringBuilder result = new StringBuilder();
        int textLength = text.length();
        for (int index = 0; index < textLength; index++) {
            char code = text.charAt(index);
            if ((int) code <= 127) {
                result.append(code);
            } else {
                result.append(String.format("\\u%04X", (int) code));
            }
        }
        return result.toString();
    }

    private static String getPackage(Class<?> token) {
        return "package " + token.getPackage().getName();
    }

    private static void printPackage(Class<?> token, BufferedWriter BufferedWriter) throws IOException {
        if (token.getPackage() != null) {
            BufferedWriter.write(escape(getPackage(token) + ";" + NEW_LINE));
        }
    }

    private static String getClassDeclaration(Class<?> token) {
        int modifiers = token.getModifiers() & ~Modifier.FINAL &
                ~Modifier.PROTECTED & ~Modifier.INTERFACE & ~Modifier.STATIC & ~Modifier.ABSTRACT;
        return Modifier.toString(modifiers) + (modifiers > 0 ? " " : "") + "class " + getClassName(token) + " " +
                (token.isInterface() ? "implements" : "extends") + " " +
                escape(token.getCanonicalName()) + " {" + NEW_LINE;
    }

    private static void printClassDeclaration(Class<?> token, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(escape(getClassDeclaration(token)));
    }


    private static String getMethodAnnotations(Annotation[] annotations) {
        StringBuilder builder = new StringBuilder();
        for (Annotation annotation : annotations) {
            builder.append(annotation)
                    .append(NEW_LINE);
        }
        return builder.toString();
    }

    private static String getTabulation(int tabs) {
        return DEFAULT_TAB.repeat(tabs);
    }


    private static String getParameters(Executable executable, boolean needType) {
        StringBuilder builder = new StringBuilder();

        Parameter[] parameters = executable.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                builder.append(COMMA)
                        .append(SPACE);
            }
            if (needType) {
                builder.append(parameters[i].getParameterizedType().getTypeName().replace('$', '.'))
                        .append(SPACE);
            }
            builder.append(parameters[i].getName());
        }

        return builder.toString();
    }

    private static String getMethodExceptions(Executable executable) {
        StringBuilder builder = new StringBuilder();
        Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            builder.append(SPACE)
                    .append("throws")
                    .append(SPACE);
        }
        for (int i = 0; i < exceptions.length; i++) {
            if (i > 0) {
                builder.append(COMMA).append(SPACE);
            }
            builder.append(exceptions[i].getCanonicalName());
        }
        return builder.toString();
    }

    private static String getMethodDeclaration(Class<?> token, Executable executable) {
        StringBuilder builder = new StringBuilder();

        String methodModifiers = Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT
                & ~Modifier.INTERFACE & ~Modifier.TRANSIENT & ~Modifier.NATIVE);

        builder.append(getTabulation(1))
                .append(methodModifiers);

        if (executable instanceof Method) {
            Method method = (Method) executable;
            Type genericReturnType = method.getGenericReturnType();
            if (method.getReturnType() != genericReturnType &&
                    genericReturnType.toString().contains("T")) {
                builder.append("<T>");
            }
            builder.append(SPACE)
                    .append(genericReturnType.getTypeName().replace('$', '.'))
                    .append(SPACE)
                    .append(method.getName());
        } else {
            builder.append(SPACE).append(getClassName(token));
        }

        builder.append(LEFT_BRACKET)
                .append(getParameters(executable, true))
                .append(RIGHT_BRACKET);

        builder.append(getMethodExceptions(executable));

        return builder.toString();
    }

    private static String getConstructors(Class<?> token) throws ImplerException {
        StringBuilder builder = new StringBuilder();

        boolean allPrivate = true;
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            allPrivate = false;
            builder.append(getMethodAnnotations(token.getAnnotations()));
            builder.append(getMethodDeclaration(token, constructor));
            builder.append(SPACE)
                    .append(LEFT_CURVE_BRACKET)
                    .append(NEW_LINE)
                    .append(getTabulation(2))
                    .append("super")
                    .append(LEFT_BRACKET)
                    .append(getParameters(constructor, false))
                    .append(RIGHT_BRACKET)
                    .append(";")
                    .append(NEW_LINE)
                    .append(getTabulation(1))
                    .append(RIGHT_CURVE_BRACKET)
                    .append(NEW_LINE)
                    .append(NEW_LINE);
        }
        if (!token.isInterface() && allPrivate) {
            throw new ImplerException("No constructor at all");
        }
        return builder.toString();
    }

    private static void printConstructors(Class<?> token, BufferedWriter bufferedWriter) throws ImplerException, IOException {
        bufferedWriter.write(escape(getConstructors(token)));
    }

    private static String getDefaultValue(Class<?> token) {
        if (token.equals(boolean.class)) {
            return "false";
        } else if (token.isPrimitive()) {
            return "0";
        } else if (token.equals(void.class)) {
            return "";
        }
        return "null";
    }

    private static String getMethodFull(Class<?> token, Method method) {
        StringBuilder builder = new StringBuilder();

        builder.append(getMethodAnnotations(method.getAnnotations()));
        builder.append(getMethodDeclaration(token, method))
                .append(SPACE)
                .append(LEFT_CURVE_BRACKET)
                .append(NEW_LINE)
                .append(getTabulation(2))
                .append("return");

        if (!method.getReturnType().equals(void.class)) {
            builder.append(SPACE)
                    .append(getDefaultValue(method.getReturnType()));
        }

        builder.append(";");

        builder.append(NEW_LINE)
                .append(getTabulation(1))
                .append(RIGHT_CURVE_BRACKET)
                .append(NEW_LINE)
                .append(NEW_LINE);

        return builder.toString();
    }

    private static void printMethod(Class<?> token, Method method, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(escape(getMethodFull(token, method)));
    }

    private static void printMethods(Class<?> token, BufferedWriter bufferedWriter) throws IOException {
        Set<MethodWrapper> set = new HashSet<>();
        for (Method method : token.getMethods()) {
            set.add(new MethodWrapper(method));
        }
        Class<?> ancestor = token;
        while (ancestor != null && !ancestor.equals(Object.class)) {
            for (Method method : ancestor.getDeclaredMethods()) {
                set.add(new MethodWrapper(method));
            }
            ancestor = ancestor.getSuperclass();
        }
        for (MethodWrapper wrapper : set) {
            if (!Modifier.isAbstract(wrapper.getMethod().getModifiers())) {
                continue;
            }
            printMethod(token, wrapper.getMethod(), bufferedWriter);
        }
        bufferedWriter.write(escape(RIGHT_CURVE_BRACKET + NEW_LINE));
    }

    private static void printFile(Class<?> token, BufferedWriter bufferedWriter) throws ImplerException, IOException {
        printPackage(token, bufferedWriter);
        printClassDeclaration(token, bufferedWriter);
        printConstructors(token, bufferedWriter);
        printMethods(token, bufferedWriter);
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Invalid arguments");
        }
        if (Modifier.isFinal(token.getModifiers()) || Enum.class == token || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Token is invalid");
        }
        try (BufferedWriter writer =
                     new BufferedWriter(
                             new FileWriter(getFilePath(token, root, FileExtension.JAVA).toString(), StandardCharsets.UTF_8))) {
            printFile(token, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Implementor implementor = new Implementor();

        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: java Implementor <class name> <path>");
            return;
        }

        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
//            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
