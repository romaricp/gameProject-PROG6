package ia;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;
import modele.*;
import moteur.*;

public class IntelligenceArtificielle {
	public enum difficulteIA{
		facile,
		normal,
		difficile
	}
	
	private difficulteIA niveauDifficulte;
	private Joueur joueurIA, joueurAdversaire;
	private Moteur moteur;
	private TourDeJeu tourDeJeuCourant;
	private boolean tourEnCours;
	private static final int MAX = 1000;
	private static final int MIN = -1000;
	
	public IntelligenceArtificielle(difficulteIA niveauDifficulte, Joueur joueurIA, Joueur joueurAdversaire, Moteur m){
		this.setNiveauDifficulte(niveauDifficulte);
		this.setJoueurIA(joueurIA);
		this.setJoueurAdv(joueurAdversaire);
		this.setMoteur(m); 
		this.setTourDeJeuCourant(new TourDeJeu()); // Ces deux variables servent pour la difficulté 
		this.setTourEnCours(false); 						   // intermédiaire (normal) et difficile qui renvoyent une 
	}														   // liste de points


	public Coup jouerIA(){
		
		Coup coupSolution = new Coup(new Point(-1,-1), new Point(-1,-1));
		Coup coupTemp;
		
		switch(this.getNiveauDifficulte()){
			case facile :
				if(!this.tourEnCours){
					this.setTourDeJeuCourant(this.coupFacile());
					this.setTourEnCours(true);
				}
			break;
			
			case normal :
				if(!this.tourEnCours){
					this.setTourDeJeuCourant(this.coupNormal());
					this.setTourEnCours(true);
				}	
			break;
			
			case difficile :
				coupSolution = this.coupDifficile();
			break;
			
			default : // difficulté normale
				if(!this.tourEnCours){
					this.setTourDeJeuCourant(this.coupNormal());
					this.setTourEnCours(true);
				}	
			break;
		}
		
		Iterator<Coup> it = this.getTourDeJeuCourant().getListeCoups().iterator();
		System.out.println("\n\n ****\t Résultat \t**** \n\n");
		this.moteur.t.dessineTableauAvecIntersections();
		while(it.hasNext()){
			Coup coupT = it.next();
			Point pDep = coupT.getpDepart(), pArr = coupT.getpArrivee();
			this.moteur.t.deplacement(pDep, pArr, this.joueurIA, new ArrayList<Point>());
			this.moteur.t.manger(this.joueurIA, this.moteur.t.recupereDirection(pDep, pArr), pDep, pArr, coupT.getChoixPrise());
			this.moteur.t.dessineTableauAvecIntersections();
		}
		
		
		ArrayList<Coup> listeCoupsDuTour = this.getTourDeJeuCourant().getListeCoups();
		coupTemp = listeCoupsDuTour.get(0).clone();
		coupSolution = coupTemp.clone();
		listeCoupsDuTour.remove(coupTemp);
			
		if(listeCoupsDuTour.isEmpty()) // Si la liste est vide on a terminé le tour
			tourEnCours = false;
		
		
		return coupSolution;
	}
	
	/*
	 * Applique l'algorithme permettant à l'ordinateur de jouer un coup en difficulté "facile"
	 * Paramètres : 	listePredecesseurs -> contient la liste des prédécesseurs et donc des points que l'on a pas le droit
	 * 					de retraverser pour ce tour
	 * 					pDep 			   -> dans le cadre d'un tour qui se prolonge (prises multiples) on indique le 
	 * 					point de départ qui est le point d'arrivée du coup précédent
	 */
	private TourDeJeu coupFacile(){
		ArrayList<TourDeJeu> listeToursJouables = new ArrayList<TourDeJeu>();
		Iterator<TourDeJeu> it;
		TourDeJeu tourSolution, tourTemp;
		int max = 0;
		
		// Récupération de tous les tours jouables pour le terrain et le joueur courant
		listeToursJouables = getToursJouables(this.moteur.t,this.getJoueurIA());

		tourSolution = listeToursJouables.get(0);
		
		it = listeToursJouables.iterator();
		
		while(it.hasNext()){
			tourTemp = it.next().clone();
			
			if(tourTemp.getValeurResultat() > max){
				max = tourTemp.getValeurResultat();
				tourSolution = tourTemp;
			}
		}
		return tourSolution;
	}
	
