import java.io.DataInputStream;
import java.io.IOException;

public class BinaryUtils {
    static short readShortBE(DataInputStream var0) throws IOException {
       return (short)((var0.read() << 8) + var0.read());
    }

    static int readIntBE(DataInputStream var0) throws IOException {
       return (var0.read() << 24) + (var0.read() << 16) + (var0.read() << 8) + var0.read();
    }

    static short readShortLE(DataInputStream var0) throws IOException {
       return (short)(var0.read() + (var0.read() << 8));
    }

    static int readIntLE(DataInputStream var0) throws IOException {
       return var0.read() + (var0.read() << 8) + (var0.read() << 16) + (var0.read() << 24);
    }

    static void skipBytes(DataInputStream var0, int var1) throws IOException {
       while(var1 > 0) {
          int var2 = var1 > 4096 ? 4096 : var1;
          int var3 = (int)var0.skip((long)var2);
          var1 -= var3;
       }

    }
}
