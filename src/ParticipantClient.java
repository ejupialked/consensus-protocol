import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ParticipantClient {
    private Socket pConnection;
    private ObjectOutputStream oos;
    private Participant participant;
    private int id;
    private int tcpPort;

    ParticipantClient(Integer id, InetAddress host, Participant participant){
        this.participant = participant;
        this.id = id;
        this.pConnection = connect(id, host);
        this.tcpPort = pConnection.getPort();
        this.participant.logger.connectionEstablished(pConnection.getPort());
        this.participant.getGroup().put(id, this);
        initStreams();
    }

    private void initStreams() {
        try {
            oos = new ObjectOutputStream(pConnection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            if(!participant.isOutcomeSent)
                participant.logger.participantCrashed(id);
            close();
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
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return connect(port, host);
        }
        return socket;
    }

    public int getId() {
        return id;
    }

    public void sendMessage(Token message){
        try {
            oos.writeObject(message);
            participant.logger.messageSent(pConnection.getPort(), message.request);
            oos.flush();
        } catch (IOException e){
            if(!participant.isOutcomeSent)
                participant.logger.participantCrashed(id);
            close();
        }
    }


    public void close(){
        try {
            oos.close();
            pConnection.close();
            participant.removeParticipant(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getTcpPort() {
        return tcpPort;
    }
}