	public static Terrain.ChoixPrise choixPriseIAFacile(){
		Random rand = new Random();

		if(rand.nextInt(2) == 1)
			return Terrain.ChoixPrise.parAspiration;
		return Terrain.ChoixPrise.parPercussion;
	}
	
	/*
	 * Applique l'algorithme permettant à l'ordinateur de jouer un coup en difficulté "normal"
	 */
	private TourDeJeu coupNormal(){
		TourDeJeu tourSolution;
		int profondeur = 8;
		
		// ALPHA BETA
		tourSolution = alphaBeta(profondeur, this.moteur.t); // simule x-profondeur tours
															 // exemple : profondeur = 3
															 // on va simuler un tour jCourant puis un tour jAdv puis 
		return tourSolution;								 // de nouveau un tour jCourant	
	}
	
	/*
	 * Application de l'algorithme alpha beta
	 */
	private TourDeJeu alphaBeta(int profondeur, Terrain terrainCourant){
		ArrayList<TourDeJeu> listeToursJouables = new ArrayList<TourDeJeu>();
		Iterator<TourDeJeu> it;
		TourDeJeu tourCourant, tourSolution = null;
		Random rand = new Random();
		int valMax = MIN, valTemp, nbPionsManges;
		Integer alpha = new Integer(MIN), beta = new Integer(MAX);
		
		// Récupération de tous les tours jouables pour le terrain et le joueur courant
		listeToursJouables = getToursJouables(terrainCourant,this.getJoueurIA());

		it = listeToursJouables.iterator();
		
		while(it.hasNext()){
			tourCourant = (TourDeJeu) it.next().clone();
			
			nbPionsManges = tourCourant.getValeurResultat();
			
			valTemp = nbPionsManges + min(profondeur-1, alpha, beta, tourCourant.getTerrainFinal());
			
			if(valTemp > valMax){
				valMax = valTemp;
				tourSolution = (TourDeJeu) tourCourant.clone();
			}
			else if(valTemp == valMax){ // Choix randomisé si des solutions ont un résultat similaire
				if(rand.nextInt(2) == 1)
					tourSolution = (TourDeJeu) tourCourant.clone();
			}
			
		}

		return tourSolution;
	}
	
	/*
	 * Min :
	 */
	private int min(int profondeur, Integer alpha, Integer beta, Terrain terrainCourant){
		ArrayList<TourDeJeu> listeToursJouables = new ArrayList<TourDeJeu>();
		Iterator<TourDeJeu> it;
		TourDeJeu tourCourant;
		int valTemp = MAX, valRes = MAX;
		
		if(profondeur == 0)
			return 0;
		
		
		// Récupération de tous les tours jouables pour le terrain et le joueur courant
		listeToursJouables = getToursJouables(terrainCourant, this.getJoueurAdv());
		
		if(listeToursJouables.isEmpty()) // Si il n'y a plus de tours possibles l'IA a perdu (ou plutôt on a gagné)
			return MAX;
		
		it = listeToursJouables.iterator();
		
		while(it.hasNext()){
			tourCourant = (TourDeJeu) it.next().clone();

			valTemp = -(tourCourant.getValeurResultat()); // nombre de pions perdus (mangés par l'adversaire) en négatif

			valTemp += max(profondeur-1, alpha, beta, tourCourant.getTerrainFinal());
			
			if(valTemp < valRes)
				valRes = valTemp;
			
			if(alpha >= valTemp) // élagage
				return valTemp;
			
			
			beta = Math.min(beta,valRes);	
		}
		
		return valRes;
	}
	
