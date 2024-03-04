# csc445

Project 1 specification: 

Measure the latency and throughput of the following TCP and/or UDP based protocols (as noted below) across at least three pairs of machines using at least two different networks. For example, two CS servers (like rho and pi), or a CS server to a laptop, wired or wireless, or off-campus. Create a web page with graphs summarizing your results. Use appropriate measurement sample sizes, and readily interpretable units in graphs.

All messages must use a simple encryption scheme. One suggestion is to use a xor encoding of 64bit (8 byte, java "long") values, based on a known shared initial key, updated using a custom rng on each step, and then validated by the receiver. Here's a simple rng update function: xorshift, requiring a non-zero initial key

      long xorShift(long r) { r ^= r << 13; r ^= r >>> 7; r ^= r << 17; return r; }
    

1) Measure round-trip latency (RTTs) and how it varies with message size in TCP, by sending and receiving (echoing and validating) messages of size 8, 64, and 512 bytes.
2) The same as (1), except using UDP.
3) Measure throughput (bits per second) and how it varies with message size in TCP, by sending 1MByte of data (with a 8-byte acknowledgment in the reverse direction) using different numbers of messages:        16384 64Byte messages, vs 4096 256Byte messages, vs 1024 1024Byte messages. Use known message contents (for example number sequences) so they can be validated.
4) The same as (3), using UDP. 

For timing, use System.nanoTime (or the closest equivalent if using other languages). Read through the Java networking tutorial. Also see SimpleService.java and EchoClient.java for some stripped-down examples of using server and client sockets. When using non-CS machines and networks, minimize unnecessary traffic while developing your programs. Beware of firewalls.

---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

Project 2 specfification: 

Write a file transfer program. To demonstrate, you'll need a client and a server program:

- The server awaits connections.
- A client connects, and indicates the name of a file to upload or download.
- The client sends or receives the file. 

Wherever applicable, use the commands and protocol for TFTP (IETF RFC 1350), with the following modifications. You will need to design and use additional packet header information than that in TFTP; use the IETF 2347 TFTP Options Extension when possible.

- Use TCP-style sliding windows rather than the sequential acks used in TFTP. Test with at least two different max window sizes.
- Arrange that each session begins with a sender ID and (random) number exchange to generate a key to be used for encrypting data. You can use Xor to create key, or anything better, and use this as the       basis for randomized xoring (as in project 1) or similar protocols.
- Support only binary (octet) transmission.
- Support a command line argument controlling whether to pretend to drop 1 percent of the packets;
- When receiving files, place them in a temporary directory (for example /tmp) to avoid overwriting in place on shared file systems. Validate that they have the same contents. 

Create a web page showing throughput across varying conditions: at least 2 different clients hosts, different window sizes; drops vs no drops

----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
