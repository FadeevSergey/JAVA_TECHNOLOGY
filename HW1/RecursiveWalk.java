import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Sergey Fadeev
 * 02.2020
 */

public class RecursiveWalk {

    private static void checkValidOfDirectory(Path outputFile) {
        try {
            if (!Files.exists(outputFile)) {
                Files.createDirectories(outputFile.getParent());
            }
        } catch(IOException e) {
            System.err.println("~~~| IOException in checkValidOfDirectory |~~~");
        }
    }

    public static void main(String[] args) throws InvalidPathException {
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                String message = "";
                if (args == null) {
                    message = "Null input arguments";
                } else {
                    if (args.length < 2) {
                        message = "Not enough input arguments";
                    }
                    if (args.length > 2) {
                        message = "There are more than two input arguments";
                    }
                    if (args.length >= 2) {
                        if (args[0] == null) {
                            message = "Input file null";
                        }
                        if (args[1] == null) {
                            message = "Output file null";
                        }
                        if (args[0] == null && args[1] == null) {
                            message = "Input and output file null";
                        }
                    }
                }
                throw new InvalidInputArgumentsException(message);
            }

            checkValidOfDirectory(Path.of(args[1]));

            try (BufferedReader in = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)) {
                try (BufferedWriter out = Files.newBufferedWriter(Paths.get(args[1]), StandardCharsets.UTF_8)) {
                    String nameOfFile;
                    FileVisitor fileVisitor = new FileVisitor(out);
                    while (in.ready()) {
                        nameOfFile = in.readLine();
                        try {
                            Files.walkFileTree(Paths.get(nameOfFile), fileVisitor);
                        } catch (InvalidPathException e) {
                            System.err.println("~~~|" + e.getMessage() + "|~~~");
                            out.write(String.format("%08x", 0) + " " + nameOfFile + System.getProperty("line.separator"));
                        }
                    }
                } catch (IOException e) {
                    System.err.println("~~~| Some problem with output file |~~~");
                }
            } catch (NoSuchFileException e) {
                System.err.println("~~~| Input file does not exist |~~~");
            }
        } catch(InvalidInputArgumentsException e) {
            System.err.println("~~~| " + e.getMessage() + " |~~~");
        } catch(NullPointerException e) {
            System.err.println("~~~| Invalid input arguments |~~~");
        } catch(InvalidPathException e) {
            System.err.println("~~~| Invalid path of input file |~~~");
        } catch(IOException e) {
            System.err.println("~~~| IOException |~~~");
        }
    }
}
