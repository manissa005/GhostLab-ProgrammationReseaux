
package serveur;
import java.io.IOException;
import java.util.*;
import serveur.CommunicateFunctions;
//import serveur.Format;
import serveur.Joueur;
import serveur.Partie;
import static java.lang.Thread.sleep;

public class Labyrinthe {

	private static final Random random = new Random();
	private Partie partie;
	private final LinkedList<Joueur> Listejoueurs;
	private LinkedList<Fantome> Listefantomes;
	private int hauteur, largeur; // ceux là comptent aussi la bordure! ne pas envoyer au client
	int[][] labyrinthe;
	//boolean border = true;
	ArrayList<Coordonnee> Chemins; //tout position de chemin
	Deque<Integer>[][] posJoueurNbr;

	@SuppressWarnings( "unchecked" )
	public Labyrinthe(Partie partie ) {
		Listefantomes = new LinkedList<>();
		this.partie = partie;
		this.hauteur = 10;
		this.largeur = 10;
		this.labyrinthe = new int[hauteur][largeur];
		this.Chemins = new ArrayList<>();
		//this.jeu = jeu;
		this.Listejoueurs = this.partie.getListeJoueurs();
		this.posJoueurNbr = new ArrayDeque[hauteur][largeur];
		initialiationLab( false );
	}
	
