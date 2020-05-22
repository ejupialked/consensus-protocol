import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPLoggerServer extends Thread {
    private String ACK = "ACK";
    private int port;
    private DatagramSocket socket;
    private byte[] buf;
    private PrintStream ps;


    UDPLoggerServer(int port){
        try {
            this.socket = new DatagramSocket(port);
            this.buf = new byte[1024];
            this.ps = new PrintStream("logger_server_" + System.currentTimeMillis() + ".log");
        } catch (SocketException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while(true) {
                try {
                    socket.receive(packet);
                    String logMessage = new String(packet.getData(), 0, packet.getLength());
                    ps.println(logMessage);
                    System.out.println(logMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sendACK(packet.getPort(), packet.getAddress(), new String(packet.getData()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    public void sendACK(int port, InetAddress address, String message) throws IOException {
        byte[] buf = (ACK + " " + message).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
    }

    public static void main(String[] args) {
        UDPLoggerServer udpLoggerServer = new UDPLoggerServer(Integer.parseInt(args[0]));
        udpLoggerServer.start();
    }
}
