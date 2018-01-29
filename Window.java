import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
/**
 * The Window class helps to control the sliding window array for receiving packets on the Server's end of things.
 * It has a 5 length array that contains Packet objects. It contains functions to do things like slide the window,
 * check if all packets have had acks or not, and reading in data from the file.
 *
 * @author Kim Steffens
 * @version December 6th, 2016
 */
class Window {
	/* Packet array window **/
	private Packet[] myWindow;

	/* File requested by client **/
	private File fileToSend;

	/* Keeps track of what sequence number the current packet being created is **/
	private int currentSeq;

	/* Used to read in data from File **/
	private FileInputStream myStream;

	/* If the window doesn't need to be slided anymore **/
	private boolean isEnd;

	/* Length of the file **/
	private long fileLength;

	/**
	 * Constructor for the Window. Sets up the file and window.
	 */
	Window(String sendFile, InetAddress myip, int port, DatagramSocket serverSocket) {
		myWindow = new Packet[5];

		// convert sendFile from String to File
		fileToSend = new File(sendFile.trim());

		// print out the path
		try {
			System.out.println("The file: " + fileToSend.getCanonicalPath());
		}
		catch(Exception e) {
			System.out.println(e);

		}

		// check if file exists
		if (!fileToSend.exists()) {
			System.out.println("File does not exist. Now quitting.");
			System.exit(0);
		}
		// set up globals
		fileLength = fileToSend.length();
		currentSeq = 1;
		isEnd = false;

		// set up file stream
		try {
			myStream = new FileInputStream(fileToSend);
		} catch(Exception e) {
			System.out.println(e);

		}

		// send the file length to the Client
		byte[] mylength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)(fileLength)).array();

		Packet fileSize = new Packet(-1, mylength);
		try {
			boolean received = false;

			// loop around until we receive an ack for file length
			while (!received) {
				serverSocket.send(fileSize.getDatagram(myip, port));
				System.out.println("Sending file length.");
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				try {
					serverSocket.receive(receivePacket);
					received = true;
				}
				catch(Exception e) {
					continue;
				}

			}

		}
		catch(Exception e) {
			System.out.println(e);
		}
		createWindow();

	}
	/**
	 * Creates the initial window. Reads in from file etc.
	 */
	public void createWindow() {

		// make 5 packets
		for (int i = 0; i < 5; i++) {

			byte[] myData;

			// only can have 1011 bytes of data
			if (fileLength >= 1011) {
				myData = new byte[1011];
				fileLength -= 1011;
			}
			else if (fileLength <= 0) return;
			else {
				
				// if this is the last packet
				myData = new byte[(int)(fileLength)];
				fileLength = 0;
				isEnd = true;
			}

			try {
				if (myStream.read(myData) < 0) {
					isEnd = true;
					return;
				}
			} catch(Exception e) {
				System.out.println(e);

			}
			// create packet
			myWindow[i] = new Packet(currentSeq, myData);
			currentSeq++;

		}
	}

	/**
	 * Send all the packets
	 */
	public void sendWindow(DatagramSocket serverSocket, InetAddress myip, int port) {

		for (int i = 0; i < 5; i++) {
			// if null or already received, don't send
			if (myWindow[i] == null || myWindow[i].isReceived()) {

				continue;
			}
			try {
				System.out.println("Sending packet number " + myWindow[i].getSeq());
				serverSocket.send(myWindow[i].getDatagram(myip, port));
			}
			catch(Exception e) {
				System.out.println(e);

			}

		}
	}

	/**
	 * As we receive acks, set them as received using this method
	 */
	public void setReceived(int seqNum) {
		for (int i = 0; i < 5; i++) {

			// set this sequence number ack as received
			if (myWindow[i] != null && (myWindow[i].getSeq() == seqNum)) {
				myWindow[i].setAckReceived(true);
				return;

			}

		}

	}

	/**
	 * Getter for the window
	 */
	public Packet[] getWindow() {

		return myWindow;

	}

	/**
	 * Getter to see if all packets have been created
	 */
	public boolean getIsEnd() {
		return isEnd;
	}

	/**
	 * Slides window as needed.
	 */
	public void slideAll() {
		while (myWindow[0].isReceived()) {
			slideWindow();

		}
	}

	/**
	 * Method to check if all packets have been received in the window
	 */
	public boolean checkReceived() {
		for (int i = 0; i < 5; i++) {
			if (myWindow[i] != null && !myWindow[i].isReceived()) return false;
		}

		return true;

	}

	/**
	 * Slides the window one space
	 */
	public void slideWindow() {
		System.out.println("Sliding window forward one space...");
		myWindow[0] = myWindow[1];
		myWindow[1] = myWindow[2];
		myWindow[2] = myWindow[3];
		myWindow[3] = myWindow[4];


		// read in new data for 5th space in window
		byte[] myData;
		if (fileLength >= 1011) {
			myData = new byte[1011];
			fileLength -= 1011;
		}
		else if (fileLength <= 0) {
			myWindow[4] = null;
			return;
		} else {
			myData = new byte[(int)(fileLength)];

			fileLength = 0;
			isEnd = true;

		}

		// if no more data, set isEnd true
		try {
			if (myStream.read(myData) < 0) {
				isEnd = true;
				return;
			}
		} catch(Exception e) {
			System.out.println(e);

		}

		// create window and increment the current sequence number
		myWindow[4] = new Packet(currentSeq, myData);
		currentSeq++;

	}

}
