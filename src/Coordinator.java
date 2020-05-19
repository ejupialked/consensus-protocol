import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Coordinator implements Runnable {
    private final int port; // Port number that the Coordinator is listening on
    private final int lport; // Port number of the logger server
    private final int parts; // Number of participants expected
    private final long timeout;
    private final Set<String> options; // Set of options

    private int noParticipantsJoined;
    private boolean isRunning;
    private Map<Integer, CoordinatorClient> participants;
    private ServerSocket ss;

    Coordinator(int port, int lport, int parts, long timeout, Set<String> options) {
        this.port = port;
        this.lport = lport;
        this.parts = parts;
        this.timeout = timeout;
        this.options = options;

        this.participants = new ConcurrentHashMap<>();
    }

    private void initConnection() {
        try {
            ss = new ServerSocket(port);
            System.out.println("Port: " + port);
            System.out.println("Host: " + InetAddress.getLocalHost().toString());
        } catch (IOException e) {
            System.err.println("Could not listen on port" + port);
        }
    }

    public synchronized boolean register(Token.Join join, CoordinatorClient ph) {
        if(participants.size() >= parts)
            return false;

        if(participants.containsKey(join.port)){
            System.err.println("Coordinator: Participant already joined.");
            return false;
        }

        participants.put(join.port, ph);
        ph.setID(join.port);
        System.out.println("Coordinator: Adding " + ph.getId() + " as a participant");

        return true;
    }

    private void unregister(int id){
        participants.remove(id);
    }

    @Override
    public void run() {
        initConnection();
        waitParticipants();
        sendDetails();
        sendVoteOptions();
    }

    /**
     * Send participants details to each participant,
     * to be used only when all participants have joined.
     */
    private void sendDetails() {
        List<Integer> listParticipant = new ArrayList<Integer>(participants.keySet());
        Token.Details details = new Token().new Details(listParticipant);
        participants.values().forEach(p -> p.sendMessage(details));
    }

    /**
     * Send request for votes to all participants
     */
    private void sendVoteOptions() {
        Token.VoteOptions details = new Token().new VoteOptions(options);
        participants.values().forEach(p -> p.sendMessage(details));
    }

    private void waitParticipants() {
        for (int i = 0; i <= parts; i++) {
            Socket participant = null;
            try {
                participant = ss.accept();
                CoordinatorClient ph = new CoordinatorClient(participant, this);
                new Thread(ph).start();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }


    public static void main(String[] args) {

        if(args.length < 5){
            System.err.println("Unable to create Coordinator." + " Insert the correct number of arguments.");
        } else {
            int port = Integer.parseInt(args[0]);
            int lport = Integer.parseInt(args[1]);
            int parts = Integer.parseInt(args[2]);
            long timeout = Long.parseLong(args[3]);

            Set<String> options = new HashSet<>();

            for (int i = 4; i < args.length ; i++) {
                options.add(args[i]);
            }

            System.out.println("Coordinator port: " + port);
            System.out.println("Logger server port: " + lport);
            System.out.println("No. participants expected: " + parts);
            System.out.println("Timeout: " +timeout + "ms");
            System.out.println("Options for voting: " + options);

            Coordinator coordinator = new Coordinator(port, lport, parts, timeout, options);
            Thread coordinatorServer = new Thread(coordinator);
            coordinatorServer.start();

        }
    }
}
