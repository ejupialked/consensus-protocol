import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class Token implements Serializable {
    String request;
    Integer sender;
    Integer receiver;

    @Override
    public String toString() {
        return request;
    }

    public void setReceiver(Integer receiver) {
        this.receiver = receiver;
    }

    public void setSender(Integer sender) {
        this.sender = sender;
    }

    class Join extends Token {
        final int port;

        public Join(int port){
            this.port = port;
            this.request = joinRequest(port);
        }

        private String joinRequest(int port) {
            StringBuilder req = new StringBuilder();
            req.append("JOIN ").append(port);
            return req.toString();
        }
    }

    class Details extends Token {
        final List<Integer> ports; // List of port number (aka. identifiers)

        public Details(List<Integer> ports){
            this.ports = ports;
            this.request = detailsRequest(ports);
        }

        private String detailsRequest(List<Integer> ports) {
            StringBuilder req = new StringBuilder();
            req.append("DETAILS ");
            ports.forEach(p -> req.append(p).append(" "));
            return req.toString();
        }

    }

    class VoteOptions extends Token {
        final List<String> voteOptions; // List of voting options for the consensus protocol

        VoteOptions(List<String> voteOptions) {
            this.voteOptions = voteOptions;
            this.request = voteOptionsRequest(voteOptions);
        }

        private String voteOptionsRequest(List<String> voteOptions) {
            StringBuilder req = new StringBuilder();
            req.append("VOTE_OPTIONS ");
            voteOptions.forEach(v -> req.append(v).append(" "));
            return req.toString();
        }
    }

    class Votes extends Token {
        final List<SingleVote> votes;

        Votes(List<SingleVote> votes) {
            this.votes = votes;
            this.request = voteRequest(votes);
        }

        private String voteRequest(List<SingleVote> votes) {
            StringBuilder req = new StringBuilder();
            req.append("VOTE");
            //VOTE port i vote 1 port 2 vote 2 ...port n vote n

            votes.forEach(vote -> req.append(vote.getParticipantPort()).append(" ").append(vote.getVote()).append(" "));
            return req.toString();
        }



    }

    class Outcome extends Token {
        final String outcome;
        final List<Integer> participants;

        Outcome(String outcome, List<Integer> participants) {
            this.outcome = outcome;
            this.participants = participants;
            this.request = outcomeRequest(outcome, participants);
        }

        //OUTCOME A 12346 12347 12348
        private String outcomeRequest(String outcome, List<Integer> participants) {
            StringBuilder req = new StringBuilder();
            req.append("OUTCOME ").append(outcome).append(" ");
            participants.forEach(p -> req.append(p).append(" "));
            return req.toString();
        }

    }


    public static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void log(Object o){
        System.out.println(o.toString());
    }
}

class SingleVote implements Serializable {
    private final int participantPort;
    private final String vote;

    public SingleVote(int participantPort, String vote) {
        this.participantPort = participantPort;
        this.vote = vote;
    }

    public int getParticipantPort() {
        return participantPort;
    }

    public String getVote() {
        return vote;
    }

    @Override
    public String toString() {
        return "<" + participantPort + ", " + vote + ">";
    }


}