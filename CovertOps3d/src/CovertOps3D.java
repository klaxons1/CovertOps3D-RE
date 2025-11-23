import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;

public class CovertOps3D extends MIDlet {
    protected void startApp() {
        Form f = new Form("CovertOps3D");
        f.append("Your MIDlet seems to run.");
        Display.getDisplay(this).setCurrent(f);
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }
}
