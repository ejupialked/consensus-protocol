import java.util.StringTokenizer;

public class ReqTokenizer {

    /**
     * Parses requests.
     */
    Token getToken(String req) {

        StringTokenizer sTokenizer = new StringTokenizer(req);

        if (!(sTokenizer.hasMoreTokens()))
            return null;

        String firstToken = sTokenizer.nextToken();

        if (firstToken.equals("JOIN")) {
            if (sTokenizer.hasMoreTokens())
                return new JoinToken(req, sTokenizer.nextToken());
            else
                return null;
        }

        if (firstToken.equals("BROADCAST")) {
            StringBuilder msg = new StringBuilder();
            while (sTokenizer.hasMoreTokens())
                msg.append(" ").append(sTokenizer.nextToken());
            return new BroadcastToken(req, msg.toString());
        }

        if (firstToken.equals("SEND")) {
            String name = sTokenizer.nextToken();
            String msg = "";
            while (sTokenizer.hasMoreTokens())
                msg += " " + sTokenizer.nextToken();
            return new SendToken(req, name, msg);
        }

        if (firstToken.equals("EXIT"))
            return new ExitToken(req);

        return null; // Ignore request..
    }

    abstract class Token {
        //request from participant
        String request;
    }

    class JoinToken extends Token {
        String name;

        JoinToken(String request, String name) {
            this.request = request;
            this.name = name;
        }
    }

    class BroadcastToken extends Token {
        String message;

        BroadcastToken(String request, String message) {
            this.request = request;
            this.message = message;
        }
    }

    class SendToken extends Token {
        String recipient, message;

        SendToken(String request, String recipient, String message) {
            this.request = request;
            this.recipient = recipient;
            this.message = message;
        }
    }

    class ExitToken extends Token {
        ExitToken(String request) {
            this.request = request;
        }
    }

}


