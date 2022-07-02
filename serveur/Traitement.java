package serveur;

import serveur.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
public class Traitement {

	/*
	 * Fonction qui prend le message "NEWPL", retourne un nouveau joueur 
	 * inscrit dans une nouvelle partie cree
	 */
    public static Joueur NouvellePartie(  CommunicateFunctions communication, String message ) {
        String[] infos = listeMots( message );
        String id = infos[1];
        String port_string = infos[2];
        int port;
        try {
            port = Integer.parseInt( port_string );
            for( int i = 0; i < Serveur.getListParties().size(); i++ ) {
                for( int j = 0; j < Serveur.getListParties().get( i ).getListeJoueurs().size(); j++ ) {
                    if( port == Serveur.getListParties().get( i ).getListeJoueurs().get( j ).getPort() ) {
                        return null;
                    }
                }
            }
        } catch( Exception e ) {
            return null;
        }
        if( port_string.length() != 4 || id.length() != 8)
            return null;
        Joueur joueur = new Joueur( communication, id, port );
        Partie partie = new Partie();
        partie.ajouterJoueur( joueur );
        joueur.partie = partie;
        return joueur;
    }

    /*
     * Fonction qui prends en argument le message "REGIS"
     * retourne un nouveau joueur inscrit dans la partie choisit
     */
    public static Joueur AnciennePartie(  CommunicateFunctions communication, String message ) {
        String[] infos = listeMots( message );
        String id = infos[1];
        String port_string = infos[2];
        String partie_string = infos[3];
        if( port_string.length() != 4 || id.length() != 8  ) {
            return null;
        }
        int port;
        int num_partie;
        try {
            port = Integer.parseInt( port_string );
            for( int i = 0; i < Serveur.getListParties().size(); i++ ) {
                for( int j = 0; j < Serveur.getListParties().get( i ).getListeJoueurs().size(); j++ ) {
                    if( port == Serveur.getListParties().get( i ).getListeJoueurs().get( j ).getPort() ) {
                        return null;
                    }
                }
            }
            num_partie= Integer.parseInt( partie_string );
        } catch( Exception e ) {
            return null;
        }
        Joueur joueur = new Joueur( communication, id, port );
        for( Partie partie : Serveur.getListParties() ) {
            if( partie.getNum() == num_partie ) {
                if( partie.ajouterJoueur( joueur ) ) {
                    joueur.partie = partie;
                    return joueur;
                } else {
                    return null;
                }
            }
        }
        return null;
    }
    
    /*
     * Fonction qui prend le message "GAME?" , envoi directement 
     * la listes des parties non commencés au client
     * et la liste des joueures de chaque partie 
     */
    public static void ListeParties( String message,  CommunicateFunctions communication ) throws IOException {
        String id_string = listeMots( message )[1];
        int id;
        try {
            id = Integer.parseInt( id_string );
        } catch( Exception e ) {
            e.printStackTrace();
            communication.serverWrite( Format.FormatByte( "DUNNO***" ) );
            return;
        }
        for( Partie partie : Serveur.getListParties() ) {
            if( partie.getNum() == id ) {
                listePartie( communication, partie );
                return;
            }
        }
        communication.serverWrite( Format.FormatByte( "DUNNO***" ) );
    }
    
    /*
     * Fonction qui envoie la liste des parties
     * et le nombre de joueurs de chaque partie 
     */
   	public static void envoyerListePartie(  CommunicateFunctions communication ) {
   		try {
   			communication.serverWrite( Format.FormatByte( "GAMES", new UpletByte( Serveur.getNbPartie(), 1 ), "***" ) );
   			for( Partie partie : Serveur.getListParties()) {
   				if( !partie.getStarted() && partie.getNbJoueurs() > 0 ) {
   					byte[] b = Format.FormatByte( "OGAME",
   										  new UpletByte( partie.getNum(), 1 ),
   										  new UpletByte( partie.getNbJoueurs(), 1 ),
   										  "***" );
   					communication.serverWrite( b );
   				}
   			}
   		} catch( Exception e ) {
   			e.printStackTrace();
   		}
   	}

   	/*
   	 * Fonction qui repond au message "LIST!"
   	 * et  envoi la liste des joueurs d'une partie
   	 */
   	public static void listePartie( CommunicateFunctions communication, Partie partie) throws IOException {
   		communication.serverWrite(Format.FormatByte(
   				  "LIST!",
   				  new UpletByte( partie.getNum(), 1 ),
   				  new UpletByte( partie.getListeJoueurs().size(), 1 ), "***" ) );
   		for( Joueur joueur : partie.getListeJoueurs() ) {
   			communication.serverWrite( Format.FormatByte( String.format( "PLAYR %s***", joueur.getIdentifiant() ) ) );
   		}
   	}
   	
    
    /*
     * Fonction qui prend un message string terminant avec "***"
     * et renvoi la liste des mots du message
     */
    public static String[] listeMots( String message ) {
		if( message.endsWith( "***" ) )
			return message.substring( 0, message.length() - 3 ).split( " " );
		return message.split( " " );
	}

}
