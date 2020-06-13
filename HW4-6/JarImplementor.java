package ru.ifmo.rain.fadeev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import info.kgeorgiy.java.advanced.implementor.BaseImplementorTest;
import org.junit.Assert;

import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static info.kgeorgiy.java.advanced.implementor.BaseImplementorTest.check;

public class JarImplementor extends Implementor implements JarImpler {
    /**
     * Creates {@code .jar} file implementing interface {@link Class}
     *
     * @param token   type token to create implementation for
     * @param jarFile target {@code .jar} file
     * @throws ImplerException when was exception
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Path tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");

            implement(token, tempDir);
            compile(token, tempDir);

            check(tempDir, token);
            createJar(token, jarFile, tempDir);

        } catch (Exception e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Creates jar file {@code .class}-file
     *
     * @param token     token for which {@code .jar}-file is generated
     * @param jarFile   target for the {@code .jar}-file
     * @param classFile path to the compiled class
     * @throws ImplerException when was exception
     */
    void createJar(Class<?> token, Path jarFile, Path classFile) throws ImplerException {
        var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (var stream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            var name = token.getPackageName().replace('.', File.separatorChar) +
                    File.separatorChar +
                    token.getSimpleName() +
                    "Impl.class";
            stream.putNextEntry(new ZipEntry(name));
            Files.copy(Paths.get(classFile.toString(), name), stream);
        } catch (Exception e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Compiles class
     *
     * @param token   class to compile
     * @param tmpPath path to save result
     */
    void compile(Class<?> token, Path tmpPath) {
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            var tmpPathString = tmpPath.toString();
            var classPath = Path.of(codeSource.getLocation().toURI()).toString();
            var compiler = ToolProvider.getSystemJavaCompiler();
            var className = token.getSimpleName() + "Impl";
            var packagePath = tmpPath.resolve(token.getPackageName().replace('.', File.separatorChar));
            String[] compileArgs = {
                    "-classpath",
                    String.join(File.pathSeparator, classPath, tmpPathString, System.getProperty("java.class.path")),
                    packagePath.resolve(className + ".java").toString()
            };
            compiler.run(System.in, System.out, System.err, compileArgs);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Main method of Implementor
     *
     * @param args list of arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                new Implementor().implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                new JarImplementor().implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
