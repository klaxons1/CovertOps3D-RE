import java.io.DataInputStream;
import java.io.IOException;

/**
 * Low-level binary helpers for reading little-endian and big-endian data
 * from DataInputStream and for skipping bytes efficiently.
 * All methods are intentionally allocation-free to keep loading fast.
 */
public final class BinaryUtils {

    private BinaryUtils() { }

    /** Reads unsigned short in big-endian order (high byte first). */
    static short readShortBE(DataInputStream input) throws IOException {
        return (short)((input.read() << 8) + input.read());
    }

    /** Reads int in big-endian order. */
    static int readIntBE(DataInputStream input) throws IOException {
        return (input.read() << 24) + (input.read() << 16) + (input.read() << 8) + input.read();
    }

    /** Reads short in little-endian order (low byte first). */
    static short readShortLE(DataInputStream input) throws IOException {
        return (short)(input.read() + (input.read() << 8));
    }

    /** Reads int in little-endian order. */
    static int readIntLE(DataInputStream input) throws IOException {
        return input.read() + (input.read() << 8) + (input.read() << 16) + (input.read() << 24);
    }

    /** Skips exactly byteCount bytes, looping because skip() may skip less. */
    static void skipBytes(DataInputStream input, int byteCount) throws IOException {
        while (byteCount > 0) {
            int chunk = byteCount > 4096 ? 4096 : byteCount;
            int skipped = (int)input.skip((long)chunk);
            byteCount -= skipped;
        }
    }
}
