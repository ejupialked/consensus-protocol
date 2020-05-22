import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Coordinator implements Runnable {

    private UDPLoggerClient udpLoggerClient;

    private final int port; // Port number that the Coordinator is listening on
    private final int lport; // Port number of the logger server
    private final int parts; // Number of participants expected
    private final long timeout;
    private final List<String> options; // Set of options

    CoordinatorLogger logger;

    private int noParticipantsJoined;
    private boolean isRunning;
    private Map<Integer, ParticipantHandler> participants;
    private ServerSocket ss;

    Coordinator(int port, int lport, int parts, long timeout, List<String> options) {
        this.port = port;
        this.lport = lport;
        this.parts = parts;
        this.timeout = timeout;
        this.options = options;
        this.participants = Collections.synchronizedMap(new HashMap<>(parts));
        this.logger = CoordinatorLogger.getLogger();
    }

    private void initConnection() {
        try {
            ss = new ServerSocket(port);
            logger.startedListening(port);
        } catch (IOException e) {
            System.err.println("Could not listen on port" + port);
        }
    }

    public boolean register(Token.Join join, ParticipantHandler ph) {
        if(participants.containsKey(join.port)){
            System.err.println("Coordinator: Participant already joined.");
            return false;
        }

        participants.put(join.port, ph);
        ph.setID(join.port);
        logger.joinReceived(join.port);
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

        participants.values().forEach(p -> {
            Token.Details tokenDetails = new Token().new Details(
                    listParticipant.stream().filter(p1 -> !p1.equals(p.getIdentifier()))
                    .collect(Collectors.toList()));
             p.sendMessage(tokenDetails);
             logger.detailsSent(p.getIdentifier(), tokenDetails.ports);
        });
    }

    /**
     * Send request for votes to all participants
     */
    private void sendVoteOptions() {
        Token.VoteOptions tokenVote = new Token().new VoteOptions(options);
        participants.values().forEach(p -> {
            p.sendMessage(tokenVote);
            logger.voteOptionsSent(p.getIdentifier(), tokenVote.voteOptions);
        });
    }

    private void waitParticipants() {
        while(true) {
            Socket participant = null;
            try {
                participant = ss.accept();
                logger.connectionAccepted(port);
                ParticipantHandler ph = new ParticipantHandler(participant, this);
                Token.Join req = (Token.Join) ph.waiJoinRequest();
                if(req != null){
                    if(register(req, ph)){
                       ph.start();
                    }
                }
                if(participants.size() == parts){
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }

        System.out.println("Reached max number of participant");
    }


    public static void main(String[] args) {

        if(args.length < 5){
            System.err.println("Unable to create Coordinator." + " Insert the correct number of arguments.");
        } else {
            int port = Integer.parseInt(args[0]);
            int lport = Integer.parseInt(args[1]);
            int parts = Integer.parseInt(args[2]);
            long timeout = Long.parseLong(args[3]);

            List<String> options = new ArrayList<>();

            for (int i = 4; i < args.length ; i++) {
                options.add(args[i]);
            }

            System.out.println("Coordinator port: " + port);
            System.out.println("Logger server port: " + lport);
            System.out.println("No. participants expected: " + parts);
            System.out.println("Timeout: " +timeout + "ms");
            System.out.println("Options for voting: " + options);

            try {
                CoordinatorLogger.initLogger(lport, port, ((int) timeout));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Coordinator coordinator = new Coordinator(port, lport, parts, timeout, options);
            Thread coordinatorServer = new Thread(coordinator);
            coordinatorServer.start();
        }
    }
}
