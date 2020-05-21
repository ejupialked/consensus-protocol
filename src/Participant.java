import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class Participant implements Runnable {

    ParticipantLogger logger;

    /* arguments **************/
    private final int cport; // Port number of the coordinator
    private final int lport; // Port number of the logger server
    private final int pport; // Port number that this participant will be listening on.
    private final long timeout;
    /**************************/

    private int f;
    private List<String> options;
    private List<Integer> participantsDetails;
    private Map<Integer, List<Vote>> roundVotes;
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
    private List<OtherParticipantHandler> p2pHandler; // clients of participantServer (this)

    private Map<Integer, ParticipantClient> group; //connections to other participants
    /****************************************************/



    Participant(int cport, int lport, int pport, long timeout){
        this.cport = cport;
        this.lport = lport;
        this.pport = pport;
        this.timeout = timeout;
        this.roundVotes = Collections.synchronizedMap(new HashMap<>());
        this.p2pHandler = Collections.synchronizedList(new ArrayList<>());
        this.group = Collections.synchronizedMap(new HashMap<>());
        this.logger = ParticipantLogger.getLogger();



        try {
            this.host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.cSocket = connect(this.cport, host);
        logger.connectionEstablished(this.cport);

        listenConnections();


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
            sleep(time);
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
        establishConnections();
        waitClients();

        while(true){
            Token.sleep(1000);
            if(group.size() == participantsDetails.size()){
                System.out.println("starting consensus");
                runConsensusAlgorithm();
                return;
            }
        }

    }

    private void runConsensusAlgorithm() {
        roundVotes.put(0, new ArrayList<>());
        List<Vote> v1 = new ArrayList<>();
        v1.add(myVote);
        roundVotes.put(1, v1);

        for (int r = 1; r <= f+1; r++) {
            logger.beginRound(r);

            for (int i = 0; i < roundVotes.size() ; i++) {
                Token.log("Round " + i + ": " + roundVotes.get(i)); Token.sleep(34);
            }

            List<Vote> newVotes;
            if(r != 1)
                newVotes = getNewVotes(roundVotes.get(r), roundVotes.get(r-1));
            else
                newVotes = new ArrayList<>(roundVotes.get(r));

            basicMulticast(newVotes, group.values()); // multi-cast newVotes to group
            System.out.println("Multicasting: " + newVotes);
            roundVotes.put(r+1, new ArrayList<>());

            Token.sleep(timeout);

            Collection<String> received = new TreeSet<String>();
            Collection<String> next = new TreeSet<String>();

            for(OtherParticipantHandler o: p2pHandler) {
                while(!o.getVotesReceived().isEmpty()){
                    Vote voteToAdd = o.getVotesReceived().remove();
                    received.add(voteToAdd.toString());
                }
            }

            roundVotes.get(r+1).forEach(v -> next.add(v.toString()));
            received.addAll(next);

            List<Vote> newVoteList = new ArrayList<>();

            //received.forEach(v -> newVoteList.add(new Vote(v));


            if(r == 2 && pport == 1111) {
                roundVotes.get(r+1).add(new Vote(0000, "B"));

                Token.log(" round " + (r-1) + " is " + roundVotes.get(r +1));
                Token.log(" round " + (r+1) + " is " + roundVotes.get(r +1));
                Token.log(" round " + r+ " is " + roundVotes.get(r ));

            }

            logger.endRound(r);
        }

        StringBuilder req = new StringBuilder();
        req.append("VOTE ");
        //VOTE port i vote 1 port 2 vote 2 ...port n vote n
        roundVotes.get(f+1).forEach(vote -> req.append(vote.getParticipantPort()).append(" ").append(vote.getVote()).append(" "));
        System.out.println(req);
    }

    private List<Vote> getNewVotes(List<Vote> curr, List<Vote> prev) {
        Set<String> ids = prev.stream()
                .map(Vote::toString)
                .collect(Collectors.toSet());
        List<Vote> noDup = curr.stream()
                .filter(v -> !ids.contains(v.toString()))
                .collect(Collectors.toList());
        return noDup;
    }

    private void basicMulticast(List<Vote> votes, Collection<ParticipantClient> group){
        List<SingleVote> singleVoteList = new ArrayList<>();
        votes.forEach(v -> singleVoteList.add(new SingleVote(v.getParticipantPort(), v.getVote())));

        List<Vote> votesForLogger = new ArrayList<>();
        votes.forEach(v -> votesForLogger.add(new Vote(v.getParticipantPort(), v.getVote())));

        Token.Votes votesToSend = new Token().new Votes(singleVoteList);
        group.forEach(m -> {
            m.sendMessage(votesToSend);
            logger.messageSent(pport, votesToSend.request);
            logger.votesSent(m.getOtherPort(), votesForLogger);
        });
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

    private void establishConnections() {
        //Connect to other participants (servers)
        for(Integer otherP: participantsDetails){
                ParticipantClient client = new ParticipantClient(otherP, host, this);
        }
    }

    private void waitClients() {
        while(true) {
            Socket client = null;
            try {
                client = participantServer.accept();
                OtherParticipantHandler pc = new OtherParticipantHandler(client, this);
                pc.start();
                p2pHandler.add(pc);
                logger.connectionAccepted(pport);

                // All participants have sent the vote to this participant! Stop receiving.
                if(p2pHandler.size() == participantsDetails.size()){
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
                    this.f = participantsDetails.size();
                    logger.messageReceived(cport, request.request);
                    logger.detailsReceived(participantsDetails);
                }else if(request instanceof Token.VoteOptions) {
                    this.options = ((Token.VoteOptions) request).voteOptions;
                    logger.messageReceived(cport, request.request);
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
            Token.Join join = new Token().new Join(pport);
            oos.writeObject(join);
            oos.flush();
            logger.messageSent(cport, join.request);
            logger.joinSent(cport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPport() {
        return pport;
    }

    public long getTimeout() {
        return timeout;
    }

    private List<Vote> toList(Set<Vote> votes){
        return new ArrayList<>(votes);
    }

    public List<Integer> getParticipantsDetails() {
        return participantsDetails;
    }

    public Map<Integer, ParticipantClient> getGroup() {
        return group;
    }


    // <2334 B>

    public static Vote getVoteFromString(String vote){
        int id = Integer.parseInt(vote.substring(1,vote.indexOf(",")));
        String v = vote.substring(vote.indexOf(" ") , vote.length()-1);



        return new Vote(id, vote);
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


            try {
                ParticipantLogger.initLogger(0000000, pport, (int) timeout);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String s = "<10203, B>";
            System.out.println(Participant.getVoteFromString(s).toString());

            //Participant participant = new Participant(cport, lport, pport, timeout);
            //Thread participantServer = new Thread(participant);
           // participantServer.start();
        }
    }


    public void removeParticipant(ParticipantClient participantClient) {
        this.group.remove(participantClient);
    }

    public void removeParticipant(OtherParticipantHandler otherParticipantHandler) {
        this.p2pHandler.remove(otherParticipantHandler);
    }
}
