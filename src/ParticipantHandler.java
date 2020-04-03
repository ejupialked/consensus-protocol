import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ParticipantHandler implements Runnable {
    private Socket participant;
    private Coordinator coordinator;

    private String id;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private boolean isRunning;

    ParticipantHandler(Socket participant, Coordinator coordinator) {
        this.participant = participant;
        this.coordinator = coordinator;
        initStreams();
    }

    private void initStreams() {
        try {
            //System.out.println("init Streams..");
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

    public void sendMessage(String message){
        try {
            oos.writeUTF(message);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Error sending message to " + id);
        }
    }

    public void receiveMessages(){

        this.isRunning = true;
        while(isRunning){
            try {
                ReqTokenizer.Token token = null;
                ReqTokenizer reqTokenizer = new ReqTokenizer();

                String request = ois.readUTF();

                // First, the client must register.
                token = reqTokenizer.getToken(request);

                if (!(token instanceof ReqTokenizer.JoinToken)) {
                    close();
                    return;
                }

                // Check the participant's registration request.
                if (!(coordinator.register(((ReqTokenizer.JoinToken) token).name, this))) {
                    close();
                    return;
                }

                System.err.println("[P " + ((ReqTokenizer.JoinToken) token).name + "]: " + token.request);


            } catch (IOException e) {
                //e.printStackTrace();
                close();
            }
        }
    }

    @Override
    public void run() {
        receiveMessages();
    }

    public void setID(String id) {
        this.id = id;
    }
}
