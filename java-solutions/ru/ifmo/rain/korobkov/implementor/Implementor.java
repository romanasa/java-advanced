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

/**
 * Generate implementation of classes and interfaces
 *
 * Implements {@link Impler}
 */
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

    /**
     * FileExtension enum
     */
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


    /**
     * Class for method to keep it in {@link java.util.HashSet}
     */
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

    /**
     * Method generates class name by token
     *
     * @param token         class definition
     * @return {@link java.lang.String} class name extracted from given definition
     */
    private static String getClassName(Class<?> token) {
        return token.getSimpleName() + GENERATED_CLASS_SUFFIX;
    }


    /**
     * Updates path according to specified package
     *
     * @param token     class definition
     * @param root      root path
     * @return {@link Path}
     */
    private static Path addPackageToPath(Class<?> token, Path root) {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage()
                    .getName().replace('.', '/') + "/");
        }
        return root;
    }

    /**
     * Returns full path where implementation class of <code>token</code>
     * with extension <code>extension</code> should be generated
     *
     * @param token           class definition
     * @param root            root path
     * @param extension       file extension
     * @return {@link Path}
     * @throws IOException if can't create directories
     */
    static Path getFilePath(Class<?> token, Path root, FileExtension extension) throws IOException {
        root = addPackageToPath(token, root);
        Files.createDirectories(root);
        return root.resolve(getClassName(token) + extension.getValue());
    }

    /**
     * Escapes text
     *
     * @param text text to escape
     * @return {@link java.lang.String} escaped string
     */
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

    /**
     * Method generates package name by class definition
     *
     * @param token          class definition
     * @return {@link java.lang.String} package name extracted from given definition
     */
    private static String getPackage(Class<?> token) {
        return "package " + token.getPackage().getName();
    }

    /**
     * Prints package of class using {@link java.io.BufferedWriter}, if it is not null
     *
     * @param token             class definition
     * @param bufferedWriter    used to print package
     * @throws IOException if error while writing
     */
    private static void printPackage(Class<?> token, BufferedWriter bufferedWriter) throws IOException {
        if (token.getPackage() != null) {
            bufferedWriter.write(escape(getPackage(token) + ";" + NEW_LINE));
        }
    }

    /**
     * Get all needed information from class definition to generate class declaration
     *
     * @param token         class definition
     * @return {@link java.lang.String} class declaration string
     */
    private static String getClassDeclaration(Class<?> token) {
        int modifiers = token.getModifiers() & ~Modifier.FINAL &
                ~Modifier.PROTECTED & ~Modifier.INTERFACE & ~Modifier.STATIC & ~Modifier.ABSTRACT;
        return Modifier.toString(modifiers) + (modifiers > 0 ? " " : "") + "class " + getClassName(token) + " " +
                (token.isInterface() ? "implements" : "extends") + " " +
                escape(token.getCanonicalName()) + " {" + NEW_LINE;
    }

    /**
     * Prints class declaration using {@link java.io.BufferedWriter}
     *
     * @param token             class definition
     * @param bufferedWriter    used to print declaration of class
     * @throws IOException if error while writing
     */
    private static void printClassDeclaration(Class<?> token, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(escape(getClassDeclaration(token)));
    }

    /**
     * Converts array of annotations to string
     *
     * @param annotations array of annotations
     * @return {@link java.lang.String} annotations converted to string
     */
    private static String getMethodAnnotations(Annotation[] annotations) {
        StringBuilder builder = new StringBuilder();
        for (Annotation annotation : annotations) {
            builder.append(annotation)
                    .append(NEW_LINE);
        }
        return builder.toString();
    }

    /**
     * Returns string that consists of <code>cnt</code> tabs
     *
     * @param cnt amount of tabs needed
     * @return {@link String} tabulation string
     */
    private static String getTabulation(int cnt) {
        return DEFAULT_TAB.repeat(cnt);
    }

    /**
     * Method returns parameters from executable
     *
     * @param executable        executable where to get parameters from
     * @param needType          flag, which indicates whether type is needed before parameter name
     * @return {@link java.lang.String} parameters string
     */
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


    /**
     * Method generates string with exceptions separated by comma from executable
     *
     * @param executable executable where to get exceptions from
     * @return {@link java.lang.String} exceptions
     */
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

    /**
     * Retrieves method declarations from class definition
     *
     * Removes the following modifiers if the executable does have them:
     * <ul>
     * <li><code>abstract</code></li>
     * <li><code>interface</code></li>
     * <li><code>transient</code></li>
     * <li><code>native</code></li>
     * </ul>
     *
     * @param token class definition
     * @param executable      executable where to get declarations
     * @return {@link java.lang.String} methods string
     */
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

    /**
     * Generates constructors for class from class definition
     *
     * @param token class definition
     * @return {@link java.lang.String}
     * @throws ImplerException when there are no public constructors and class definition is not an interface
     * @see ImplerException
     */
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

    /**
     * Prints class constructors using {@link java.io.BufferedWriter}
     *
     * @param token             class definition
     * @param bufferedWriter    used to print class constructors
     * @throws ImplerException  if error occurred at the stage of generating constructor
     * @throws IOException      if error with IO
     * @see ImplerException
     */
    private static void printConstructors(Class<?> token, BufferedWriter bufferedWriter) throws ImplerException, IOException {
        bufferedWriter.write(escape(getConstructors(token)));
    }

    /**
     * Method returns default value for <code>type</code>
     *
     * @param token     class definition
     * @return {@link java.lang.String} default value for type extracted from class
     */
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


    /**
     * Generates full implementation of method
     *
     * The following items are generated here:
     * <ul>
     * <li>Annotations</li>
     * <li>Method declaration with all needed modifiers</li>
     * <li>Method implementation with default return value if needed</li>
     * </ul>
     *
     * @param token         class definition
     * @param method        method full implementation of which is needed
     * @return {@link java.lang.String} full method implementation
     */
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

    /**
     * Prints method using {@link java.io.BufferedWriter}
     *
     * @param token             class definition
     * @param method            method to print
     * @param bufferedWriter    used to print implementation of method
     * @throws IOException if error with IO
     */
    private static void printMethod(Class<?> token, Method method, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(escape(getMethodFull(token, method)));
    }


    /**
     * Prints methods using {@link java.io.BufferedWriter}
     *
     * @param token             class definition
     * @param bufferedWriter    used to print methods
     * @throws IOException if error while writing
     */
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

    /**
     * Generates full implementation of class definition in file with suffix <code>Impl.java</code>
     * <br>
     * Prints the following: <br>
     * <ul>
     * <li>package</li>
     * <li>class declaration</li>
     * <li>constructors</li>
     * <li>methods</li>
     * </ul>
     *
     * @param token             class definition
     * @param bufferedWriter    used for printing
     * @throws ImplerException  if error
     * @throws IOException      if error
     */
    private static void printFile(Class<?> token, BufferedWriter bufferedWriter) throws ImplerException, IOException {
        printPackage(token, bufferedWriter);
        printClassDeclaration(token, bufferedWriter);
        printConstructors(token, bufferedWriter);
        printMethods(token, bufferedWriter);
    }

    /**
     * Generates implementation for the given class <code>classDefinition</code> and dumps it to <code>root</code>
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException if something went wrong
     *
     * @see info.kgeorgiy.java.advanced.implementor.Impler
     */
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


    /**
     * Entry point of the program. Command line arguments are processed here
     * <p>
     * Usage:
     * <ul>
     * <li>{@code java -cp . -p . -m ru.ifmo.rain.korobkov.implementor <code>classname</code> <code>path</code>}</li>
     * </ul>
     *
     * @param args command line arguments.
     * @see Implementor
     */
    public static void main(String[] args) {
        Implementor implementor = new Implementor();

        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: java -cp . -p . -m ru.ifmo.rain.korobkov.implementor <class name> <path>");
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
