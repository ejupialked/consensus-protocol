import java.util.List;

public abstract class Token {
    String request;

    @Override
    public String toString() {
        return request;
    }

    class JoinToken extends Token {
       final int port; //Port that participant is listening on

        JoinToken(int port){
            this.port = port;
            this.request = "JOIN " + port;
        }
    }

    class DetailsToken extends Token {
        final List<Integer> ports; // List of port number (aka. identifiers)

        DetailsToken(List<Integer> ports){
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

    class VoteOptionsToken extends Token {
        final List<String> voteOptions; // List of voting options for the consensus protocol

        VoteOptionsToken(List<String> voteOptions) {
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

    class VoteToken extends Token {
        final List<Vote> votes;

        VoteToken(List<Vote> votes) {
            this.votes = votes;
            this.request = voteRequest(votes);
        }

        private String voteRequest(List<Vote> votes) {
            StringBuilder req = new StringBuilder();
            req.append("VOTE ");
            //VOTE port i vote 1 port 2 vote 2 ...port n vote n
            votes.forEach(vote -> req.append(vote.getParticipantPort()).append(" ").append(vote.getVote()));
            return req.toString();
        }
    }

    class OutcomeToken extends Token {
        final String outcome;
        final List<Integer> participants;

        OutcomeToken(String outcome, List<Integer> participants) {
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

}
