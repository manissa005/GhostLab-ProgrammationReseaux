package serveur;
public class Coordonnee {
    private int x;
    private int y;
    
    public Coordonnee(int x, int y){
        this.x=x;
        this.y=y;
    }
    public String toString(){
        return "coordonnees  x: "+x+"  y: "+y;
    }
    public boolean equalsTo(Coordonnee cor){
        return cor.getX()==this.x &&cor.getY()==this.y;
    }
    public String getX_string() { 
        String chaine = x - 1 + "";
        return "0".repeat( 3 - chaine.length() ) + chaine;
    }
    public String getY_string() {
        String chaine = y - 1 + "";
        return "0".repeat( 3 - chaine.length() ) + chaine;
    }
    public int getX(){
        return this.x;
    }
    public int getY(){
        return this.y;
    }
    void setX(int x){
        this.x=x;
    }
    void setY(int y){
        this.y=y;
    }

}