	/*
	 * Max :
	 */
	private int max(int profondeur, Integer alpha, Integer beta,  Terrain terrainCourant){
		ArrayList<TourDeJeu> listeToursJouables = new ArrayList<TourDeJeu>();
		Iterator<TourDeJeu> it;
		TourDeJeu tourCourant;
		int valTemp = MIN, valRes = MIN;
		
		if(profondeur == 0)
			return 0;
		
		// Récupération de tous les tours jouables pour le terrain et le joueur courant
		listeToursJouables = getToursJouables(terrainCourant, this.getJoueurIA());

		if(listeToursJouables.isEmpty()) // Si il n'y a plus de tours possibles on a perdu
			return MIN;
		
		it = listeToursJouables.iterator();
		
		while(it.hasNext()){
			tourCourant = (TourDeJeu) it.next().clone();
			
			valTemp = tourCourant.getValeurResultat();
			valTemp += min(profondeur-1, alpha, beta, tourCourant.getTerrainFinal());
			
			if(valTemp > valRes)
				valRes = valTemp;
			
			if(valTemp >= beta)  // élagage 
				return valTemp;
			
			alpha = Math.max(alpha,valRes);
		}
		
		return valRes;
	}
	
	/*
	 * getToursJouables, renvoie une liste de liste de tous les tours jouables pour le joueur à un instant t
	 * paramètres : - ArrayList<Point> listePointsDeDepart
	 * 				- boolean priseObligatoire : permet un le traitement nécessaire pour les prises obligatoires
	 */
	private ArrayList<TourDeJeu> getToursJouables(Terrain cloneTerrain, Joueur joueurCourant){
		Point pDepartCourant, pArriveeCourante;
		ArrayList<Point> listePointsDeDepart, listeCoupsObligatoires;
		ArrayList<TourDeJeu> listeToursJouables = new ArrayList<TourDeJeu>(), listeToursTemp;
		TourDeJeu tourTemp;
		boolean priseObligatoire = false;
		
		Iterator<Point> itPointsDepart, itPointsArrivee;
		Iterator<TourDeJeu> itToursTemp;
		
		// Récupération de la liste des points de Départ possibles
		listeCoupsObligatoires = cloneTerrain.couplibre(joueurCourant.getJoueurID()); // On regarde si on a des coups obligatoires
		
		if(listeCoupsObligatoires.isEmpty()) // DEBUT DE TOUR - Sans coup obligatoire (mouvement libre n'amenant aucune prise)
			listePointsDeDepart = this.moteur.listePionsJouables(joueurCourant, cloneTerrain);
		else{							 // DEBUT DE TOUR - Avec coup/prise obligatoire
			listePointsDeDepart = listeCoupsObligatoires;
			priseObligatoire = true;
		}
		
		itPointsDepart = listePointsDeDepart.iterator();
		
		// Pour tous les points de départ possibles
		while(itPointsDepart.hasNext()){
			listeToursTemp = new ArrayList<TourDeJeu>(); // On initialise une nouvelle liste de tours de jeu
			pDepartCourant = (Point) itPointsDepart.next().clone();
			
			itPointsArrivee = this.moteur.deplacementPossible(pDepartCourant, new ArrayList<Point>(), cloneTerrain).iterator();
				
			if(priseObligatoire){ // Si on a des prises obligatoires il faut trier les solutions disponibles
				// pour tous les successeurs du point de départ courant
				while(itPointsArrivee.hasNext()){
					pArriveeCourante = (Point) itPointsArrivee.next().clone();
					Terrain.Direction dir = cloneTerrain.recupereDirection(pDepartCourant, pArriveeCourante);
					if(cloneTerrain.estUnePriseAspiration(pDepartCourant, dir) || cloneTerrain.estUnePrisePercussion(pDepartCourant, dir))
						getListeToursPourCoupDepart(listeToursTemp, new TourDeJeu(), new Coup(pDepartCourant, pArriveeCourante), cloneTerrain, new ArrayList<Point>(), 0, joueurCourant);
				}
			}
			else{	// Les tours ne sont ici constitués que d'un seul coup de gain 0
				while(itPointsArrivee.hasNext()){
					pArriveeCourante = (Point) itPointsArrivee.next().clone();
					cloneTerrain.deplacement(pDepartCourant, pArriveeCourante, joueurCourant, new ArrayList<Point>());
					tourTemp = new TourDeJeu(new Coup(pDepartCourant,pArriveeCourante));
					tourTemp.setTerrainFinal(cloneTerrain);
					listeToursTemp.add(tourTemp);
				}
			}
			
			itToursTemp = listeToursTemp.iterator();
			
			while(itToursTemp.hasNext()){ // on décompose la liste pour la recomposer
				tourTemp = (TourDeJeu) itToursTemp.next().clone();
				listeToursJouables.add(tourTemp.clone());
			}
		}
		return listeToursJouables;
	}
	
