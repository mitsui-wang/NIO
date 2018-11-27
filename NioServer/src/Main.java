import com.mitsui.nio.MainServer;

public class Main {

    public static void main(String[] args) {
        MainServer mainServer = new MainServer();
        mainServer.init();
        new Thread(mainServer).start();
    }





}
