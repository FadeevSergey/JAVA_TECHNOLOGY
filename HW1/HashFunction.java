import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * @author Sergey Fadeev
 * 02.2020
 */

class HashFunction {
    static int getHashValueFNV(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            final int BUFFER_SIZE = 8192;
            int resultHashValue = 0x811c9dc5;
            byte[] arrayOfBytes;
            arrayOfBytes = in.readNBytes(BUFFER_SIZE);
            while (arrayOfBytes.length != 0) {
                for (byte tempByte : arrayOfBytes) {
                    resultHashValue = (resultHashValue * 0x01000193) ^ (tempByte & 0xff);
                }
                arrayOfBytes = in.readNBytes(BUFFER_SIZE);
            }
            return resultHashValue;
        } catch (NoSuchFileException e) {
            System.err.println("---| File not found, cannot calculate hash value |---");
            return 0;
        } catch (IOException e) {
            System.err.println("---| IOException |---");
            return 0;
        }
    }
}
