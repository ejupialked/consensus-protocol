import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class Participant implements Runnable {

    ParticipantLogger logger;

    /* arguments **************/
    private final int cport; // Port number of the coordinator
    private final int lport; // Port number of the logger server
    private final int pport; // Port number that this participant will be listening on.
    private final long timeout;
    /**************************/

    private List<String> options;
    private List<Integer> participantsDetails;
    private List<Vote> allVotes;
    private Vote myVote;

    /*** Coordinator connection ***/
    private Socket cSocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private boolean isRunning;
    /******************************/

    /*** P2P connections ********************************/
    private InetAddress host;
    private ServerSocket participantServer;
    private Map<Integer, OtherParticipantHandler> p2pClients; // clients of participantServer (this)

    private Map<Integer, ParticipantClient> p2pServers; //connections to other participants
    /****************************************************/



    Participant(int cport, int lport, int pport, long timeout){
        this.cport = cport;
        this.lport = lport;
        this.pport = pport;
        this.timeout = timeout;
        this.allVotes = new ArrayList<>();
        this.p2pServers = new HashMap<>();
        this.p2pClients = new HashMap<>();

        try {
            ParticipantLogger.initLogger(0000, pport, (int) timeout);
            this.logger = ParticipantLogger.getLogger();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            this.host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.cSocket = connect(this.cport, host);
        logger.connectionEstablished(this.cport);


        try {
            oos = new ObjectOutputStream(cSocket.getOutputStream());
            ois = new ObjectInputStream(cSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket connect(int port, InetAddress host){
        Socket socket = null;
        try {
            socket = new Socket(host, port);
        } catch (UnknownHostException e) {
            log("Host Unknown. Quitting");
            System.exit(0);
        } catch (IOException ex){
            log("Could not Connect to " + host + ":" + port + ".  Trying again...");
            delay(1000);
            return connect(port, host);
        }
        return socket;
    }

    private void delay (long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void log(String log){
        System.out.println(log);
    }

    private void close() {
        try {
            oos.close();
            ois.close();
            cSocket.close();
            isRunning = false;
        } catch (IOException e) {
            log("Closing connection...");
        }
    }

    @Override
    public void run() {
        sendJoinRequest();
        receiveDetailsAndVotes();
        decideVote();

        new Thread(this::establishP2PConnections).start();

        listenConnections();
        waitP2PClients();

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runConsensusAlgorithm();
        p2pClients.values().forEach(Thread::start);
    }

    private void runConsensusAlgorithm() {

        List<Token.VoteToken> votes = new ArrayList<>();
        getAllVotes().forEach(v -> {
            votes.add(new Token().new VoteToken(v.getParticipantPort(), v.getVote()));
        });

        p2pServers.values().forEach(p -> p.sendVote(new Token().new Votes(votes)));
    }

    private void listenConnections() {
        try {
            this.participantServer = new ServerSocket(pport);
            logger.startedListening();
            Token.log("Started listening on port "+ pport);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void establishP2PConnections() {
        Token.log("establishP2PConnections");
        Token.log("with this ports: " + participantsDetails);
        //Connect to other participants (servers)
        for(Integer otherP: participantsDetails){
            Token.log("connecting with: " + otherP);
            ParticipantClient server = new ParticipantClient(otherP, host, this);
            p2pServers.put(otherP, server);
        }

        Token.log("I have this connections : " + p2pServers.keySet() );
    }

    private void waitP2PClients() {
        while(true) {
            Socket client = null;
            try {
                client = participantServer.accept();
                OtherParticipantHandler pc = new OtherParticipantHandler(client, this);
                Integer otherPort = pc.waitOtherPort();
                //         if(otherPort != null && participantsDetails.contains(otherPort) && !p2pClients.containsKey(otherPort)){
                if(otherPort != null && participantsDetails.contains(otherPort) && !p2pClients.containsKey(otherPort)){
                    p2pClients.put(otherPort, pc);
                    logger.connectionAccepted(otherPort);
                }else{
                    throw new IOException();
                }

                // All participants are connected to this participant! Stop waiting.
                if(p2pClients.size() == participantsDetails.size()){
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    private void decideVote() {
        Random rand = new Random();
        String randomVote = options.get(rand.nextInt(options.size()));
        this.myVote = new Vote(pport, randomVote);
        Token.log("Vote decided: " + myVote);
        this.allVotes.add(this.myVote);
    }

    private void receiveDetailsAndVotes() {
        isRunning = true;
        while(isRunning){
            try {
                Token request = null;
                try {
                    request = ((Token) ois.readObject());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if(request instanceof Token.Details){
                    this.participantsDetails = ((Token.Details) request).ports;
                    logger.detailsReceived(participantsDetails);
                }else if(request instanceof Token.VoteOptions) {
                    this.options = ((Token.VoteOptions) request).voteOptions;
                    logger.voteOptionsReceived(options);
                    break;
                } else{
                    System.out.println(request.request);

                }

            } catch (IOException e) {
                e.printStackTrace();
                close();
            }
        }
    }

    private void sendJoinRequest() {
        try {
            oos.writeObject( new Token().new Join(pport));
            oos.flush();
            logger.joinSent(cport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPport() {
        return pport;
    }

    public List<Vote> getAllVotes() {
        return allVotes;
    }

    public static void main(String[] args) {
        if(args.length != 4){
            System.err.println("Unable to create Participant." + " Insert the correct number of arguments.");
        }else {
            int cport = Integer.parseInt(args[0]);
            int lport = Integer.parseInt(args[1]);
            int pport = Integer.parseInt(args[2]);
            long timeout = Long.parseLong(args[3]);


            System.out.println("Coordinator port: " + cport);
            System.out.println("Logger server port: " + lport);
            System.out.println("This participant port: " + pport);



            Participant participant = new Participant(cport, lport, pport, timeout);
            Thread participantServer = new Thread(participant);
            participantServer.start();
        }
    }
}
