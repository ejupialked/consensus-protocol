import java.io.IOException;
import java.net.*;

public class UDPLoggerClient {
	private final int loggerServerPort;
	private final int processId;
	private final int timeout;
	private InetAddress host;
	private DatagramPacket ack;
	private String ACK = "ACK";
	private byte[] bufACK = ACK.getBytes();

	/**
	 * @param loggerServerPort the UDP port where the Logger process is listening o
	 * @param processId the ID of the Participant/Coordinator, i.e. the TCP port where the Participant/Coordinator is listening on
	 * @param timeout the timeout in milliseconds for this process 
	 */
	public UDPLoggerClient(int loggerServerPort, int processId, int timeout) {
		this.loggerServerPort = loggerServerPort;
		this.processId = processId;
		this.timeout = timeout;

		this.ack = new DatagramPacket(bufACK, bufACK.length, host, loggerServerPort);

		try {
			this.host = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends a log message to the Logger process
	 * 
	 * @param message the log message
	 * @throws IOException
	 */
	public void logToServer(String message) throws IOException {
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(timeout);
			byte[] buf = (processId +" "+ message).getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, host, loggerServerPort);
			sendAndWaitACK(0, socket, packet);
	}

	public void sendAndWaitACK(int attempt, DatagramSocket socket, DatagramPacket packet) throws IOException {
		if(attempt == 3){
			throw new IOException();
		}
		socket.send(packet);
		System.out.println("send DatagramPacket " + new String(packet.getData()));

		try {
			socket.receive(ack);
			System.out.println("receive DatagramPacket " + new String(ack.getData()));
		}catch (SocketTimeoutException s) {
			System.out.println("resend attemp " + attempt );
			sendAndWaitACK(attempt+1, socket, packet);
		}
	}
}
