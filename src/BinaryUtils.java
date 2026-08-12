import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Low-level binary helpers for reading little-endian and big-endian data
 * from DataInputStream and for skipping bytes efficiently.
 * All methods are intentionally allocation-free to keep loading fast.
 */
public final class BinaryUtils {

    private BinaryUtils() { }

    /** Reads an unsigned short in big-endian order (high byte first). */
    static short readShortBE(DataInputStream input) throws IOException {
        return (short)((input.readUnsignedByte() << 8) | input.readUnsignedByte());
    }

    /** Reads an int in big-endian order. */
    static int readIntBE(DataInputStream input) throws IOException {
        return (input.readUnsignedByte() << 24)
                | (input.readUnsignedByte() << 16)
                | (input.readUnsignedByte() << 8)
                | input.readUnsignedByte();
    }

    /** Reads a short in little-endian order (low byte first). */
    static short readShortLE(DataInputStream input) throws IOException {
        return (short)(input.readUnsignedByte() | (input.readUnsignedByte() << 8));
    }

    /** Reads an int in little-endian order. */
    static int readIntLE(DataInputStream input) throws IOException {
        return input.readUnsignedByte()
                | (input.readUnsignedByte() << 8)
                | (input.readUnsignedByte() << 16)
                | (input.readUnsignedByte() << 24);
    }

    /**
     * Skips exactly byteCount bytes. Some valid InputStream implementations
     * return zero from skip() while data is still readable, so consume one byte
     * in that case instead of treating it as an endless-loop condition.
     */
    static void skipBytes(DataInputStream input, int byteCount) throws IOException {
        while (byteCount > 0) {
            int chunk = byteCount > 4096 ? 4096 : byteCount;
            int skipped = (int)input.skip((long)chunk);
            if (skipped > 0) {
                byteCount -= skipped;
            } else {
                // The fallback is only used by streams that cannot skip. It
                // preserves the fast skip path for JAR resources, while still
                // failing promptly on a truncated or misparsed asset.
                if (input.read() < 0) {
                    throw new EOFException("skipBytes: end of stream, " + byteCount + " bytes left");
                }
                byteCount--;
            }
        }
    }
}
