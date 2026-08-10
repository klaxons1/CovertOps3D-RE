/**
 * Simple debug logger for J2ME/CLDC environment.
 * Stores last error and prints to System.out where available.
 */
public final class DebugLogger {

    private static String lastError = "";
    private static String lastStack = "";

    private DebugLogger() {}

    public static void log(String tag, String msg) {
        lastError = tag + ": " + msg;
        try {
            System.out.println("[" + tag + "] " + msg);
        } catch (Throwable t) {
        }
    }

    public static void logException(String tag, Throwable e) {
        try {
            lastError = tag + ": " + e.toString();
            StringBuffer sb = new StringBuffer();
            sb.append(e.toString()).append(" | ");
            try {
                Throwable cur = e;
                int depth = 0;
                while (cur != null && depth < 2) {
                    sb.append(cur.getClass().getName()).append(":").append(cur.getMessage()).append(";");
                    break;
                }
            } catch (Throwable ignore) {}
            lastStack = sb.toString();
            System.out.println("[" + tag + "] EXCEPTION: " + e.toString());
            e.printStackTrace();
        } catch (Throwable t) {
            lastError = tag + ": exception logging failed";
        }
    }

    public static void logOutOfMemory(String tag, OutOfMemoryError oom) {
        logException(tag, oom);
    }

    public static String getLastError() {
        return lastError;
    }

    public static String getLastStack() {
        return lastStack;
    }

    public static void drawLastError(javax.microedition.lcdui.Graphics g, FontRenderer fr) {
        if (lastError == null || lastError.length() == 0) return;
        try {
            int y = 10;
            g.setColor(0xFF0000);
            g.fillRect(0, 0, PortalRenderer.VIEWPORT_WIDTH, 50);
            g.setColor(0xFFFFFF);
            if (fr != null) {
                fr.drawLargeString(lastError, g, 2, y);
                y += 24;
                if (lastStack.length() > 0) {
                    String truncated = lastStack.length() > 60 ? lastStack.substring(0, 60) : lastStack;
                    fr.drawLargeString(truncated, g, 2, y);
                }
            }
        } catch (Throwable t) {
        }
    }
}
