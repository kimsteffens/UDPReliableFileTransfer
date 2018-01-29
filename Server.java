import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
/**
 * Server for UDP file transfer. Creates data packets and receives acks. Interacts with a Window object to slide
 * window, etc.
 * 
 * @author Kim Steffens
 * @version December 6th, 2016
 */
class Server {
	public static void main(String args[]) throws Exception {

		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System. in ));

		// user-defined port
		System.out.println("Enter a port number: ");
		int port = Integer.parseInt(inFromUser.readLine());
		DatagramSocket serverSocket = new DatagramSocket(port);
		System.out.println("Waiting for a client...");

		// all packets are 1024 bytes
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		Window myWindow;

		String sendFile = "";

		// get the file name
		try {
			inFromUser = new BufferedReader(new InputStreamReader(System. in ));


			// obtain file name from client
			while (sendFile.equals("")) {
				serverSocket.receive(receivePacket);
				sendFile = new String(receiveData);
			}

			// 10 ms timeout
			serverSocket.setSoTimeout(10);

			System.out.println("File to send: " + sendFile);

		} catch(IOException e) {
			System.out.println("Error in obtaining file name.");
			System.exit(0);
		}

		// get client IP + port
		InetAddress clientIP = receivePacket.getAddress();
		int clientPort = receivePacket.getPort();

		// set up window
		myWindow = new Window(sendFile, clientIP, clientPort, serverSocket);

		while (true) {

			// send the window
			myWindow.sendWindow(serverSocket, clientIP, clientPort);

			int count = 0;
			// try to receive all 5 acks
			while (count < 5) {
				try {
					serverSocket.receive(receivePacket);
				}
				catch(Exception e) {
					break;
				}
				Packet ack = new Packet(receivePacket);

				if (!ack.getNotCorrupt()) {
					System.out.println("Received a corrupt packet.");
					continue;

				}

				System.out.println("Received ack for packet number " + ack.getSeq());
				myWindow.setReceived(ack.getSeq());

				count++;
			}

			// if no more window sliding needed
			if (myWindow.getIsEnd()) {
				
				// check if all received yet
				while (!myWindow.checkReceived()) {

					// send all
					myWindow.sendWindow(serverSocket, clientIP, clientPort);
					int newCount = 0;
					
					// try to receive all acks
					while (newCount < 5) {

						try {
							serverSocket.receive(receivePacket);
						}
						catch(Exception e) {
							newCount++;
							continue;
						}
						Packet ack = new Packet(receivePacket);
						if (!ack.getNotCorrupt()) {
							System.out.println("Received a corrupt packet.");
							continue;

						}

						System.out.println("Received ack for packet number " + ack.getSeq());
						myWindow.setReceived(ack.getSeq());

						newCount++;
						
					}
					
				}

				// if all received, send final message and quit
				Packet finalMessage = new Packet(0);
				serverSocket.send(finalMessage.getAckDatagram(clientIP, clientPort));

				System.out.println("File transmission complete. Telling client goodbye and quitting.");
				System.exit(0);
			}
			// slide until needed
			else {
				myWindow.slideAll();

			}

			// reset received data
			receiveData = new byte[1024];
			receivePacket = new DatagramPacket(receiveData, receiveData.length);

		}
	}
}
