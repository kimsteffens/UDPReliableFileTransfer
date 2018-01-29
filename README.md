# UDPReliableFileTransfer
A command-line file transfer program in Java that utilizes UDP socket programming. This program seeks to provide reliable transfer by being able to handle if packets are lost, duplicated, out-of-order, or corrupted. This program uses a sliding window of size 5 (see Window.java) to handle the packets (see Packet.java) that get sent between client and server. Checksum is calculated on each packet to verify it is not corrupted. Acknowledgement packets are used to verify that packets have been received.

## Usage
To use this program, first run Server.java in a Unix terminal. Enter the port number as prompted. Then, in a separate terminal, run Client.java. Enter the IP address and port number as prompted (if on the same machine as Server.java is, use 127.0.0.1 as the IP address).. Enter the file name to transfer from server to client. A lot of lines will quickly be printed out. These are just stating actions that have automatically taken place (such as the window sliding, an acknowledgement being received, or a corrupt packet was spotted. 

