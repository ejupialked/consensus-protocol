import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

public class OtherParticipantHandler extends Thread {
    private Socket otherParticipant;
    private Participant participant;
    private int otherPort = 9999999;

    private Queue<Vote> votesReceived;

    private ObjectInputStream ois;

    OtherParticipantHandler(Socket otherParticipant, Participant participant) {
        this.otherParticipant = otherParticipant;
        this.participant = participant;
        this.votesReceived = new LinkedBlockingDeque<>();
        initStreams();
    }

    @Override
    public void run() {
        receiveVotes();
    }

    public int getOtherPort() {
        return otherPort;
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

    public void receiveVotes(){

        int i = 0;
        while(true){

            i++;
            try {
                Token request = null;
                try {
                    request = ((Token) ois.readObject());
                    participant.logger.messageReceived(participant.getPport(), request.request);


                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if(request instanceof Token.Votes){
                    List<Vote> votesForLogger = new ArrayList<>();

                    if(i==1){
                        this.otherPort = ((Token.Votes) request).votes.get(0).getParticipantPort();
                    }
                    //LIST for logger
                    ((Token.Votes) request).votes.forEach(v -> {
                        votesForLogger.add(new Vote(v.getParticipantPort(), v.getVote()));
                    });
                    participant.logger.votesReceived(otherPort, votesForLogger);


                    // SET for b-delivery
                    ((Token.Votes) request).votes.forEach(v -> {
                        votesReceived.add(new Vote(v.getParticipantPort(), v.getVote()));
                    });

                    Token.log("From " + otherPort +" " +votesReceived);
                }else{
                    System.out.println(request.request);
                }

            } catch (EOFException e){
                System.err.println("Closing connection with " + otherPort);
                close();
                this.participant.removeParticipant(this);
                return;
            } catch (IOException e ) {
                System.err.println("Closing connection with " + otherPort);
                this.participant.removeParticipant(this);
                close();
                return;

            }
        }

    }


    public Queue<Vote> getVotesReceived() {
        return votesReceived;
    }

    public void setOtherPort(int otherPort) {
        this.otherPort = otherPort;
    }
}
