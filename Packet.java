import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import java.util.zip.CRC32;
/**
 * The Packet class helps to create and interpret UDP packets. It stores the sequence number of the packet
 * as well as a checksum, and data as needed. ACK packets and data packets are created slightly differently.
 * The packet class also checks the checksum.
 *
 * @author Kim Steffens
 * @version December 6th, 2016
 */
class Packet {

	/** sequence number as int */
	private int seq;

	/** sequence number as bytes */
	private byte[] seqBytes;

	/** data for packets */
	private byte[] data;

	/** if ack or data packet */
	private int isAck;

	/** For usage by Server/Window: to check if an ack was received for a data pack */
	private boolean ackReceived;

	/** the checksum */
	private byte[] checksumBytes = null;

	/** the entire packet data */
	private byte[] packetData;

	/** if corrupt or not */
	private boolean isNotCorrupt;

	/**
	 * Constructor for data packets (used by Server).
	 */
	Packet(int seq, byte[] data) {
		this.checksumBytes = new byte[8];
		this.isAck = 0;
		this.seq = seq;
		this.data = data;
		seqToBytes();
		ackReceived = false;

	}

	/**
	 * Constructor for Ack packets (used by Client).
	 */
	Packet(int seq) {
		this.checksumBytes = new byte[8];
		this.isAck = 1;
		this.seq = seq;
		seqToBytes();
	}

	/**
	 * Constructor for interpreting packets received.
	 */
	Packet(DatagramPacket packet) {
		seqBytes = new byte[4];
		this.packetData = packet.getData();

		// get the sequence number
		seqBytes[0] = packetData[1];
		seqBytes[1] = packetData[2];
		seqBytes[2] = packetData[3];
		seqBytes[3] = packetData[4];

		// convert seq number
		bytesToSeq();

		// get the checksum
		checksumBytes = new byte[8];

		checksumBytes[0] = packetData[5];
		checksumBytes[1] = packetData[6];
		checksumBytes[2] = packetData[7];
		checksumBytes[3] = packetData[8];
		checksumBytes[4] = packetData[9];
		checksumBytes[5] = packetData[10];
		checksumBytes[6] = packetData[11];
		checksumBytes[7] = packetData[12];

		// check the checksum
		checkChecksum(packetData);
		
		// copies any data in
		data = Arrays.copyOfRange(packetData, 13, packetData.length);
		
		
	}


	/**
	 * Convert int to bytes for seq number
	 */
	void seqToBytes() {

		seqBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(this.seq).array();

	}

	/**
	 * Convert bytes to int for seq number
	 */
	void bytesToSeq() {

		seq = ByteBuffer.wrap(seqBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

	}

	/**
	 * getter for data
	 */
	byte[] getData() {

		return data;
	}

	/**
	 * getter for seq int
	 */
	int getSeq() {
		return seq;

	}

	/**
	 * Getter for isAck
	 */
	int getAck() {

		return isAck;
	}
	
	/**
	 * Gets datagram for data packets
	 */
	DatagramPacket getDatagram(InetAddress myip, int port) {
		byte[] myBytes = new byte[1024];

		// put seqence and isAck in
		myBytes[0] = (byte) isAck;
		myBytes[1] = seqBytes[0];
		myBytes[2] = seqBytes[1];
		myBytes[3] = seqBytes[2];
		myBytes[4] = seqBytes[3];

		// put in data
		for (int i = 0; i < this.data.length; i++) {
			myBytes[i + 13] = this.data[i];
		}
		
		// computer checksum
		checksumCalc(myBytes);
		myBytes[5] = checksumBytes[0];
		myBytes[6] = checksumBytes[1];
		myBytes[7] = checksumBytes[2];
		myBytes[8] = checksumBytes[3];
		myBytes[9] = checksumBytes[4];
		myBytes[10] = checksumBytes[5];
		myBytes[11] = checksumBytes[6];
		myBytes[12] = checksumBytes[7];

		return new DatagramPacket(myBytes, myBytes.length, myip, port);

	}

	/**
	 * Creates datagram for Ack packets
	 */
	DatagramPacket getAckDatagram(InetAddress myip, int port) {
		byte[] myBytes = new byte[1024];
		
		// put seq and isAck in
		myBytes[0] = (byte) isAck;
		myBytes[1] = seqBytes[0];
		myBytes[2] = seqBytes[1];
		myBytes[3] = seqBytes[2];
		myBytes[4] = seqBytes[3];
		
		// compute checksum
		checksumCalc(myBytes);
		myBytes[5] = checksumBytes[0];
		myBytes[6] = checksumBytes[1];
		myBytes[7] = checksumBytes[2];
		myBytes[8] = checksumBytes[3];
		myBytes[9] = checksumBytes[4];
		myBytes[10] = checksumBytes[5];
		myBytes[11] = checksumBytes[6];
		myBytes[12] = checksumBytes[7];

		return new DatagramPacket(myBytes, myBytes.length, myip, port);
	}

	/**
	 * Set packet as received
	 */
	public void setAckReceived(boolean b) {
		ackReceived = b;

	}

	/**
	 * getter for ackreceived
	 */
	public boolean isReceived() {
		return ackReceived;

	}


	/**
	 * Calculates 8 byte checksum using CRC32
	 */
	public void checksumCalc(byte[] checkBytes) {
		Checksum checksum = new CRC32();

		// set zero for checksum
		checkBytes[5] = 0x0000;
		checkBytes[6] = 0x0000;
		checkBytes[7] = 0x0000;
		checkBytes[8] = 0x0000;
		checkBytes[9] = 0x0000;
		checkBytes[10] = 0x0000;
		checkBytes[11] = 0x0000;
		checkBytes[12] = 0x0000;
	
		// get checksum
		checksum.update(checkBytes, 0, checkBytes.length);
		long longChecksum = checksum.getValue();
	
		// copy to global
		this.checksumBytes = ByteBuffer.allocate(8).putLong(longChecksum).array();

	}

	/**
	 * Verify the checksum
	 */
	public void checkChecksum(byte[] checkBytes) {
		Checksum checksum = new CRC32();

		// set bytes as zero
		checkBytes[5] = 0x0000;
		checkBytes[6] = 0x0000;
		checkBytes[7] = 0x0000;
		checkBytes[8] = 0x0000;
		checkBytes[9] = 0x0000;
		checkBytes[10] = 0x0000;
		checkBytes[11] = 0x0000;
		checkBytes[12] = 0x0000;

		// get new checksum calc
		checksum.update(checkBytes, 0, checkBytes.length);
		long longChecksum = checksum.getValue();
		byte[] longbyte = ByteBuffer.allocate(8).putLong(longChecksum).array();
		longChecksum = ByteBuffer.wrap(longbyte).getLong();
	
		// compare to old checksum
		long currentCheck = ByteBuffer.wrap(checksumBytes).getLong();

		if (longChecksum == currentCheck) 
			this.isNotCorrupt = true;
		else 
			this.isNotCorrupt = false;

		// print result
		System.out.println("Checksum correct for packet " + getSeq() + ": " + isNotCorrupt);
	}

	/**
	 * Getter to see if packet is corrupt
	 */
	public boolean getNotCorrupt() {

		return isNotCorrupt;
	}

}
