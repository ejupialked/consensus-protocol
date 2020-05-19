import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

public class Participant implements Runnable {

    private final int cport; // Port number of the coordinator
    private final int lport; // Port number of the logger server
    private final int pport; // Port number that this participant will be listening on.
    private final long timeout;

    private Socket coordinatorSocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private InetAddress host;
    private boolean isRunning;

    private Set<String> options;
    private List<Integer> participantsDetails;

    Participant(int cport, int lport, int pport, long timeout){
        this.cport = cport;
        this.lport = lport;
        this.pport = pport;
        this.timeout = timeout;

        try {
            this.host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.coordinatorSocket = connect(this.cport, host);

        try {
            oos = new ObjectOutputStream(coordinatorSocket.getOutputStream());
            ois = new ObjectInputStream(coordinatorSocket.getInputStream());
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

    private void close() {
        try {
            oos.close();
            ois.close();
            coordinatorSocket.close();
            isRunning = false;
        } catch (IOException e) {
            log("Closing connection...");
        }
    }

    @Override
    public void run() {
        sendJoinRequest();
        listenForRequests();
    }

    private void listenForRequests() {
        isRunning = true;
        while(isRunning){
            try {
                Token request = null;
                try {
                    request = ((Token) ois.readObject());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if(request instanceof Token.Details){
                    System.out.println(((Token.Details) request).request);
                    this.participantsDetails = ((Token.Details) request).ports;
                    this.participantsDetails.removeIf(p -> p == pport);
                }else if(request instanceof Token.VoteOptions) {
                    System.out.println(((Token.VoteOptions) request).request);
                    this.options = ((Token.VoteOptions) request).voteOptions;
                } else{
                    System.out.println(request.request);
                }

            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }
    }



    private void sendJoinRequest() {
        Token.sendMessage(oos, new Token().new Join(pport));
    }


    public static void main(String[] args) {

        if(args.length != 4){
            System.err.println("Unable to create Participant." + " Insert the correct number of arguments.");
        }else {
            int cport = Integer.parseInt(args[0]);
            int lport = Integer.parseInt(args[1]);
            int pport = Integer.parseInt(args[2]);
            long timeout = Long.parseLong(args[3]);


            System.out.println("Coordinator port: " + cport);
            System.out.println("Logger server port: " + lport);
            System.out.println("This participant port: " + pport);

            System.out.println("Timeout: " +timeout + "ms");

            Participant participant = new Participant(cport, lport, pport, timeout);
            Thread participantServer = new Thread(participant);
            participantServer.start();
        }
    }
}
