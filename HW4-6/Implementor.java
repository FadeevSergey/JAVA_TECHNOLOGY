package ru.ifmo.rain.fadeev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.CodeSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import javax.tools.ToolProvider;

/**
 * @author Fadeev Sergey
 * @version 1.0
 * <p>
 * Class implements Impler, JarImpler interfaces
 */
public class Implementor implements Impler {
    /**
     * Generates methods from the interface <var>token</var>.
     * <p>
     *
     * @param token type token to create implementation.
     * @param root  root.
     * @throws ImplerException when was exception.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        var modifiers = token.getModifiers();
        if (!token.isPrimitive() && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers)) {
            Path path = getPath(token, root);
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                var result = generate(token);
                var unicodeResult = stringToUnicode(result);
                writer.write(unicodeResult);
            } catch (IOException e) {
                throw new ImplerException(e.getMessage());
            }
        } else {
            throw new ImplerException("Invalid token");
        }
    }

    /**
     * Creates an absolute path
     *
     * @param token input Interface
     * @param root  path to input
     * @return absolute path for implemented class
     * @throws ImplerException if was exception
     */
    private Path getPath(Class<?> token, Path root) throws ImplerException {
        Path resultPath;

        try {
            final var separator = File.separator;
            final var suffix = "Impl.java";
            final var fileName = token.getSimpleName();
            final var packageName = token.getPackageName().split("\\.");
            String savePath = String.join(separator, packageName) + separator + fileName + suffix;
            resultPath = Path.of(root.toString(), savePath);
            Files.createDirectories(resultPath.getParent());
        } catch (Exception e) {
            throw new ImplerException(e.getMessage());
        }

        return resultPath;
    }

    /**
     * Generates file
     *
     * @param token input Interface
     * @return result of generation
     */
    private String generate(Class<?> token) {
        return new CodeGenerator(token).generate();
    }

    /**
     * Converts a string to unicode
     *
     * @param target input string
     * @return unicode
     */
    private String stringToUnicode(String target) {
        var resultInUnicode = new StringBuilder();
        for (var c : target.toCharArray()) {
            resultInUnicode.append("\\u").append(Integer.toHexString(c | 0x10000).substring(1));
        }
        return resultInUnicode.toString();
    }
}

/**
 * Class for generating code
 */
class CodeGenerator {

    /**
     * Input interface
     */
    private Class<?> token;
    /**
     * StringBuilder with result
     */
    private final StringBuilder result;

    /**
     * Constructor
     *
     * @param token given interface
     */
    public CodeGenerator(Class<?> token) {
        this.token = token;
        result = new StringBuilder();
    }

    /**
     * Constructor
     *
     * @return string with result of generation
     */
    public String generate() {
        addHeader();
        addMethods();

        return result.toString();
    }

    /**
     * Adds everything except methods
     */
    private void addHeader() {
        if (!token.getPackageName().isEmpty()) {
            result.append("package");
            result.append(" ");
            result.append(token.getPackageName());
            result.append(";");
            result.append(System.lineSeparator());
        }

        result.append(System.lineSeparator());
        result.append("public");
        result.append(" ");
        result.append("class");
        result.append(" ");
        result.append(token.getSimpleName());
        result.append("Impl").append(" ");
        result.append("implements");
        result.append(" ");
        result.append(token.getCanonicalName());
        result.append(" ");
        result.append("{");
        result.append(System.lineSeparator());
    }

    /**
     * Adds  methods
     *
     */
    private void addMethods() {
        var methodsSet = Arrays
                .stream(token.getMethods())
                .map(ComparableMethod::new)
                .collect(Collectors.toCollection(HashSet::new));
        while (token != null) {
            methodsSet
                    .addAll(Arrays.stream(token.getDeclaredMethods())
                            .map(ComparableMethod::new)
                            .collect(Collectors.toCollection(HashSet::new)));
            token = token.getSuperclass();
        }

        methodsSet
                .stream()
                .filter(method -> Modifier.isAbstract(method.getMethod().getModifiers()))
                .forEach(method -> addMethod(method.getMethod()));

        result.append("}").append(System.lineSeparator());
    }

