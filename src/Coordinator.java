import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Coordinator implements Runnable {

    private final int MAX_PARTICIPANTS = 10;

    private int port;
    private int noParticipants;
    private boolean isRunning;
    private Map<String, ParticipantHandler> participants;
    private ServerSocket ss;

    Coordinator(int port, int noParticipants) {
        this.port = port;
        this.noParticipants = noParticipants;
        this.participants = Collections.synchronizedMap(new HashMap<>(MAX_PARTICIPANTS));
        initConnection();
    }

    private void initConnection() {
        try {
            ss = new ServerSocket(port);
            System.out.print("Number of expected participants: " );
            System.err.println(noParticipants);

            System.out.println("Port: " + port);
            System.out.println("Host: " + InetAddress.getLocalHost().toString());

        } catch (IOException e) {
            System.out.println("Could not listen on port" + port);
        }

        new Thread(this).start();
    }


    public boolean register(String id, ParticipantHandler ph) {
        if(noParticipants >= MAX_PARTICIPANTS)
            return false;

        if(participants.containsKey(id)){
            System.err.println("Coordinator: Participant already joined.");
            return false;
        }

        participants.put(id, ph);
        ph.setID(id);
        System.err.println("Coordinator: Adding " + id + " as a participant");


        if(participants.size() == noParticipants){
            broadcast("Coordinator", "DETAILS " + participants.keySet().toString());
        }

        return true;
    }

    private void unregister(String id){
        participants.remove(id);
        broadcast("Coordinator", "Removing " + id + "..." );
    }



    private synchronized void broadcast(String id, String msg) {
        String txt = id + ": " + msg;
        for (ParticipantHandler p : participants.values()) {
            p.sendMessage(txt);
        }
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning){
            Socket participant = null;
            try {
                participant = ss.accept();
                ParticipantHandler ph = new ParticipantHandler(participant, this);
                new Thread(ph).start();
            } catch (IOException e) {
                this.isRunning = false;
            }
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        int noPartcipants = Integer.parseInt(args[1]);

        Coordinator coordinator = new Coordinator(port, noPartcipants);
    }
}
