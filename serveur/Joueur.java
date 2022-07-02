package serveur;

import java.io.*;
import java.net.*;
public class Joueur {

	public CommunicateFunctions communication;
	private final String identifiant;
	public Partie partie;
	private boolean start = false;
	private final int port;
	private int score = 0;
	private Coordonnee coordonnee;

	public Joueur( CommunicateFunctions communication, String identifiant, int port ) {
		this.coordonnee = null;
		this.communication= communication;
		this.identifiant = identifiant;
		this.port = port;
	}

	public String getIdentifiant() {
		return this.identifiant;
	}

	public int getPort() {
		return this.port;
	}

	public Partie getPartie() {
		return this.partie;
	}

	public boolean getStart() {
		return this.start;
	}

	public void setStart( boolean start ) {
		this.start = start;
	}

	public int getScore() {
		return this.score;
	}
	public void setScore( int score ) {
		this.score = score;
	}
	public void setCoordonnee( Coordonnee coordonnee) {
        this.coordonnee = coordonnee;
    }

    public Coordonnee getCoordonnee() {
        return this.coordonnee;
    }

	/*
	 * 	Fonction qui envoi le score du joueur sous forme de string
	 */
	public String AfficheScore() { 
		String score_string = score + "";
		return "0".repeat( 4 - score_string.length() ) + score_string;
	}

	/*
	 * Fonction qui envoi un message a un joueur de la meme partie que l'expediteur
	 */
	public boolean EnvoiMessageJoueur( String message ) {

		String[] infos = Traitement.listeMots( message );
		if( partie == null )
			return false;
		String ident = infos[1];
		String mess = "";
		for( int i = 2; i < infos.length; i++ ) {
			mess += infos[i] + ( i < infos.length - 1 ? " " : "" );
		}

		//System.out.println( "id|" + id + "|" );
		for( Joueur joueur : partie.getListeJoueurs() ) {
			if( joueur.getIdentifiant().equals( ident ) ) {
				try {
					byte[] data = Format.FormatByte( String.format(
							  "MESSP %s %s+++", getIdentifiant(), mess
										  ) );
					DatagramPacket paquet = new DatagramPacket( data, data.length,
										joueur.communication.sock.getInetAddress(),
										joueur.port
					);
					DatagramSocket socket = new DatagramSocket();
					socket.send( paquet );
					socket.close();
					return true;
				} catch( Exception e ) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}


}