	/*
	 * Fonction qui deplcae un joueur vers le haut avec le "pas"
	 * verifie si un fantome est capture et met a jour le labyrinthe
	 * et le nombres de fantome
	 */
	public synchronized void UpMouvement( Joueur j, int pas ) throws IOException {
		Coordonnee cor = j.getCoordonnee();
		for( int i = 0; i < pas; i++ ) {
			if( this.labyrinthe[cor.getX() - 1][cor.getY()] == 1 || this.labyrinthe[cor.getX() - 1][cor.getY()] == -1 ) {
				break;
			} else {
				if( this.labyrinthe[cor.getX() - 1][cor.getY()] == 2 ) {
					supprimerFantome( j, new Coordonnee( cor.getX() - 1, cor.getY() ) );
					j.setScore( j.getScore() + 1 );
					this.CaptureFantome( j );
				}
				this.posJoueurNbr[cor.getX()][cor.getY()].pop();
				if( this.posJoueurNbr[cor.getX()][cor.getY()].isEmpty() ) {
					this.labyrinthe[cor.getX()][cor.getY()] = 0;
				}
				this.posJoueurNbr[cor.getX() - 1][cor.getY()].push( 3 );
			} 
			cor.setX( cor.getX() - 1 );
		}
		this.labyrinthe[cor.getX()][cor.getY()] = 3;
		j.setCoordonnee( cor );
		if( getNbFantomes() == 0 )
			this.partie.supprimerPartie();
		print();
	}

	
	/*
	 * Fonction qui deplcae un joueur vers la droite avec le "pas"
	 * verifie si un fantome est capture et met a jour le labyrinthe
	 * et le nombres de fantome
	 */
	public synchronized void RightMouvement( Joueur j, int pas ) throws IOException {
		Coordonnee cor = j.getCoordonnee();
		for( int i = 0; i < pas; i++ ) {
			if( this.labyrinthe[cor.getX()][cor.getY() + 1] == 1 || this.labyrinthe[cor.getX()][cor.getY() + 1] == -1 ) {
				break;
			} else {
				if( this.labyrinthe[cor.getX()][cor.getY() + 1] == 2 ) {
					//retire Fantome
					j.setScore( j.getScore() + 1 );
					supprimerFantome( j, new Coordonnee( cor.getX(), cor.getY() + 1 ) );
					//envoie message
					this.CaptureFantome( j );
				}
				this.posJoueurNbr[cor.getX()][cor.getY()].pop();
				if( this.posJoueurNbr[cor.getX()][cor.getY()].isEmpty() ) {
					this.labyrinthe[cor.getX()][cor.getY()] = 0;
				}
				this.posJoueurNbr[cor.getX()][cor.getY() + 1].push( 3 );

			}
			cor.setY( cor.getY() + 1 );
		}
		this.labyrinthe[cor.getX()][cor.getY()] = 3;
		j.setCoordonnee( cor );
		if( getNbFantomes() == 0 )
			this.partie.supprimerPartie();
		print();
	}

	
	/*
	 * Fonction qui deplcae un joueur vers la gauche avec le "pas"
	 * verifie si un fantome est capture et met a jour le labyrinthe
	 * et le nombres de fantome
	 */
	public synchronized void LeftMouvement( Joueur j, int pas ) throws IOException {
		Coordonnee cor = j.getCoordonnee();
		for( int i = 0; i < pas; i++ ) {
			if( this.labyrinthe[cor.getX()][cor.getY() - 1] == 1 || this.labyrinthe[cor.getX()][cor.getY() - 1] == -1 ) {
				break;
			} else {
				if( this.labyrinthe[cor.getX()][cor.getY() - 1] == 2 ) {
					//effacce fantome
					j.setScore( j.getScore() + 1 );
					supprimerFantome( j, new Coordonnee( cor.getX(), cor.getY() - 1 ) );
					//envoie message
					this.CaptureFantome( j );
				}
				this.posJoueurNbr[cor.getX()][cor.getY()].pop();
				if( this.posJoueurNbr[cor.getX()][cor.getY()].isEmpty() ) {
					this.labyrinthe[cor.getX()][cor.getY()] = 0;
				}
				this.posJoueurNbr[cor.getX()][cor.getY() - 1].push( 3 );
			}
			cor.setY( cor.getY() - 1 );
		}

		j.setCoordonnee( cor );
		this.labyrinthe[cor.getX()][cor.getY()] = 3;
		if( getNbFantomes() == 0 )
			this.partie.supprimerPartie();
		print();
	}

	
	/*
	 * Fonction qui deplcae un joueur vers le bas avec le "pas"
	 * verifie si un fantome est capture et met a jour le labyrinthe
	 * et le nombres de fantome
	 */
	public synchronized void DownMouvement( Joueur j, int pas ) throws IOException {
		Coordonnee cor = j.getCoordonnee();
		for( int i = 0; i < pas; i++ ) {
			if( this.labyrinthe[cor.getX() + 1][cor.getY()] == 1 || this.labyrinthe[cor.getX() + 1][cor.getY()] == -1 ) {
				break;
			} else {
				if( this.labyrinthe[cor.getX() + 1][cor.getY()] == 2 ) {
					j.setScore( j.getScore() + 1 );
					supprimerFantome( j, new Coordonnee( cor.getX() + 1, cor.getY() ) );
					this.CaptureFantome( j );
				}
				this.posJoueurNbr[cor.getX()][cor.getY()].pop();
				if( this.posJoueurNbr[cor.getX()][cor.getY()].isEmpty() ) {
					this.labyrinthe[cor.getX()][cor.getY()] = 0;
				}
				this.posJoueurNbr[cor.getX() + 1][cor.getY()].push( 3 );

			}
			cor.setX( cor.getX() + 1 );
		}
		j.setCoordonnee( cor );
		this.labyrinthe[cor.getX()][cor.getY()] = 3;
		if( getNbFantomes() == 0 )
			this.partie.supprimerPartie();
		print();
	}

	/* 
	 * Fonction qui initialise la labyrinthe avec des 1
	 * utilise make : 
	 * utilise 
	 */
	public void initialiationLab( boolean bool ) {
		for( int i = 0; i < hauteur; i++ ) {//remplir labyrinthe par 1    1:mur 0:chemin
			for( int j = 0; j < largeur; j++ ) {
				this.labyrinthe[i][j] = 1;
				this.posJoueurNbr[i][j] = new ArrayDeque<>();
			}
		}
		CheminValide( bool );
		Bougerfantome();
		initialisationTousJoueurs();
		initialisationFile();
	}

	/*
	 * Fonction qui intialise les cases joueurs avec le numero 3
	 */
	public void initialisationFile() {
		for( Joueur j : Listejoueurs ) {
			this.posJoueurNbr[j.getCoordonnee().getX()][j.getCoordonnee().getY()].push( 3 );
		}
	}

