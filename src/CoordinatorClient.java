import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class CoordinatorClient implements Runnable {
    private Socket participant;
    private Coordinator coordinator;

    private int id;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private boolean isRunning;

    CoordinatorClient(Socket participant, Coordinator coordinator) {
        this.participant = participant;
        this.coordinator = coordinator;
        initStreams();
    }

    @Override
    public void run() {
        waiJoinRequest();
        receiveMessages();
    }

    private void initStreams() {
        try {
            oos = new ObjectOutputStream(participant.getOutputStream());
            ois = new ObjectInputStream(participant.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            oos.close();
            ois.close();
            participant.close();
            isRunning = false;
        } catch (IOException e) {
            System.out.println("Closing connection with P" + id);
        }
    }

    public void sendMessage(Token message){
        Token.sendMessage(oos, message);
    }

    public void receiveMessages(){
        this.isRunning = true;
        while(isRunning){
            try {
                Token request = null;
                try {
                    request = ((Token) ois.readObject());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if(request instanceof Token.Outcome){
                    System.out.println(((Token.Outcome) request).outcome);
                }else{
                    System.out.println(request.request);
                }

            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }
    }

    private void waiJoinRequest() {
        Token.Join joinRequest = null;
        try {
            joinRequest = ((Token.Join) ois.readObject());
            if(coordinator.register(joinRequest, this))
                System.out.println(joinRequest.request);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Unknown participant did not send a join request.");
        }
    }

    public void setID(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
