import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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

    private int f; //max no. of failures
    private List<String> options;
    private List<Integer> participantsDetails;
    private Map<Integer, List<Vote>> roundVotes;
    private Vote myVote;
    private String decidedState;

    boolean isOutcomeSent;

    /*** Coordinator connection ***/
    private Socket cConnection;
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
        this.isOutcomeSent = false;

        try {
            this.host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.cConnection = connect(this.cport, host);
        logger.connectionEstablished(cConnection.getPort());

        listenConnections();

        try {
            oos = new ObjectOutputStream(cConnection.getOutputStream());
            ois = new ObjectInputStream(cConnection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Socket connect(int port, InetAddress host){
        Socket socket = null;
        try {
            socket = new Socket(host, port);
        } catch (UnknownHostException e) {
            System.err.println("Host Unknown. Quitting");
            System.exit(0);
        } catch (IOException ex){
            System.err.println("Could not Connect to " + host + ":" + port + ".  Trying again...");
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

    private void close() {
        try {
            oos.close();
            ois.close();
            cConnection.close();
            isRunning = false;
        } catch (IOException e) {
            System.out.println("Closing connection...");
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
            if(group.size() == participantsDetails.size()){
                System.out.println("starting consensus");
                Token.sleep(500);
                runConsensusAlgorithm();
                System.exit(0);
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

            List<Vote> newVotes;
            if(r != 1)
                newVotes = getNewVotes(roundVotes.get(r), roundVotes.get(r-1));
            else
                newVotes = new ArrayList<>(roundVotes.get(r));

            basicMulticast(newVotes, group.values()); // multi-cast newVotes to group
            System.out.println("Multicasting: " + newVotes);
            roundVotes.put(r+1, new ArrayList<>());

            Collection<String> received = new TreeSet<String>();
            Collection<String> next = new TreeSet<String>();
            Token.sleep(getTimeout());

            for(OtherParticipantHandler o: p2pHandler) {
                while(!o.getVotesReceived().isEmpty()){
                    Vote voteToAdd = o.getVotesReceived().remove();
                    received.add(voteToAdd.toString());
                }
            }

            roundVotes.get(r).forEach(v -> next.add(v.toString()));
            received.addAll(next);
            List<Vote> newVoteList = new ArrayList<>();
            received.forEach(v -> newVoteList.add(getVoteFromString(v)));
            roundVotes.put(r+1, newVoteList);
            logger.endRound(r);
            Token.sleep(1000);
        }

        StringBuilder req = new StringBuilder();
        req.append("VOTE ");
        //VOTE port i vote 1 port 2 vote 2 ...port n vote n
        roundVotes.get(f+1).forEach(vote -> req.append(vote.getParticipantPort()).append(" ").append(vote.getVote()).append(" "));
        System.out.println(req);

        this.decidedState = maximum(roundVotes.get(f+1));

        List<Integer> participants = new ArrayList<>();
        roundVotes.get(f+1).forEach(v -> participants.add(v.getParticipantPort()));

        Token.Outcome outcome = new Token().new Outcome(decidedState, participants);

        logger.outcomeDecided(outcome.outcome,outcome.participants);

        System.out.println(outcome);

        sendOutcome(outcome);
        this.isOutcomeSent = true;

        p2pHandler.forEach(OtherParticipantHandler::close);
        System.exit(0);
       // group.values().forEach(ParticipantClient::close);

    }

    private void sendOutcome(Token.Outcome outcome) {
        try {
            oos.writeObject(outcome);
            oos.flush();
            logger.messageSent(cConnection.getPort(), outcome.request);
            logger.outcomeNotified(outcome.outcome, outcome.participants);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String maximum(List<Vote> votes) {
          List<String> list = new ArrayList<String>();
          votes.forEach(v -> list.add(v.getVote()));

            Map<String, Integer> map = new HashMap<>();

            for (String t : list) {
                Integer val = map.get(t);
                map.put(t, val == null ? 1 : val + 1);
            }

            Map.Entry<String, Integer> max = null;

            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if (max == null || e.getValue() > max.getValue())
                    max = e;
            }

            System.err.println(map);
            return max.getKey();
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

        if(!votesToSend.votes.isEmpty()) {
            group.forEach(m -> {
                if(this.pport == 1111)
                    Token.sleep(10000);
                m.sendMessage(votesToSend);
                logger.messageSent(m.getTcpPort(), votesToSend.request);
                logger.votesSent(m.getId(), votesForLogger);
            });
        }
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
        for(Integer id: participantsDetails){
                ParticipantClient client = new ParticipantClient(id, host, this);

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
                    logger.messageReceived(cConnection.getPort(), request.request);
                    logger.detailsReceived(participantsDetails);
                }else if(request instanceof Token.VoteOptions) {
                    this.options = ((Token.VoteOptions) request).voteOptions;
                    logger.messageReceived(cConnection.getPort(), request.request);
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
            logger.messageSent(cConnection.getPort(), join.request);
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

    public static Vote getVoteFromString(String vote){
        int id = Integer.parseInt(vote.substring(1,vote.indexOf(",")));
        String v = vote.substring(vote.indexOf(" ")+1 , vote.length()-1);
        return new Vote(id, v);
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
                ParticipantLogger.initLogger(lport, pport, (int) timeout);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Participant participant = new Participant(cport, lport, pport, timeout);
            Thread participantServer = new Thread(participant);
            participantServer.start();
        }
    }


    public void removeParticipant(ParticipantClient participantClient) {
        this.group.remove(participantClient);
    }

    public void removeParticipant(OtherParticipantHandler otherParticipantHandler) {
        this.p2pHandler.remove(otherParticipantHandler);
    }
}
