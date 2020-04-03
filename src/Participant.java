import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Participant implements Runnable{
    private Socket socket;
    private InetAddress host;

    private int cport, pport, lport;

    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private boolean isRunning;

    Participant(int cport, int pport){
        this.cport = cport;
        this.pport = pport;

        try {
            this.host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.socket = connect(cport, host);

        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            sendMessage("JOIN " + pport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket connect(int cport, InetAddress host){
        Socket socket = null;
        try {
            socket = new Socket(host, cport);
        } catch (UnknownHostException e) {
            log("Host Unknown. Quitting");
            System.exit(0);
        } catch (IOException ex){
            log("Could not Connect to " + host + ":" + cport + ".  Trying again...");
            delay(1000);
            return connect(cport, host);
        }
        log("Connected to " + host + ":" + cport);
        return socket;
    }



    private void delay (long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void log(String log){
        System.out.println(log);
    }

    private void sendMessage(String message){
        try {
            oos.writeUTF(message);
            oos.flush();

            log("Sending " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            oos.close();
            ois.close();
            socket.close();
            isRunning = false;
        } catch (IOException e) {
            log("Closing connection...");
        }
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            try {
                String msg = ois.readUTF();
                log(msg);
            } catch (IOException e) {
                close();
            }
        }
    }

    public static void main(String[] args) {
        int cport = Integer.parseInt(args[0]);
        int pport = Integer.parseInt(args[1]);

        Participant p = new Participant(cport, pport);

        new Thread(p).start();
    }
}