	/*
	 * Fonction qui cree des chemins valides dans le labyrinthe
	 */
	public void CheminValide( boolean bool ) {
		//setBorderOn();
		Deque<Coordonnee> pile = new ArrayDeque<>();
		int posX = 1, posY = 1;
		if( bool ) {
			posX = random.nextInt( this.hauteur - 1 ) + 1;
			posY = random.nextInt( this.largeur - 1 ) + 1;
		}
		Coordonnee cor = new Coordonnee( posX, posY );
		while( true ) {
			this.labyrinthe[cor.getX()][cor.getY()] = 0;
			Chemins.add( new Coordonnee( cor.getX(), cor.getY() ) );
			pile.push( cor );
			cor = VersCor( cor );
			if( pile.isEmpty() ) {
				break;
			}
			if( cor.getY() == -1 && cor.getX() == -1 ) {
				pile.pop();
				if( pile.isEmpty() ) {
					break;
				}
				cor = pile.pop();
			}
		}
	}

	
	public Coordonnee VersCor( Coordonnee depart ) {
		Deque<Integer> deq = new ArrayDeque<>();
		ArrayList<Integer> cor = new ArrayList<>( Arrays.asList( 0, 1, 2, 3 ) );
		int cpt = 4, newX = 0, newY = 0;
		for( int i = 0; i < 4; i++ ) {//insertion
			int v = random.nextInt( cpt );
			deq.push( cor.get( v ) );
			cor.remove( v );
			cpt--;
		}
		while( true ) {
			if( deq.isEmpty() ) {
				return new Coordonnee( -1, -1 );
			}
			int orien = deq.pop();
			switch( orien ) {
				case 0://haut
					newX = depart.getX() - 1;
					newY = depart.getY();
					break;
				case 1://droite
					newX = depart.getX();
					newY = depart.getY() + 1;
					break;
				case 2://bas
					newX = depart.getX() + 1;
					newY = depart.getY();
					break;
				case 3://gauche
					newX = depart.getX();
					newY = depart.getY() - 1;
					break;
			}
			if( ( !( this.labyrinthe[newX][newY] == -1 ) ) && ( !( this.labyrinthe[newX][newY] == 0 ) ) ) {
				Coordonnee newPos = new Coordonnee( newX, newY );
				if( CaseValide( newPos ) ) {
					return newPos;
				}
			}
		}
	}

	/*
	 * Fonction qui teste une case du labyrinthe si elle est valide ou pas
	 * renvoi true si on peut se deplacer vers une autre case depuis la case "cor"
	 * false sinon
	 */
	public boolean CaseValide( Coordonnee cor ) {
		int corX = cor.getX(), corY = cor.getY(), valide = 0;
		if( this.labyrinthe[corX - 1][corY] == 0 ) {//up
			valide += 1;
		}
		if( this.labyrinthe[corX][corY + 1] == 0 ) {//right
			valide += 1;
		}
		if( this.labyrinthe[corX + 1][corY] == 0 ) {//down
			valide += 1;
		}
		if( this.labyrinthe[corX][corY - 1] == 0 ) {//left
			valide += 1;
		}
		if(valide < 2)
			return true;
		else
			return false;
	}

	/*
	 * Fonction qui initialise toutes les cases des joueurs avec le numero 3
	 */
	public void InitialisationCaseJoueur() {
		for( Joueur j : Listejoueurs ) {
			this.labyrinthe[j.getCoordonnee().getX()][j.getCoordonnee().getY()] = 3;
		}
	}

	public synchronized void initialisationTousJoueurs() {
		int nb_chemin = this.Chemins.size();
		int nb_joueurs = this.Listejoueurs.size();
		for( Joueur j : Listejoueurs ) {
			int rand = random.nextInt( this.Chemins.size() );
			Coordonnee cor = this.Chemins.get( rand );
			while( nb_chemin > nb_joueurs && labyrinthe[cor.getX()][cor.getY()] != 0 ) {
				rand = random.nextInt( this.Chemins.size() );
				cor = this.Chemins.get( rand );
			}
			j.setCoordonnee( cor );
		}
		InitialisationCaseJoueur();
	}

