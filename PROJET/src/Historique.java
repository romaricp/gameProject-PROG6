import java.awt.Point;
import java.util.ArrayList;


public class Historique {

	ArrayList<Terrain> histoPrincipal;
	ArrayList<Point> histoTour;
	int itPrincipal, itTour;
	boolean joueur; //true = j1, false = j2
	
	Historique() {
		histoPrincipal = new ArrayList<Terrain>();
		histoTour = new ArrayList<Point>();
		itPrincipal = 0;
	}
	
	void ajouterTour(Terrain t) {
		if(itPrincipal < histoPrincipal.size()-1) {
			for(int i = histoPrincipal.size()-1; i > itPrincipal; i--) { 
				histoPrincipal.remove(i);
			}
		}
		Terrain c = t.copie();
		histoPrincipal.add(c);
		itPrincipal = histoPrincipal.size()-1;
		//System.out.println("itprincipal"+itPrincipal);
		
	}
	
	void ajouterCoup(Point p) {
		histoTour.add((Point)p.clone());
		itTour++;
		
	}
	
	Terrain annuler() {
		if(histoPrincipal.isEmpty())
			return null;
		else {
			return histoPrincipal.get(itPrincipal--);
		}
	}
	
	Terrain refaire() {
		if(itPrincipal == histoPrincipal.size()-1)
			return null;
		else {
			return histoPrincipal.get(itPrincipal++);
		}
	}
	
	void effacerHistoTour() {
		histoTour = new ArrayList<Point>();
	}
	
	void afficher() {
		System.out.println("----------------");
		//for(int it = 0; it < histoPrincipal.size(); it++) {
			//Terrain tmp = histoPrincipal.get(it);
			Terrain tmp = histoPrincipal.get(itPrincipal);
			tmp.dessineTableauAvecIntersections();
		//}
		System.out.println("----------------");
	}
	
}
