package serveur;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.LinkedList;

public class Partie {

	private static int nb = 0; //numero incremente pour chaque nouvelle partie
	private static int port= 5144; // port incremente pour chaque nouvelle partie
	private int num;
	private final InetSocketAddress addresse; // adresse MULTI-CAST
	private final LinkedList<Joueur> listeJoueurs;
	private boolean started = false;
	//private Jeu jeu;
	private Labyrinthe labyrinthe;

	public Partie() {
		listeJoueurs = new LinkedList<>();
		this.num = nb;
		nb++;
		addresse = new InetSocketAddress( "224.42.51.44", port );
		port++;
		Serveur.getListParties().add( this );
	}

	/*
	 * Fonction qui prend un joueur et l'ajoute dans une partie
	 * renvoi false si l'identifiant du joueur existe deja ou si la parti a commence
	 * true sinon
	 */
	public boolean ajouterJoueur( Joueur joueur ) {
		if( !this.started ) {
			for( Joueur j : this.listeJoueurs ) {
				if( j.getIdentifiant().equals( joueur.getIdentifiant() ) )
					return false;
			}
			this.listeJoueurs.add( joueur );
			return true;
		}
		else
			return false;	
	}

	/*
	 * Fonction qui verifie si tous les joueurs ont envoye START
	 * retourne true si cest le cas
	 */
	public boolean StartPartie() {
		for( Joueur j : this.listeJoueurs ) {
			if( !j.getStart() ) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Fonction qui supprime un joueur d'une partie et supprime la partie
	 * si elle n'a plus de joueurs
	 */
	public void supprimerJoueur( Joueur joueur ) {
		if( this.listeJoueurs.contains( joueur ) ) {
			this.listeJoueurs.remove( joueur );
			if( labyrinthe != null ) {
				labyrinthe.getLabyrinthe()[joueur.getCoordonnee().getX()][joueur.getCoordonnee().getY()] = 0;
				//jeu.refresh();
			}
			if( getNbJoueurs() == 0 ) {
				supprimerPartie();
			} /*else if( !started && StartPartie() ) {
				launchGame();
			}*/
		}
	}

	public void launchGame() {
		started= true;
		//jeu = new Jeu( this );
		byte[] welco = Format.FormatByte( "WELCO",
										  new UpletByte( num, 1 ),
										  new UpletByte( getHauteur(), 2 ),
										  new UpletByte( getLargeur(), 2 ),
										  new UpletByte( labyrinthe.getNbFantomes(), 1 ),
										  getAdresseIp(),
										  getPort(),
										  "***" );
		for( Joueur joueur : listeJoueurs ) {
			try {
				joueur.communication.serverWrite( welco );
				joueur.communication.serverWrite( Format.FormatByte( String.format(
						  "POSIT %s %s %s***",
						  joueur.getIdentifiant(),
						  joueur.getCoordonnee().getX_string(),
						  joueur.getCoordonnee().getY_string()
										) ) );
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Fonction qui supprime une partie de la liste des parties du serveur
	 * verifie s'il y a un gagnant avant la suppression et envoi un message au gagnant
	 */
	public void supprimerPartie() {
		//if( this.jeu != null ) {
			/*if( this.jeu.affichage != null )
				this.jeu.affichage.dispose();
			this.jeu.go = false;*/
			Joueur gagnant = null;
			for( Joueur j : listeJoueurs) {
				if( gagnant == null || ( j != null && j.getScore() > gagnant.getScore() ) )
					gagnant = j;
			}
			if( gagnant != null ) {
				byte[] endga = Format.FormatByte(
						  String.format( "ENDGA %s %s+++", gagnant.getIdentifiant(), gagnant.AfficheScore() )
										);
				for( Joueur j : listeJoueurs ) {
					try {
						CommunicateFunctions.envoiUDP( getAddresse(), endga );
						j.communication.sock.close();
					} catch( IOException ignored ) {
					}
				}
			}
		//}
		Serveur.getListParties().remove( this );
	}

	
	//GETTERS 
	
	/*
	 * Fonction qui renvoie le port de la partie en string
	 */
	private String getPort() {
		String port = addresse.getPort() + "";
		port = "0".repeat( 4 - port.length() ) + port;
		return port;
	}
	/*
	 * Fonction qui renvoie l'adresse IP de la partie en string
	 */
	private String getAdresseIp() {
		return addresse.getHostName() + "#".repeat( 15 - addresse.getHostName().length() );
	}
	
	public InetSocketAddress getAddresse() {
		return addresse;
	}

	public int getHauteur() {
		return labyrinthe.getHauteur();
	}

	public int getLargeur() {
		return labyrinthe.getLargeur();
	}
/*	public Jeu getGame() {
		return this.jeu;
	} */

	public int getNbJoueurs() {
		return listeJoueurs.size();
	}

	public boolean getStarted() {
		return this.started;
	}

	public int getNum() {
		return this.num;
	}

	public LinkedList<Joueur> getListeJoueurs() {
		return this.listeJoueurs;
	}
	public Labyrinthe getLabyrinthe() {
		return this.labyrinthe;
	}

}