	//fantomes
	public synchronized boolean Bougerfantome() {
		if( Listefantomes.size() == 0 ) {
			for( int x = 0; x < Listejoueurs.size(); x++ ) {
				Listefantomes.add( new Fantome() );
			}
		}
		for( Fantome f : this.Listefantomes ) {
			int i = random.nextInt( Chemins.size() );
			if( f.getPosition() != null ) {
				this.labyrinthe[f.getPosition().getX()][f.getPosition().getY()] = 0;
			}
			Coordonnee cor = Chemins.get( i );
			int valid = 100;
			while( valid >= 0 && this.labyrinthe[cor.getX()][cor.getY()] != 0 ) {
				i = random.nextInt( this.Chemins.size() );
				cor = this.Chemins.get( i );
				valid--;
			}
			if( valid< 0 )
				return false;
			f.setCoordonnee( cor );
			try {
				String fantomeSig = String.format( "GHOST %s %s+++", cor.getX_string(), cor.getY_string() );
				CommunicateFunctions.envoiUDP( this.partie.getAddresse(), Format.FormatByte( fantomeSig ) );
			} catch( IOException ignored ) {
			}
			this.labyrinthe[cor.getX()][cor.getY()] = 2;
		}
		return true;
	}

	/*
	 * Fonction qui supprime un fantome a la position du joueur donnee de la liste des fantomes
	 * et lui envoi un message "MOVEF" avec son score
	 */
	public synchronized void supprimerFantome( Joueur j, Coordonnee cor ) throws IOException {
		LinkedList<Fantome> ListeF = new LinkedList<>();
		for( Fantome f : this.Listefantomes ) {
			if( f.getPosition() != null ) {
				if( f.getPosition().equalsTo( cor ) ) {
					f.supprimerFantome();
				} else {
					ListeF.add( f );
				}
			}
		}
		this.Listefantomes.clear();
		this.Listefantomes = ListeF;
		j.communication.serverWrite( Format.FormatByte( String.format( "MOVEF %s %s %s***",
										 cor.getX_string(),
										 cor.getY_string(),
										 j.AfficheScore() ) ) );
	}

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI = "\u001B[42m";
	public static final String ANSI_RED = "\u001B[31m";

	public void print() {//affiche

		for( int i = 0; i < hauteur; i++ ) {
			for( int j = 0; j < largeur; j++ ) {
				if( this.labyrinthe[i][j] == -1 ) {
					System.out.print( "$ " );
				} else if( this.labyrinthe[i][j] == 1 ) {
					System.out.print( "@ " );
				} else if( this.labyrinthe[i][j] == 0 ) {
					System.out.print( "  " );
				} else if( this.labyrinthe[i][j] == 2 ) {
					System.out.print( ANSI_RED + "S " + ANSI_RESET );
				} else {
					System.out.print( ANSI + "V " + ANSI_RESET );
				}

			}
			System.out.println();
		}
	}

	
	/*
	 * Fonction qui envoi un message avec le nouveau score au joueur
	 * ayant capture un fantome avec son port UDP
	 */
	public void CaptureFantome( Joueur joueur ) {
		String NouveauScore = String.format( "SCORE %s %s %s %s+++",
										joueur.getIdentifiant(),
										joueur.AfficheScore(),
										joueur.getCoordonnee().getX_string(),
										joueur.getCoordonnee().getY_string() );
		try {
			CommunicateFunctions.envoiUDP( partie.getAddresse(), Format.FormatByte( NouveauScore ) );
		} catch( IOException ignored ) {
		}
	}
	
	
	//THREAD MOVE FANTOME
	boolean bouge = true;
	Thread MouvementFantome = new Thread( () -> {
		while( true) {
			try {
				sleep( 60000 );
			} catch( InterruptedException ignored ) {
			}
			 bouge = this.Bougerfantome();
		}
	} );
	
	public int[][] getLabyrinthe() {
		return labyrinthe;
	}

	public int getNbFantomes() {
		return Listefantomes.size();
	}
	public int getHauteur() {
		return this.hauteur;
	}
	public int getLargeur() {
		return this.largeur;
	}

}
