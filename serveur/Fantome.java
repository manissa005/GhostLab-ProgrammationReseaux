package serveur;

public class Fantome{
	private Coordonnee coordonnee;
    
	public Fantome() {
    	this.coordonnee = null; 
    }

    public synchronized void supprimerFantome(){
        this.setCoordonnee(null);
    }
    
    public void setCoordonnee( Coordonnee coordonnee ) {
        this.coordonnee = coordonnee;
    }

    public Coordonnee getPosition() {
        return this.coordonnee;
    }
}

