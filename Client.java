import java.io.* ;
import java.net.* ;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
/**
 * Client for UDP file transfer. Receives packets, checks their checksum, and writes their data to a file.
 * Sends acks to Server to announce that it received the packets
 * 
 * @author Kim Steffens
 * @version December 6th, 2016
 */
class Client {
	public static void main(String args[]) throws Exception {
		DatagramSocket clientSocket = new DatagramSocket();
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System. in ));

		// user defined ip and port
		System.out.println("Enter an ip address: ");
		String ip = inFromUser.readLine();
		System.out.println("Enter a port number: ");
		int port = Integer.parseInt(inFromUser.readLine());

		InetAddress serverIP;
		int serverPort;

		// file user wants
		System.out.println("Enter a file name to request:");
		String fileName = inFromUser.readLine();

		// send the file
		byte[] sendData = fileName.getBytes();
		InetAddress IPAddress = InetAddress.getByName(ip);
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		clientSocket.send(sendPacket);

		// write file to file with "my-" prefixed to differentiate
		RandomAccessFile writer = new RandomAccessFile("my-" + fileName, "rw");

		byte[] rData = new byte[4];
		DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
		clientSocket.receive(rPacket);


		// while loop to ensure that file size is received.
		int size = -1;
		int count = 0;
		while (size == -1) {
			byte[] fData = new byte[1024];
			DatagramPacket fPacket = new DatagramPacket(fData, fData.length);
			clientSocket.receive(fPacket);

			byte[] sizeBytes = new byte [4];

			// create packet
			Packet mySize = new Packet(fPacket);
			if (!mySize.getNotCorrupt())
				continue;
			if (mySize.getSeq() != -1)
				continue;
			sizeBytes = Arrays.copyOfRange(mySize.getData(), 0, 4);

			size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
			Packet sendPack = new Packet( - 1);
			serverIP = fPacket.getAddress();
			serverPort = fPacket.getPort();
			DatagramPacket toSend = sendPack.getAckDatagram(serverIP, serverPort);
			clientSocket.send(toSend);
			

		}

		
		System.out.println("Received file length. Size of this file: " + size);

		// used to calculate to make sure there are no trailing zeros at the end, because RandomAccessFile does that if left to its own devices
		int finalSeq = (size / 1011) + 1;
		int finalPackSize = size % 1011;
		byte[] myFinalByte = new byte[finalPackSize];


		// receive all the data
		while (true) {

			// receive server messages
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			Packet newmessage = new Packet(receivePacket);

			// Server sends zero when done
			if (newmessage.getSeq() == 0) {
				System.out.println("Received notification. File transfer complete.");
				break;
			}
			// ensure not corrupt
			if (!newmessage.getNotCorrupt()){
				System.out.println("Received a corrupt packet.");
				continue;
			}
			// if extra file size packets are sent
			if (newmessage.getSeq() == -1)
				continue;

			System.out.println("Got packet number " + newmessage.getSeq());

			// interpret packet
			Packet sendPack = new Packet(newmessage.getSeq());
			serverIP = receivePacket.getAddress();
			serverPort = receivePacket.getPort();
			DatagramPacket toSend = sendPack.getAckDatagram(serverIP, serverPort);
			clientSocket.send(toSend);
			System.out.println("Sent acknowledgement for packet number " + newmessage.getSeq());

			System.out.println("final: " + finalSeq);

			// seek position to write to
			writer.seek((newmessage.getSeq() - 1) * 1011);

			// if last packet, don't write all 1011 bytes
			if (newmessage.getSeq() == finalSeq) {
				System.out.println("Writing final packet " + newmessage.getSeq());
				for (int i = 0; i < myFinalByte.length; i++)
				myFinalByte[i] = newmessage.getData()[i];
				writer.write(myFinalByte);
			}
			// write to file
			else writer.write(newmessage.getData());
		}
		writer.close();
		clientSocket.close();
		System.exit(0);
	}

}
