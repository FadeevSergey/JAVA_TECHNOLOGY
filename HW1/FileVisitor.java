import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Sergey Fadeev
 * 02.2020
 */

public class FileVisitor extends SimpleFileVisitor<Path> {
    private BufferedWriter out;

    FileVisitor(BufferedWriter out)
    {
        this.out = out;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        out.write(String.format("%08x", HashFunction.getHashValueFNV(file)) + " " + file + System.getProperty("line.separator"));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        out.write(String.format("%08x", 0) + " " + file + System.getProperty("line.separator"));
        return FileVisitResult.CONTINUE;
    }
}