    /**
     * Adds return type and name of method
     *
     * @param executable given method
     */
    private void addReturnTypeAndNameOfMethod(Executable executable) {
        var method = (Method) executable;
        result.append(method.getReturnType().getCanonicalName());
        result.append(" ");
        result.append(method.getName());
    }

    /**
     * Adds args of method
     *
     * @param executable given args
     */
    private void addArgs(Executable executable) {
        var args = Arrays.stream(executable.getParameters())
                .map(CodeGenerator::getParameter)
                .collect(Collectors.joining(", ", "(", ")"));

        result.append(args);
    }

    /**
     * Return string representation of parameter
     *
     * @param parameter given parameter
     *
     * @return String perameter
     */
    private static String getParameter(Parameter parameter) {
        return (parameter.getType().getCanonicalName() + " ") + parameter.getName();
    }

    /**
     * Add exceptions in signature
     *
     * @param executable given method
     */
    private void addExceptions(Executable executable) {
        var exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            result.append(" " + "throws" + " ");
            result.append(exceptions[0].getCanonicalName());
        }

        for (int i = 1; i < exceptions.length; i++) {
            result.append(", ");
            result.append(exceptions[i].getCanonicalName());
        }
    }

    /**
     * Add body of method
     *
     * @param executable given method
     */
    private void addMethodBody(Executable executable) {
        result.append("return");
        if (boolean.class.equals(((Method) executable).getReturnType())) {
            result.append(" false");
        } else if (void.class.equals(((Method) executable).getReturnType())) {
            result.append("");
        } else if (((Method) executable).getReturnType().isPrimitive()) {
            result.append(" 0");
        } else {
            result.append(" null");
        }
    }

    /**
     * Add method
     *
     * @param executable given method
     */
    private void addMethod(Executable executable) {
        var modifiers = executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT;
        result.append(System.lineSeparator());
        result.append("\t");
        result.append(Modifier.toString(modifiers));
        result.append(modifiers != 0 ? " " : "");

        addReturnTypeAndNameOfMethod(executable);
        addArgs(executable);
        addExceptions(executable);

        result.append(" ");
        result.append("{");
        result.append(System.lineSeparator());
        result.append("\t").append("\t");

        addMethodBody(executable);

        result.append(";");
        result.append(System.lineSeparator());
        result.append("\t");
        result.append("}");
        result.append(System.lineSeparator());
    }
}

/**
 * The class that stores the method.
 * Can be used in collections as a set or map.
 */
class ComparableMethod {
    /**
     * Method
     */
    private final Method method;

    /**
     * Constructor of ComparableMethod
     *
     * @param method given method
     */
    ComparableMethod(Method method) {
        this.method = method;
    }

    /**
     * @return method
     */
    Method getMethod() {
        return method;
    }

    /**
     * Calculates and returns hash value of {@link #method}
     *
     * @return hash value of {{@link #method}}
     */
    @Override
    public int hashCode() {
        var hash = method.getReturnType().hashCode();

        hash = (hash + 31 * method.getName().hashCode()) % 1000000007;
        hash = (hash + 41 * Arrays.hashCode(method.getParameterTypes()));

        return hash;
    }

    /**
     *
     * @param o object to equal
     * @return true, if they're equal, and false if they're not
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || (getClass() != o.getClass())) {
            return false;
        }

        var secondMethod = (ComparableMethod) o;
        return method.getName().equals(secondMethod.method.getName()) &&
                Objects.equals(method.getReturnType(), secondMethod.method.getReturnType()) &&
                Arrays.equals(method.getParameterTypes(), secondMethod.method.getParameterTypes());
    }
}