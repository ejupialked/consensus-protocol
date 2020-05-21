import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ParticipantClient extends Thread {

    private Socket pConnection;
    private ObjectOutputStream oos;
    private Participant participant;
    private int otherPort;

    ParticipantClient(Integer otherPort, InetAddress host, Participant participant){
        this.participant = participant;
        this.otherPort = otherPort;
        this.pConnection = connect(otherPort, host);
        initStreams();
        sendP2PRequest();
        Token.log("end init connection with " + otherPort);
    }

    private void sendP2PRequest() {
        sendMessage(new Token().new P2P(participant.getPport()));
    }

    @Override
    public void run() {
        //consensus algorithm
       // sendVote(new Token().new Votes(participant.getAllVotes()));
    }

    private void initStreams() {
        try {
            oos = new ObjectOutputStream(pConnection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket connect(int port, InetAddress host){
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            Token.log("connetcted with " + port);
            participant.logger.connectionEstablished(port);
        } catch (UnknownHostException e) {
            System.exit(0);
        } catch (IOException ex){
            System.err.println("Could not Connect to " + host + ":" + port + ".  Trying again...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return connect(port, host);
        }
        return socket;
    }

    public void sendVote(Token.Votes votes){
        votes.setSender(participant.getPport());
        votes.setReceiver(otherPort);
        sendMessage(votes);
    }

    public void sendMessage(Token message){
        try {
            Token.log("Sending this message: " + message.request);
            oos.writeObject(message);
            oos.flush();
            Token.log("Sent!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
