package serveur;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;

public class Serveur implements Runnable {

	private final CommunicateFunctions communication;

	public Serveur( CommunicateFunctions communication ) {
		this.communication = communication;
	}

	private static LinkedList<Partie> listeParties = new LinkedList<>();


	public static LinkedList<Partie> getListParties(){
		return listeParties;
	}
	
	public static int getNbPartie() {
		return listeParties.size();
	}
	@Override
	public void run() {
		try {

			//connexione des listes de partie
			Traitement.envoyerListePartie( communication );

			Joueur joueur = null;

			while( true ) { //boucle pour commande
				String x = null;
				try {
					x = communication.serverRead();
				} catch( Exception e ) {
					e.printStackTrace();
				}
				if( x == null || this.communication.sock.isClosed() ) {
					if( joueur != null ) {
						System.out.println( "|" + joueur.getIdentifiant() + " has disconnected|" );
						joueur.getPartie().supprimerJoueur( joueur );
					}
					return;
				}

				System.out.println( "|" + x + "|" );
				try {
					if( x.equals( "GAME?***" ) ) { //GAMES*** affiche le nb de partie non lancÃ©
						Traitement.envoyerListePartie( communication);
					} else if( x.startsWith( "NEWPL" ) && x.endsWith( "***" ) ) {
						if( joueur != null ) {
							communication.serverWrite( Format.FormatByte( "REGNO***" ) );
						} else {
							joueur = Traitement.NouvellePartie( communication, x );

							if( joueur == null ) {
								communication.serverWrite( Format.FormatByte( "REGNO***" ) );
							} else {
								communication.serverWrite( Format.FormatByte( "REGOK",
																	new UpletByte( joueur.getPartie().getNum(), 1 )
										  , "***" ) );

							}
						}

					} else if( x.startsWith( "REGIS" ) && x.endsWith( "***" ) && joueur == null ) {
						joueur = Traitement.AnciennePartie( communication, x );
						if( joueur != null ) {  //REGIS id port m***
							communication.serverWrite( Format.FormatByte( "REGOK", new UpletByte( joueur.getPartie().getNum(), 1 ), "***" ) );
						} else {
							communication.serverWrite( Format.FormatByte( "REGNO***" ) );
						}
					} else if( x.equals( "UNREG***" ) && joueur != null ) { //UNREG*** quitte la partie
						communication.serverWrite( Format.FormatByte( "UNROK",
															new UpletByte( joueur.getPartie().getNum(), 1 ), "***" ) );

						joueur.getPartie().supprimerJoueur( joueur );
						joueur = null;
					} else if( x.equals( "START***" ) && joueur != null ) {
						joueur.setStart( true );
						if( joueur.getPartie().StartPartie() ) {
							joueur.getPartie().launchGame();
						}
					} else if( x.startsWith( "LIST?" ) && x.endsWith( "***" ) ) { //LIST? numPartie*** affiche les joueurs de la
						Traitement.ListeParties( x,communication );                      // partie demandÃ©

					} else if( x.startsWith( "SEND?" ) && x.endsWith( "***" ) && joueur != null ) {
						if( joueur.EnvoiMessageJoueur( x ) ) {
							communication.serverWrite( Format.FormatByte( "SEND!***" ) );
						} else {
							communication.serverWrite( Format.FormatByte( "NSEND***" ) );
						}

					} else if( x.startsWith( "DISC!" ) && x.endsWith( "***" ) && joueur == null ) { // se deconnecte
						communication.serverWrite( Format.FormatByte( "GOBYE***" ) );
						communication.sock.close();
						return;
					} else if( x.startsWith( "SIZE? " ) && x.endsWith( "***" ) ) {
						int m;
						try {
							m = Integer.parseInt( Traitement.listeMots( x )[1] );
						} catch( Exception e ) {
							communication.serverWrite(Format.FormatByte( "DUNNO***" ) );
							continue;
						}
						Partie p = null;
						for( Partie partie : listeParties ) {
							if( m == partie.getNum() )
								p = partie;
						}

						if( p == null ) {
							communication.serverWrite( Format.FormatByte( "DUNNO***" ) );
						} else {
							communication.serverWrite(Format.FormatByte( "SIZE!",
																new UpletByte( p.getNum(), 1 ),
																new UpletByte( p.getHauteur(), 2 ),
																new UpletByte( p.getLargeur(), 2 ),
																"***" ) );

						}

					} else if( x.startsWith( "UPMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getStarted() ) { //mouvement
						int pas;
						try {
							pas = Integer.parseInt( Traitement.listeMots( x )[1] );
						} catch( Exception e ) {
							communication.serverWrite( Format.FormatByte( "DUNNO***" ) );
							continue;
						}
						int score_avant = joueur.getScore();
						joueur.getPartie().getLabyrinthe().UpMouvement( joueur, pas );
					//	joueur.getPartie().getGame().refresh();
						joueur.getPartie().getLabyrinthe().InitialisationCaseJoueur();

						if( score_avant == joueur.getScore() ) {
							communication.serverWrite(Format.FormatByte( "MOVE!",
																joueur.getCoordonnee().getX_string(),
																joueur.getCoordonnee().getY_string(),
																"***" ) );

						} else {
							communication.serverWrite(Format.FormatByte( String.format( "MOVEF %s %s %s***",
																		   joueur.getCoordonnee().getX_string(),
																		   joueur.getCoordonnee().getY_string(),
																		   joueur.AfficheScore()  ) ) );

						}

					} else if( x.startsWith( "DOMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getStarted() ) { //mouvement
						int pas;
						try {
							pas = Integer.parseInt( Traitement.listeMots( x )[1] );
						} catch( Exception e ) {
							communication.serverWrite( Format.FormatByte( "DUNNO***" ) );
							continue;
						}
						int score_avant = joueur.getScore();
						joueur.getPartie().getLabyrinthe().DownMouvement( joueur, pas );
						//joueur.getPartie().getGame().refresh();
						joueur.getPartie().getLabyrinthe().InitialisationCaseJoueur();

						if( score_avant == joueur.getScore() ) {
							communication.serverWrite(Format.FormatByte( String.format( "MOVE! %s %s***", joueur.getCoordonnee().getX_string(), joueur.getCoordonnee().getY_string() ) ) );

						} else {
							communication.serverWrite( Format.FormatByte( String.format( "MOVEF %s %s %s***",
																		   joueur.getCoordonnee().getX_string(),
																		   joueur.getCoordonnee().getY_string(),
																		   joueur.AfficheScore() ) ) );

						}

					} else if( x.startsWith( "RIMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getStarted() ) { //mouvement
						int pas;
						try {
							pas = Integer.parseInt( Traitement.listeMots( x )[1] );
						} catch( Exception e ) {
							communication.serverWrite(Format.FormatByte( "DUNNO***" ) );
							continue;
						}
						int score_avant = joueur.getScore();
						joueur.getPartie().getLabyrinthe().RightMouvement( joueur, pas );
						joueur.getPartie().getLabyrinthe().InitialisationCaseJoueur();

						if( score_avant == joueur.getScore() ) {
							communication.serverWrite( Format.FormatByte( String.format( "MOVE! %s %s***",
																		   joueur.getCoordonnee().getX_string(),
																		   joueur.getCoordonnee().getY_string() ) ) );

						} else {
							communication.serverWrite( Format.FormatByte( String.format( "MOVEF %s %s %s***",
																		   joueur.getCoordonnee().getX_string(),
																		   joueur.getCoordonnee().getY_string(),
																		   joueur.AfficheScore() ) ) );

						}
					} else if( x.startsWith( "LEMOV" ) && x.endsWith( "***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getStarted() ) { //mouvement
						int pas;
						try {
							pas = Integer.parseInt( Traitement.listeMots( x )[1] );
						} catch( Exception e ) {
							communication.serverWrite(Format.FormatByte( "DUNNO***" ) );
							continue;
						}
						int score_avant = joueur.getScore();
						joueur.getPartie().getLabyrinthe().LeftMouvement( joueur, pas );
						joueur.getPartie().getLabyrinthe().InitialisationCaseJoueur();

						if( score_avant == joueur.getScore() ) {
							communication.serverWrite( Format.FormatByte( String.format( "MOVE! %s %s***",
																		   joueur.getCoordonnee().getX_string(),
																		   joueur.getCoordonnee().getY_string() ) ) );

						}

					} else if( x.equals( "IQUIT***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getStarted() ) {
						Partie p = joueur.getPartie();
						p.supprimerJoueur( joueur );
						//end game
						if( p.getNbJoueurs() == 0 ) {
						//	p.getLabyrinthe().fantomeMove.interrupt();
						}
						communication.serverWrite( Format.FormatByte( "GOBYE***" ) );
						communication.sock.close();
						return;
					} else if( x.equals( "GLIS?***" ) && joueur != null && joueur.getPartie() != null && joueur.getPartie().getStarted() ) {
						communication.serverWrite(Format.FormatByte(
								  "GLIS!",
								  new UpletByte( joueur.getPartie().getNbJoueurs(), 1 ),
								  "***" ) );

						for( Joueur j : joueur.getPartie().getListeJoueurs() ) { 
							communication.serverWrite(Format.FormatByte( String.format( "GPLYR %s %s %s %s***",
																		   j.getIdentifiant(),
																		   j.getCoordonnee().getX_string(),
																		   j.getCoordonnee().getY_string(),
																		   j.AfficheScore()
																		 ) ) );

						}
					} else if( x.startsWith( "MALL?" ) && joueur != null && joueur.getPartie().getStarted() ) {
						String str = x.substring( 6, x.length() - 3 );
						CommunicateFunctions.envoiUDP( joueur.getPartie().getAddresse(),
								Format.FormatByte( String.format( "MESSA %s %s+++",
																		 joueur.getIdentifiant(), str ) ) );
						communication.serverWrite( Format.FormatByte( "MALL!***" ) );
					} else {
						communication.serverWrite( Format.FormatByte( "DUNNO***" ) );
					}
				} catch( Exception ignored ) {
					communication.serverWrite( Format.FormatByte( "DUNNO***" ) );
				}
			}

		} catch( SocketException ignored ) {
		} catch( Exception e ) {
			try {
				communication.sock.close();
			} catch( IOException ignored ) {
			}
		}
	}
	public static void main( String[] args ) {

		try {

			ServerSocket servSocket = null;
			int port = 4243;
			while( servSocket == null ) {
				try {
					servSocket = new ServerSocket( port );
				} catch( Exception e ) {
					port++;
				}
			}
			System.out.println( "le serveur ecoute sur le port : " + port );
			while( true ) {
				Socket socket = servSocket.accept();
				try {
					Serveur serveurRunnable = new Serveur( new CommunicateFunctions( socket ) );
					Thread t = new Thread( serveurRunnable );
					synchronized( t ) {
						t.start();
					}
				} catch( IOException e ) {
					socket.close();
				}
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}

}