import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ParticipantClient {
    private Socket pConnection;
    private ObjectOutputStream oos;
    private Participant participant;
    private int otherPort;

    ParticipantClient(Integer otherPort, InetAddress host, Participant participant){
        this.participant = participant;
        this.otherPort = otherPort;
        this.pConnection = connect(otherPort, host);
        participant.logger.connectionEstablished(otherPort);
        this.participant.getGroup().put(otherPort, this);
        initStreams();
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

    public int getOtherPort() {
        return otherPort;
    }

    public void sendMessage(Token message){
        try {
            oos.writeObject(message);
            participant.logger.messageSent(otherPort, message.request);
            oos.flush();
        } catch (SocketException e){
            participant.removeParticipant(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
