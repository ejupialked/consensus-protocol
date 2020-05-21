import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class OtherParticipantHandler extends Thread {
    private Socket otherParticipant;
    private Participant participant;
    private int otherPort;

    private ObjectInputStream ois;

    OtherParticipantHandler(Socket otherParticipant, Participant participant) {
        this.otherParticipant = otherParticipant;
        this.participant = participant;
        initStreams();
    }

    @Override
    public void run() {
        receiveMessages();
    }

    private void initStreams() {
        try {
            ois = new ObjectInputStream(otherParticipant.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            ois.close();
            otherParticipant.close();
        } catch (IOException e) {

        }
    }

    public void receiveMessages(){
        while(true){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                Token request = null;
                try {
                    request = ((Token) ois.readObject());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if(request instanceof Token.Votes){
                    participant.logger.messageReceived(otherPort, request.request);
                    List<Vote> votes = new ArrayList<>();
                    ((Token.Votes) request).votes.forEach(v -> {
                        votes.add(new Vote(v.getParticipantPort(), v.getVote()));
                    });
                    participant.logger.votesReceived(otherPort, votes);
                    Token.log(votes);
                }else{
                    System.out.println(request.request);
                }

            } catch (IOException e) {
                //e.printStackTrace();
                System.err.println("Closing connection with " + otherPort);
                close();
            }
        }
    }

    /**
     * Wait the ID of the other participant that wants to connect to this {@code participant}
     * @return the ID of the other participant
     */
    public Integer waitOtherPort() {
        Token.P2P p2pRequest = null;
        try {
            p2pRequest = ((Token.P2P) ois.readObject());
            participant.logger.messageReceived(p2pRequest.id, p2pRequest.request);
            this.otherPort = p2pRequest.id;
            return p2pRequest.id;
        } catch (IOException e) {
            e.printStackTrace();
            close();
        } catch (ClassNotFoundException e) {
            System.err.println("Unknown participant did not send its id.");
            return null;
        }
        return null;
    }
}
