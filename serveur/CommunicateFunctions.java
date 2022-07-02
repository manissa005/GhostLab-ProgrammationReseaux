package serveur;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class CommunicateFunctions {

	public final Socket sock;
	public final DataInputStream input;
	public final DataOutputStream output; 

	public CommunicateFunctions(Socket sock) throws IOException {
		this.sock = sock;
		this.input = new DataInputStream( sock.getInputStream() );
		this.output = new DataOutputStream( sock.getOutputStream() );
	}

	public String serverRead() {
		int size;
		int cpt = 0; // un compteur pour les * et + à la fin d'un message
		ByteBuffer m = ByteBuffer.allocate( 15 ); // on alloue 15 oct pour un mot
		String msg = ""; // le message à lire
		try {
			byte[] reqArray = new byte[5];
			size = input.read( reqArray, 0, 5 ); // on lit les 5 premiers octets = requete
			if( size != 5 )
				return null;
			String req = new String( reqArray );
			msg += req;
			while(true) {
				byte[] oct = new byte[1];
				size = input.read( oct, 0, 1 );
				char c = new String( oct ).charAt( 0 );

				if(c == ' ') {
					if( m.position() > 0 ) {
						msg += " " + byteToString( req, m.array() ).trim();
						m = ByteBuffer.allocate( 15 );
					}
				}else if(c == '*' || c == '+') {
					if(cpt == 2) {
						if( m.position() > 0 ) {
							msg += " " + byteToString( req, m.array() ).trim();
						}
						if( c == '+' )
							msg += "+++";
						else
							msg += "***";
						break;
					} else {
						cpt++;
					}
				} else {
					m.put( oct );
				}
			}
		} catch( IOException e ) {
			return null;
		}
		return msg;
	}

	public String byteToString(String req, byte[] m ) { // traitement du mot m selon la requete req
		int size = m.length;
		while( size > 0 && m[size-1] == 0x0 ) { // on ignore les octets 0x0
			size=size-1;
		}
		if( size == 0 ) {
			return "0";
		} else if(size == 1) {
			return Byte.toUnsignedInt( m[0] ) + "";
		} else if( size == 2 ) {
			return ( ( ( m[1] & 0xff ) << 8 ) | ( m[0] & 0xff ) ) + "";
		}else if(req.equals("MALL?") || req.equals("SEND?")) {
			return new String( m );
		}  else {
			return new String( m );
		}
	}

	public void serverWrite(byte[] mess ) throws IOException {
		output.write( mess ); // envoyer un message
		output.flush();
	}

	public static void envoiUDP(InetSocketAddress address, byte[] oct ) throws IOException {
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket paquet = new DatagramPacket( oct, oct.length, address );
		socket.send( paquet );
		socket.close();
	}

}