	/*
	 * getListeToursPourCoupDepart, renvoie tous les tours de jeu possible pour le coup de départ donné en paramètre
	 *				   cette méthode n'est appelée que lors d'un tour avec plusieurs prises
	 */
	public void getListeToursPourCoupDepart(ArrayList<TourDeJeu> listeToursComplets, TourDeJeu tourTemp, Coup coupDeDepart, Terrain cloneTerrain, ArrayList<Point> listePredecesseurs, int nbPionsManges, Joueur joueurCourant){
		Iterator<Point> itPointsArriveeSuivants;
		Terrain terrainCopie;
		Point pDep, pArr, pArrTemp;

		
		pDep = coupDeDepart.getpDepart();
		pArr = coupDeDepart.getpArrivee();
	
		terrainCopie = cloneTerrain.copie();
		
		terrainCopie.deplacement(pDep, pArr, joueurCourant, listePredecesseurs);
		nbPionsManges += terrainCopie.manger(joueurCourant, terrainCopie.recupereDirection(pDep, pArr), pDep, pArr,coupDeDepart.getChoixPrise()).size();
		listePredecesseurs.add(pDep);
		
		
		// Il y a aprés chaque prise possibilité de s'arrêter
		tourTemp.addCoup(coupDeDepart);
		tourTemp.setValeurResultat(nbPionsManges);
		tourTemp.setTerrainFinal(terrainCopie);
		listeToursComplets.add(tourTemp.clone());
				
		pDep = pArr;
		
		// On récupère les successeurs possibles à la position d'arrivée du coup joué
		itPointsArriveeSuivants = this.moteur.deplacementPossible(pDep, listePredecesseurs, cloneTerrain).iterator();

		while(itPointsArriveeSuivants.hasNext()){
			pArrTemp = (Point) itPointsArriveeSuivants.next().clone();
			Terrain.Direction dir = terrainCopie.recupereDirection(pDep, pArrTemp);
			if(terrainCopie.estUnePriseAspiration(pDep, dir) || terrainCopie.estUnePrisePercussion(pDep, dir))
				getListeToursPourCoupDepart(listeToursComplets, tourTemp.clone(), new Coup(pDep, pArrTemp), terrainCopie, listePredecesseurs, nbPionsManges, joueurCourant);
		}
	}
	
	/*
	 * Applique l'algorithme permettant à l'ordinateur de jouer un coup en difficulté "difficile"
	 */
	private Coup coupDifficile(){
		Coup pSolution = null;
		
		return pSolution;
	}
	
	public difficulteIA getNiveauDifficulte() {
		return niveauDifficulte;
	}

	public void setNiveauDifficulte(difficulteIA niveauDifficulte) {
		this.niveauDifficulte = niveauDifficulte;
	}

	public Joueur getJoueurIA() {
		return joueurIA;
	}

	public void setJoueurIA(Joueur joueurIA) {
		this.joueurIA = joueurIA;
	}

	public Joueur getJoueurAdv(){
		return joueurAdversaire;
	}

	private void setJoueurAdv(Joueur joueurAdversaire) {
		this.joueurAdversaire = joueurAdversaire;
	}

	public Moteur getMoteur() {
		return moteur;
	}

	public void setMoteur(Moteur m) {
		this.moteur = m;
	}

	public boolean isTourEnCours() {
		return tourEnCours;
	}

	public void setTourEnCours(boolean tourEnCours) {
		this.tourEnCours = tourEnCours;
	}

	private void setTourDeJeuCourant(TourDeJeu tourDeJeu) {
		this.tourDeJeuCourant = tourDeJeu;
	}
	
	private TourDeJeu getTourDeJeuCourant(){
		return this.tourDeJeuCourant;
	}
}
