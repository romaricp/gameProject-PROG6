package moteur;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import ia.*;
import ihm.*;
import modele.*;
import modele.Case.Etat;
import modele.Parametres.NiveauJoueur;
import modele.Terrain.Direction;
import reseau.*;

/**
 * Classe contenant l'ensemble des règles du jeu. Fait office de contrôleur et donne les instructions d'affichage à l'IHM. Instancie les classes Terrain et IHM. Fonctionne comme un automate.
 */
public class Moteur {
	public static void main(String[] args) {
		Moteur m = new Moteur();
		IHM i = new IHM();
		i.setVisible(true);
		i.com = new Communication(i, m, Communication.IHM);
		m.com = new Communication(i, m, Communication.MOTEUR);
		// m.init();
		i.nouvellePartie();
	}

	/**
	 * Définit les différents états de l'automate
	 */
	public enum EtatTour {
		/**
		 * Etat d'attente de séléction de pion par le joueur.
		 */
		selectionPion,
		/**
		 * Etat d'attente de séléction d'une destination par le joueur.
		 */
		selectionDestination,
		/**
		 * Etat d'attente de séléction d'un choix par le joueur dans le cas d'une prise multiple.
		 */
		attenteChoix,
		/**
		 * Etat où l'IA joue.
		 */
		jeuxIa,
		/**
		 * Etat où la partie est terminée.
		 */
		partieFinie;
	}

	/**
	 * Canal de communication entre le moteur et l'IHM.
	 */
	public Communication com;
	/**
	 * Terrain de jeu.
	 */
	public Terrain t;

	/**
	 * Historique de la partie.
	 */
	private Historique h;
	/**
	 * Etat du tour en cours (de l'automate).
	 */
	private EtatTour e;
	/**
	 * Point de départ du coup en cours.
	 */
	private Point pDepart;
	/**
	 * Point d'arrivé du coup en cours.
	 */
	private Point pArrive;
	/**
	 * Joueur du tour en cours.
	 */
	private Joueur joueurCourant;
	/**
	 * Joueur 1 (blanc).
	 */
	public Joueur j1;
	/**
	 * Joueur 2 (noir).
	 */
	public Joueur j2;
	/**
	 * Réception et envoi de l'echange sur le canal de communication.
	 */
	private Echange ech;
	/**
	 * Point d'aspiration si prise avec choix.
	 */
	private Point aspi;
	/**
	 * Point de percussion si prise avec choix.
	 */
	private Point perc;
	/**
	 * Vrai si un enchaînement de coup est en cours. Faux sinon.
	 */
	private boolean tourEnCours;
	/**
	 * Liste des points jouables en début de tour (coups obligatoires ou disponibilité de coups libres).
	 */
	private ArrayList<Point> listePointDebut;
	/**
	 * Coup joué par l'IA.
	 */
	private Coup jeuIa;
	/**
	 * Vrai si affichage sur console. Faux sinon. Utilisé en debug.
	 */

	private boolean trace = false;
	/**
	 * Nombre de coups sans prise.
	 */
	private int compteurNul;

	/**
	 * Intelligence artificelle utiliser pour l'aide.
	 */
	private IntelligenceArtificielle iaAide;

	/**
	 * Constructeur par défaut.
	 */
	public Moteur() {
		j1 = new Joueur(Case.Etat.joueur1, Joueur.typeJoueur.humain, "Joueur 1");
		j2 = new Joueur(Case.Etat.joueur2, Joueur.typeJoueur.ordinateur, IntelligenceArtificielle.difficulteIA.normal, j1, t);
	}

	/**
	 * Constructeur utilisé dans le cas d'un chargement de partie.
	 * @param t Le terrain à charger pour reprendre la partie.
	 */
	public Moteur(Terrain t) {
		this.t = t;
		h = new Historique();
		ech = new Echange();
	}

	/**
	 * Initialise le moteur. Cette methode n'est pas dans le constructeur car si une nouvelle partie est lancée par l'utilisateur, le moteur ne peut pas se construir lui-même.
	 */
	public void init() {
		t = new Terrain();

		// t.TerrainTest(11);
		j1.resetScore();
		j2.resetScore();
		h = new Historique();
		h.ajouterTour(t);
		ech = new Echange();
		listePointDebut = new ArrayList<Point>();

		// j2 = new Joueur(Case.Etat.joueur2, Joueur.typeJoueur.humain,
		// "Joueur 2");
		// j1 = new Joueur(Case.Etat.joueur1, Joueur.typeJoueur.ordinateur,
		// IntelligenceArtificielle.difficulteIA.facile, j2, this);

		joueurCourant = j1;
		compteurNul = 0;
		// calculerScore();

		// message("bandeauSup", joueurCourant.getNom());
		// message("bandeauInf", "Selection du pion");

		gestionEvenementGraphique(joueurCourant.getNom(), "Selection du pion", joueurCourant.getJoueurID().getNum());

		if (joueurCourant.isJoueurHumain()) {
			e = EtatTour.selectionPion;
		} else {
			e = EtatTour.jeuxIa;
			jouerIa();
		}

	}

	public void init(Object dataValue) {
		t = new Terrain();
		j1.resetScore();
		j2.resetScore();
		// t.TerrainTest(11);
		h = new Historique();
		h.ajouterTour(t);
		ech = new Echange();
		listePointDebut = new ArrayList<Point>();
		joueurCourant = j1;
		int[] score = { j1.getScore(), j2.getScore() };
		ech.vider();
		ech.ajouter("aide",false);
		com.envoyer(ech,joueurCourant.getJoueurID().getNum());
		gestionEvenementGraphique(null,null,null, score, null,null);
		gestionBouton();
		actionParametre(dataValue);
		
	}

	/**
	 * Détermine si des prises sont réalisables parmis les déplacements possibles.
	 * @param p Point à partir duquel on essaye de déterminer des prises.
	 * @param listePredecesseurs ArrayList de Points. Liste des points par lesquels est passé le pion durant le tour.
	 * @return ArrayList de Points. Liste des arrivées possibles pour lesquelles une prise sera effectuée.
	 */
	public ArrayList<Point> prisePossible(Point p, ArrayList<Point> listePredecesseurs) {
		ArrayList<Point> listePrise = new ArrayList<Point>();
		ArrayList<Point> listeMouvement = t.deplacementPossible(p, listePredecesseurs);
		Iterator<Point> it = listeMouvement.iterator();
		while (it.hasNext()) {
			Point temp = (Point) it.next().clone();
			Terrain.Direction d = t.recupereDirection(p, temp);
			if (t.estUnePriseAspiration(p, d) || t.estUnePrisePercussion(p, d))
				listePrise.add(temp);
		}
		return listePrise;
	}

	/**
	 * Test à chaque fin de tour si la partie est terminée.
	 * @param aucunDeplacement Permet de savoir si la partie est bloquée.
	 * @return Vrai si la partie a été gagnée par un joueur, si elle est bloquée ou si c'est un match nul.
	 * Faux sinon.
	 */
	public boolean partieTerminee(boolean aucunDeplacement) {
		ech.vider();
		if (joueurCourant.scoreNul() || aucunDeplacement) {
			Joueur gagnant = joueurCourant.recupereJoueurOpposant(joueurCourant, j1, j2, false);
			Joueur perdant = joueurCourant;
			String BandeauSup = gagnant.getNom();
			String BandeauInf = "a remporté la partie";
			EvenementGraphique cgv = new EvenementGraphique(BandeauSup, BandeauInf, EvenementGraphique.FinPartie.VICTOIRE);
			EvenementGraphique cgd = new EvenementGraphique(BandeauSup, BandeauInf, EvenementGraphique.FinPartie.DEFAITE);
			if (com.enReseau()) {
				ech.ajouter("coup", cgv);
				com.envoyer(ech, gagnant.getJoueurID().getNum());
				ech.vider();
				ech.ajouter("coup", cgd);
				com.envoyer(ech, perdant.getJoueurID().getNum());
			} else {
				if (j1.isJoueurHumain() == j2.isJoueurHumain())
					gestionEvenementGraphique(BandeauSup, BandeauInf, EvenementGraphique.FinPartie.VICTOIRE);
				else {
					if (joueurCourant.isJoueurHumain())
						gestionEvenementGraphique(BandeauSup, BandeauInf, EvenementGraphique.FinPartie.DEFAITE);
					else
						gestionEvenementGraphique(BandeauSup, BandeauInf, EvenementGraphique.FinPartie.VICTOIRE);
				}
			}
			gestionEvenementGraphique(BandeauSup, BandeauInf);
			com.envoyer(ech);
			return true;
		} else if (compteurNul == 40) {
			String BandeauSup = "Match nul";
			String BandeauInf = "Trop de coups sans prise joués";
			gestionEvenementGraphique(BandeauSup, BandeauInf, EvenementGraphique.FinPartie.NUL);
			return true;
		}
		return false;
	}

	/**
	 * Etat de l'automate où le moteur reçoit le pion sélectionné par le joueur ou l'IA.
	 * Test si le pion sélectionné correspond aux règles. 
	 * @param p Pion sélectionné
	 */
	public void selectionPion(Point p) {
		tourEnCours = false;

		if (t.getCase(p.x, p.y).getOccupation() != joueurCourant.getJoueurID()) {
		} else {
			listePointDebut = t.listePionsJouables(joueurCourant);
			if (listePointDebut.isEmpty())
				partieTerminee(true);
			if (listePointDebut.contains(p)) {
				pDepart = p;
				if (joueurCourant.isJoueurHumain()) {
					ech.vider();
					ech.ajouter("pionSelectionne", pDepart);
					com.envoyer(ech);
					e = EtatTour.selectionDestination;
					gestionEvenementGraphique(null, "Choisir la destination");
				}
				

			}
		}
	}

	/**
	 * Etat de l'automate où le moteur reçoit la destination sélectionnée par le joueur ou l'IA
	 * Test si la destination est conforme aux règles.
	 * @param p Point sélectionné pour effectuer un déplacement.
	 */
	public void selectionDestination(Point p) {
		if (!tourEnCours && listePointDebut.contains(p)) {
			pDepart = p;
			if (joueurCourant.isJoueurHumain()) {
				ech.vider();
				ech.ajouter("pionDeselectionne", true);
				ech.ajouter("pionSelectionne", pDepart);
				com.envoyer(ech);
			}
		} else {
			ArrayList<Point> l = prisePossible(pDepart, h.histoTour);
			if (l.isEmpty()) {
				l = t.deplacementPossible(pDepart, h.histoTour);
			}
			if (l.contains(p)) {
				pArrive = p;
				Terrain.Direction d = t.recupereDirection(pDepart, pArrive);
				boolean priseAspi = t.estUnePriseAspiration(pDepart, d);
				boolean prisePercu = t.estUnePrisePercussion(pDepart, d);
				if (t.deplacement(pDepart, pArrive, joueurCourant, h.histoTour) == 0) {
					Point[] tabPts = { pDepart, pArrive };
					gestionEvenementGraphique(tabPts, null, null, null);
					prise(priseAspi, prisePercu);
					tourEnCours = true;
				}
			}
		}

	}

	/**
	 * Effectue une prise en fonction des points de départ et d'arrivé en attribut.
	 * @param priseAspi Vrai si une prise par aspiration est disponible. Faux sinon.
	 * @param prisePercu Vrai si une prise par percusion est disponible. Faux sinon.
	 */
	public void prise(boolean priseAspi, boolean prisePercu) {
		Terrain.Direction d = t.recupereDirection(pDepart, pArrive);
		ArrayList<Point> l = new ArrayList<Point>();
		
		h.ajouterCoup(pDepart, pArrive);
		if (priseAspi && prisePercu) {
			compteurNul = 0;
			Terrain.ChoixPrise choix;
			if (joueurCourant.isJoueurHumain()) {
				Point offA = t.offsetAspiration(d, pDepart);
				aspi = new Point(offA.x + pDepart.x, offA.y + pDepart.y);
				Point offP = t.offsetPercussion(d, pArrive);
				perc = new Point(offP.x + pArrive.x, offP.y + pArrive.y);
				Point[] tabPts = { aspi, perc };
				gestionEvenementGraphique(null, tabPts, null, null, null, "choisissez votre prise");
				e = EtatTour.attenteChoix;
				ech.vider();
				ech.ajouter("finTour", false);
			} else {
				choix = jeuIa.getChoixPrise();
				l = t.manger(joueurCourant, d, pDepart, pArrive, choix);
				majScore(l.size());
				int[] score = { j1.getScore(), j2.getScore() };

				gestionEvenementGraphique(null, null, l, score);
				traceTerrain();

			}
		} else if (priseAspi && !prisePercu) {
			compteurNul = 0;
			l = t.manger(joueurCourant, d, pDepart, pArrive, Terrain.ChoixPrise.parAspiration);
			majScore(l.size());
			int[] score = { j1.getScore(), j2.getScore() };

			gestionEvenementGraphique(null, null, l, score);
			traceTerrain();

			if (joueurCourant.isJoueurHumain()) {
				testFinTour();
				ech.vider();
				ech.ajouter("finTour", true);
				com.envoyer(ech, joueurCourant.getJoueurID().getNum());
			}

		} else if (!priseAspi && prisePercu) {
			compteurNul = 0;
			l = t.manger(joueurCourant, d, pDepart, pArrive, Terrain.ChoixPrise.parPercussion);
			majScore(l.size());
			int[] score = { j1.getScore(), j2.getScore() };

			gestionEvenementGraphique(null, null, l, score);
			traceTerrain();
			if (joueurCourant.isJoueurHumain()) {
				testFinTour();
				ech.vider();
				ech.ajouter("finTour", true);
				com.envoyer(ech, joueurCourant.getJoueurID().getNum());
			}
		} else {
			compteurNul++;
			if (joueurCourant.isJoueurHumain())
				finTour();
		}
	}

	/**
	 * Termine le tour en cours et change le joueur courant. Peut être appelée automatiquement si le joueur courant ne peut plus effectuer de prise, ou manuellement s'il décide de s'arrêter pendant un
	 * enchaînement.
	 */
	public void finTour() {
		traceTerrain();
		if (joueurCourant.getJoueurID() == Case.Etat.joueur1)
			joueurCourant = j2;
		else
			joueurCourant = j1;
		h.effacerHistoTour();
		h.ajouterTour(t);
		ech.vider();
		ech.ajouter("pionDeselectionne", true);
		ech.ajouter("annuler", false);
		ech.ajouter("refaire", false);
		ech.ajouter("finTour", false);
		ech.ajouter("aide",false);
		ech.ajouter("aide", false);

		com.envoyer(ech);

		ech.vider();
		gestionBouton();
		if (partieTerminee(false)) {
			e = EtatTour.partieFinie;
		} else {
			tourEnCours = false;
			traceTerrain();
			gestionEvenementGraphique();
			if (joueurCourant.isJoueurHumain())	
				gestionEvenementGraphique(joueurCourant.getNom(), "Selection du pion", joueurCourant.getJoueurID().getNum());
			else 
				gestionEvenementGraphique(joueurCourant.getNom(), "en train de jouer", joueurCourant.getJoueurID().getNum());
			if (joueurCourant.isJoueurHumain()) {
				ech.vider();
				ech.ajouter("aide", true);
				com.envoyer(ech,joueurCourant.getJoueurID().getNum());
				e = EtatTour.selectionPion;
			} else {
				ech.vider();
				ech.ajouter("annuler", false);
				com.envoyer(ech);
				e = EtatTour.jeuxIa;
				jouerIa();
			}
		}
	}

	/**
	 * Test après chaque prise si le tour peut se terminer ou si un enchaînement est réalisable.
	 */
	public void testFinTour() {
		pDepart = pArrive;
		if (prisePossible(pDepart, h.histoTour).isEmpty()) {
			finTour();
		} else {
			e = EtatTour.selectionDestination;
			gestionEvenementGraphique(null, "Selection destination");
		}
	}

	/**
	 * Met à jour le score de l'adversaire du joueur courant après une prise.
	 * @param nbPionsManges Nombre de pions mangés à l'adversaire.
	 */
	public void majScore(int nbPionsManges) {
		Joueur.recupereJoueurOpposant(joueurCourant, j1, j2, false).setScore(nbPionsManges);
	}

	/**
	 * Envoie un message à afficher sur un bandeau de l'IHM.
	 * @param destination Bandeau de destination.
	 * @param message Message à afficher.
	 */
	public void message(String destination, String message) {
		ech.vider();
		ech.ajouter(destination, message);
		com.envoyer(ech);
	}

	/**
	 * Envoie toutes les informations necessaires à l'IHM pour réaliser l'actualisation de l'affichage lié à un coup.
	 * @param deplacement Tableau de deux Points contenant le point de départ et le point d'arrivé.
	 * Peut être à null en fonction de la situation.
	 * @param choixPrise Tableau de deux Points contenant un choix à faire entre une prise par aspiration ou par percussion.
	 * Peut être null s'il n'y a pas de choix à faire.
	 * @param pionsManges Liste des pions mangés pendant le coup.
	 * @param score Score des joueurs mis à jour en fonctions des pions mangés.
	 */
	public void gestionEvenementGraphique(Point[] deplacement, Point[] choixPrise, ArrayList<Point> pionsManges, int[] score) {
		ech.vider();
		EvenementGraphique cg = new EvenementGraphique(deplacement, choixPrise, pionsManges, score, calculChemin());
		ech.ajouter("coup", cg);
		com.envoyer(ech);
	}

	/**
	 * Surchage de gestionEvenementGraphique pour le cas ou on coup inclut un changement de bandeau.
	 * @param deplacement Tableau de deux Points contenant le point de départ et le point d'arrivé.
	 * Peut être à null en fonction de la situation.
	 * @param choixPrise Tableau de deux Points contenant un choix à faire entre une prise par aspiration ou par percussion.
	 * Peut être null s'il n'y a pas de choix à faire.
	 * @param pionsManges Liste des pions mangés pendant le coup.
	 * @param score Score des joueurs mis à jour en fonctions des pions mangés.
	 * @param chaine1 Définie sur quel bandeau ira le message.
	 * @param chaine2 Le message à afficher sur le bandeau.
	 */
	public void gestionEvenementGraphique(Point[] deplacement, Point[] choixPrise, ArrayList<Point> pionsManges, int[] score, String chaine1, String chaine2) {
		ech.vider();
		EvenementGraphique cg = new EvenementGraphique(deplacement, choixPrise, pionsManges, score, chaine1, chaine2, calculChemin());
		ech.ajouter("coup", cg);
		com.envoyer(ech);
	}

	/**
	 * Surchage de gestionEvenementGraphique pour le cas ou l'on envoie un terrain uniquement.
	 */
	public void gestionEvenementGraphique() {
		ech.vider();
		traceTerrain();
		Terrain t2 = t.copie();
		EvenementGraphique cg = new EvenementGraphique(t2.getTableau());
		ech.ajouter("coup", cg);
		com.envoyer(ech);
	}

	/**
	 * Surchage de gestion gestionEvenementGraphique pour le cas ou l'on ne met à jour que les bandeaux.
	 * @param bandeauSup Message pour le bandeau supérieur.
	 * @param bandeauInf Message pour le bandeau inférieur.
	 */
	public void gestionEvenementGraphique(String bandeauSup, String bandeauInf) {
		ech.vider();
		EvenementGraphique cg;
		if (tourEnCours)
			cg = new EvenementGraphique(null, null, null, null, bandeauSup, bandeauInf, calculChemin());
		else
			cg = new EvenementGraphique(null, null, null, null, bandeauSup, bandeauInf, null);
		ech.ajouter("coup", cg);
		com.envoyer(ech);
	}

	/**
	 * Surchage de gestion gestionEvenementGraphique pour le cas ou l'on ne met à jour que les bandeaux et pour transmettre le joueur courant.
	 * @param bandeauSup Message pour le bandeau supérieur.
	 * @param bandeauInf Message pour le bandeau inférieur.
	 * @param i Identifiant du joueur courant sur le réseau.
	 */
	public void gestionEvenementGraphique(String bandeauSup, String bandeauInf, int i) {
		ech.vider();
		EvenementGraphique cg;
		cg = new EvenementGraphique(bandeauSup, bandeauInf, i);
		ech.ajouter("coup", cg);
		com.envoyer(ech);
	}

	/**
	 * Surchage de gestion gestionEvenementGraphique pour le cas ou l'on ne met à jour que les bandeaux dans le cas d'une fin de partie.
	 * @param bandeauSup Message pour le bandeau supérieur.
	 * @param bandeauInf Message pour le bandeau inférieur.
	 * @param fp Définit l'animation à afficher.
	 */
	public void gestionEvenementGraphique(String bandeauSup, String bandeauInf, EvenementGraphique.FinPartie fp) {
		ech.vider();
		EvenementGraphique cg;
		cg = new EvenementGraphique(bandeauSup, bandeauInf, fp);
		ech.ajouter("coup", cg);
		com.envoyer(ech);
	}

	/**
	 * Permet de recalculer les scores des joueurs. Utilisée dans les cas de annuler/refaire et lors d'un chargement de partie.
	 */
	public void calculerScore() {
		int scoreJ1 = 0;
		int scoreJ2 = 0;
		for (int i = 0; i < Terrain.LIGNES; i++) {
			for (int j = 0; j < Terrain.COLONNES; j++) {
				if (t.tableau[i][j].getOccupation() == Case.Etat.joueur1)
					scoreJ1++;
				if (t.tableau[i][j].getOccupation() == Case.Etat.joueur2)
					scoreJ2++;
			}
		}
		j1.chargerScore(scoreJ1);
		j2.chargerScore(scoreJ2);
	}

	/**
	 * Permet de calculer le chemin du pion pendant le tour.
	 * @return Liste de Points correspondant aux positions occupées par le pion durant l'enchaînement.
	 */
	public ArrayList<Point> calculChemin() {
		ArrayList<Point> chemin = (ArrayList<Point>) h.histoTour.clone();
		if (!chemin.contains(pDepart))
			chemin.add(pDepart);
		if (!chemin.contains(pArrive))
			chemin.add(pArrive);
		return chemin;
	}

	/**
	 * Fait jouer l'IA lors de son tour et gère les échanges entre l'IA et le moteur.
	 */
	public void jouerIa() {
		Thread th = new Thread() {
			public void run() {
				com.envoyer(new Echange("chargement",true));
				do {
					
					jeuIa = joueurCourant.jouer();
					selectionPion(jeuIa.getpDepart());
					selectionDestination(jeuIa.getpArrivee());
					traceTerrain();
				} while (joueurCourant.IaContinue());
				com.envoyer(new Echange("chargement",false));
				finTour();
			}
		};
		th.start();
	}

	/**
	 * Permet de griser ou d'afficher les boutons annuler/refaire en fonction de l'état d'affiche de l'historique.
	 */
	public void gestionBouton() {
		Echange ech2 = new Echange();
		int i = h.getItPrincipal();
		if (Joueur.recupereJoueurOpposant(joueurCourant, j1, j2, false).isJoueurHumain()) {
			if (i <= 0) {
				ech2.ajouter("annuler", false);
				if (i < h.histoPrincipal.size()-1) {
					ech2.ajouter("refaire", true);
					}
			} else {
				ech2.ajouter("annuler", true);
				if (i == h.histoPrincipal.size()-1) {
					ech2.ajouter("refaire", false);
				} else {
					ech2.ajouter("refaire", true);
				}
			}
		} else {
			if (i <= 1) {
				ech2.ajouter("annuler", false);
				if (i < h.histoPrincipal.size()-1) {
					ech2.ajouter("refaire", true);
					}
			} else {
				ech2.ajouter("annuler", true);
				if (i == h.histoPrincipal.size()-1) {
					ech2.ajouter("refaire", false);
				} else {
					ech2.ajouter("refaire", true);
				}
			}
		}
		com.envoyer(ech2, joueurCourant.getJoueurID().getNum());
	}

	/**
	 * Affiche l'état actuel du terrain en console. Utilisée uniquement en debug.
	 */
	public void traceTerrain() {
		if (trace)
			t.dessineTableauAvecIntersections();
	}

	/**
	 * Dirige l'automate en fonction de ce que l'IHM envoie et qui est lié à la seléction de pions.
	 * @param dataValue Point reçu de l'IHM via la méthode action.
	 */
	public void actionPoint(Object dataValue) {
		
		if (e == EtatTour.selectionPion) {
			selectionPion((Point) dataValue);
		} else if (e == EtatTour.selectionDestination) {
			selectionDestination((Point) dataValue);
		} else if (e == EtatTour.attenteChoix) {
			Terrain.Direction d = t.recupereDirection(pDepart, pArrive);
			ArrayList<Point> l = new ArrayList<Point>();
			boolean tperc = perc.equals((Point) dataValue);
			boolean taspi = aspi.equals((Point) dataValue);
			if (tperc || taspi) {
				if (tperc) {
					l = t.manger(joueurCourant, d, pDepart, pArrive, Terrain.ChoixPrise.parPercussion);
				} else if (taspi) {
					l = t.manger(joueurCourant, d, pDepart, pArrive, Terrain.ChoixPrise.parAspiration);
				}
				majScore(l.size());
				int[] score = { j1.getScore(), j2.getScore() };
				gestionEvenementGraphique(null, null, l, score);
				ech.vider();
				ech.ajouter("finTour", true);
				com.envoyer(ech, joueurCourant.getJoueurID().getNum());
				traceTerrain();
				testFinTour();
			}
		}
	}

	/**
	 * Réalise une annulation sur commande de l'IHM et lui envoi les modifications necessaires.
	 */
	public void actionAnnuler() {
		if (e != EtatTour.partieFinie) {
			if (compteurNul != 0) {
				compteurNul--;
			}
			if (tourEnCours) {
				t.setTableau(h.getDernierTerrain().copie().getTableau());
				e = EtatTour.selectionPion;
				tourEnCours=false;
				h.effacerHistoTour();
				gestionEvenementGraphique(null,"selection Pion");
				gestionEvenementGraphique();
				
				
				
			} else {
				ech.vider();
				Terrain annulation = h.annuler();
				if (annulation != null) {
					joueurCourant = joueurCourant.recupereJoueurOpposant(joueurCourant, j1, j2, false);
					if (!joueurCourant.isJoueurHumain()) {
						annulation = h.annuler();
						if (annulation != null) {
							joueurCourant = joueurCourant.recupereJoueurOpposant(joueurCourant, j1, j2, false);
							t.setTableau(annulation.getTableau());
							ech.ajouter("terrain", annulation.getTableau());

						}
					} else {
						t.setTableau(annulation.getTableau());
						ech.ajouter("terrain", annulation.getTableau());
					}
				}
				calculerScore();
				int[] tabScore = { j1.getScore(), j2.getScore() };
				ech.ajouter("score", tabScore);
				ech.ajouter("pionDeselectionne",true);
				com.envoyer(ech);
				gestionBouton();
				message("bandeauSup", joueurCourant.getNom());
				message("bandeauInf", "Selection du pion");
				if (joueurCourant.isJoueurHumain()) {
					e = EtatTour.selectionPion;
				} else {
					e = EtatTour.jeuxIa;
					j2 = new Joueur(j2); 
					jouerIa();
				}
			}
		}
	}

	/**
	 * Refait un coup sur commande de l'IHM et lui envoi les modifications necessaires.
	 */
	public void actionRefaire() {
		if (e != EtatTour.partieFinie) {
			compteurNul++;
			ech.vider();
			Case[][] refaire = h.refaire().getTableau();
			if (refaire != null) {
				joueurCourant = joueurCourant.recupereJoueurOpposant(joueurCourant, j1, j2, false);
				if (!joueurCourant.isJoueurHumain()) {
					refaire = h.refaire().getTableau();
					if (refaire != null) {
						joueurCourant = joueurCourant.recupereJoueurOpposant(joueurCourant, j1, j2, false);
						t.setTableau(refaire);
						ech.ajouter("terrain", refaire);
					}
				} else {
					t.setTableau(refaire);
					ech.ajouter("terrain", refaire);
				}
			}
			calculerScore();
			int[] tabScore = { j1.getScore(), j2.getScore() };
			ech.ajouter("score", tabScore);
			ech.ajouter("pionDeselectionne",true);
			com.envoyer(ech);
			gestionBouton();
			message("bandeauSup", joueurCourant.getNom());
			message("bandeauInf", "Selection du pion");
			if (joueurCourant.isJoueurHumain()) {
				e = EtatTour.selectionPion;
			} else {
				e = EtatTour.jeuxIa;
				jouerIa();
			}
		}
	}

	/**
	 * Sérialise et sauvegarde la partie dans un fichier.
	 * @param dataValue Référence du fichier sur lequel la sauvegarde sera effectuée.
	 */
	public void actionSauvegarder(Object dataValue) {
		Sauvegarde s = new Sauvegarde(t, h, j1, j2, joueurCourant);
		ObjectOutputStream oos = null;
		try {
			final FileOutputStream fichier = new FileOutputStream((File) dataValue);
			oos = new ObjectOutputStream(fichier);
			oos.writeObject(s);
		} catch (final java.io.IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (oos != null) {
					oos.flush();
					oos.close();
				}
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Déserialise et charge un partie à partir d'un fichier.
	 * @param dataValue Référence du fichier à partir duquel charger.
	 */
	public void actionCharger(Object dataValue) {
		ObjectInputStream ois = null;
		try {
			final FileInputStream fichier = new FileInputStream((File) dataValue);
			ois = new ObjectInputStream(fichier);
			Sauvegarde chargement = (Sauvegarde) ois.readObject();
			t = new Terrain(chargement.plateau);
			h = new Historique(chargement.histo);
			
			/*
			j1= new Joueur(chargement.joueur1);
			j2= new Joueur(chargement.joueur2);
			joueurCourant = new Joueur(chargement.joueurCourant);*/
			
			if (chargement.joueur1.isJoueurHumain())
				j1 = new Joueur(chargement.joueur1);
			else {
				j1 = new Joueur(Case.Etat.joueur1, Joueur.typeJoueur.ordinateur, chargement.joueur1.getIA().getNiveauDifficulte(), chargement.joueur2, t);
				j1.chargerScore(chargement.joueur1.getScore());
			}
			
			
			if (chargement.joueur2.isJoueurHumain())
				j2 = new Joueur(chargement.joueur2);
			else {
				j2 = new Joueur(Case.Etat.joueur2, Joueur.typeJoueur.ordinateur, chargement.joueur2.getIA().getNiveauDifficulte(), chargement.joueur2, t);
				
				j2.chargerScore(chargement.joueur2.getScore());
			}
			joueurCourant = new Joueur(chargement.joueurCourant);
			
		} catch (final java.io.IOException e) {
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ois != null) {
					ois.close();
				}
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		}
		gestionEvenementGraphique();
		calculerScore();
		int[] tabScore = { j1.getScore(), j2.getScore() };
		ech.ajouter("score", tabScore);
		com.envoyer(ech);
		envoiParametre();
		gestionBouton();
		if (joueurCourant.isJoueurHumain()) {
			e = EtatTour.selectionPion;
		} else {
			e = EtatTour.jeuxIa;
			jouerIa();
		}
	}

	/**
	 * Met à jour les paramètres de la partie en fonction de ce qui est envoyé par l'IHM.
	 * @param dataValue Paramètres de la partie.
	 */
	public void actionParametre(Object dataValue) {
		Parametres p = (Parametres) dataValue;
		
		if (p.j1_type != null) {
			if (p.j1_type == Parametres.NiveauJoueur.HUMAIN) {
				j1.setJoueurHumain(true);
				j1.viderIa();
			} else {
				j1.setJoueurHumain(false);
				if (p.j1_type == Parametres.NiveauJoueur.FACILE)
					j1.chargerIa(IntelligenceArtificielle.difficulteIA.facile, j2, t);
				else if (p.j1_type == Parametres.NiveauJoueur.MOYEN)
					j1.chargerIa(IntelligenceArtificielle.difficulteIA.normal, j2, t);
				else if (p.j1_type == Parametres.NiveauJoueur.DIFFICILE)
					j1.chargerIa(IntelligenceArtificielle.difficulteIA.difficile, j2, t);
			}
		}
		if (p.j2_type != null) {
			if (p.j2_type == Parametres.NiveauJoueur.HUMAIN) {
				j2.setJoueurHumain(true);
				j2.viderIa();
			} else {
				j2.setJoueurHumain(false);
				if (p.j2_type == Parametres.NiveauJoueur.FACILE)
					j2.chargerIa(IntelligenceArtificielle.difficulteIA.facile, j1, t);
				else if (p.j2_type == Parametres.NiveauJoueur.MOYEN)
					j2.chargerIa(IntelligenceArtificielle.difficulteIA.normal, j1, t);
				else if (p.j2_type == Parametres.NiveauJoueur.DIFFICILE)
					j2.chargerIa(IntelligenceArtificielle.difficulteIA.difficile, j1, t);
			}
		}

		if (p.j1_identifiant != null && j1.isJoueurHumain())
			j1.setNom(p.j1_identifiant);
		else
			p.j1_identifiant = j1.getNom();

		if (p.j2_identifiant != null && j2.isJoueurHumain())
			j2.setNom(p.j2_identifiant);
		else
			p.j2_identifiant = j2.getNom();
		
		
		ech.vider();
		ech.ajouter("parametres", p);
		com.envoyer(ech);
		if (joueurCourant.isJoueurHumain()) {
			gestionEvenementGraphique(joueurCourant.getNom(), "Selection du pion", joueurCourant.getJoueurID().getNum());
			ech.vider();
			ech.ajouter("aide", true);
			com.envoyer(ech,joueurCourant.getJoueurID().getNum());
			e = EtatTour.selectionPion;
		} else {
			gestionEvenementGraphique(joueurCourant.getNom(), "en train de jouer", joueurCourant.getJoueurID().getNum());
			e = EtatTour.jeuxIa;
			jouerIa();
		}
	}

	public void actionAide() {
		iaAide = new IntelligenceArtificielle(IntelligenceArtificielle.difficulteIA.normal, joueurCourant, Joueur.recupereJoueurOpposant(joueurCourant, j1, j2, false), t);
		Thread th = new Thread() {
			public void run() {
				Coup coupAide = new Coup();
				if (tourEnCours) {
					ArrayList<TourDeJeu> tour = new ArrayList<TourDeJeu>();
					Iterator<TourDeJeu> it;
					iaAide.getListeToursPourCoupDepart(new ArrayList<Point>(), tour, new TourDeJeu(), h.getDernierCoup(), t.copie(), (ArrayList<Point>) h.histoTour.clone(), 0, joueurCourant);
					it = tour.iterator();
					int valeurMax = 0;
					int posMax = 0;
					int pos = 0;

					while (it.hasNext()) {
						TourDeJeu tdj = it.next();
						if (tdj.getValeurResultat() > valeurMax) {
							posMax = pos;
							valeurMax = tdj.getValeurResultat();
						}
						pos++;
					}
					tour.get(posMax).getListeCoups().remove(h.getDernierCoup());
					coupAide = tour.get(posMax).getListeCoups().get(0);
					tour.get(posMax).getListeCoups().remove(0);
				} else
					coupAide = iaAide.jouerIA();

				Point tempDebut = coupAide.getpDepart();
				Point tempArrive = coupAide.getpArrivee();
				selectionPion(tempDebut);
				selectionDestination(tempArrive);
				if (coupAide.getChoixPrise() == Terrain.ChoixPrise.parAspiration) {
					Direction dir = t.recupereDirection(tempDebut, tempArrive);
					Point offset = t.offsetAspiration(dir, tempArrive);
					Point temp = new Point(tempDebut.x + offset.x, tempArrive.y + offset.y);
					actionPoint((Object) temp);
				} else if (coupAide.getChoixPrise() == Terrain.ChoixPrise.parPercussion) {
					Direction dir = t.recupereDirection(tempDebut, tempArrive);
					Point offset = t.offsetPercussion(dir, tempArrive);
					Point temp = new Point(tempDebut.x + offset.x, tempArrive.y + offset.y);
					actionPoint((Object) temp);
				}
				traceTerrain();
			}
		};
		th.start();
	}

	public void envoiParametre(){
		Parametres p = new Parametres();
		p.j1_identifiant = j1.getNom();
		p.j2_identifiant = j2.getNom();
		if (j1.isJoueurHumain()){
			p.j1_type = NiveauJoueur.HUMAIN;
			}
		else {
			switch (j1.getIA().getNiveauDifficulte()){
			case facile :
				p.j1_type = NiveauJoueur.FACILE;
				break;
			case normal :
				p.j1_type = NiveauJoueur.MOYEN ;
				break;
			case difficile :
				p.j1_type = NiveauJoueur.DIFFICILE ;
				break;
				}
		}
		if (j2.isJoueurHumain()){
			p.j2_type = NiveauJoueur.HUMAIN;
			}
		else {
			switch (j2.getIA().getNiveauDifficulte()){
			case facile :
				p.j2_type = NiveauJoueur.FACILE;
				break;
			case normal :
				p.j2_type = NiveauJoueur.MOYEN ;
				break;
			case difficile :
				p.j2_type = NiveauJoueur.DIFFICILE ;
				break;
				}
		}
		ech.vider();
		ech.ajouter("parametres", p);
		com.envoyer(ech);
	}
	
	
	/**
	 * Réalise les différentes actions en fonctions des commandes envoyées par l'IHM.
	 * @param o Contient la commande ainsi qu'un objet qui sera traité dans les actions.
	 * @param j Identifiant de joueur pour le réseau.
	 */
	public void action(Object o, int j) {
		
		Echange echange = (Echange) o;

		Case.Etat joueurReception = null;
		if (j == 1)
			joueurReception = Etat.joueur1;
		else if (j == 2)
			joueurReception = Etat.joueur2;

		for (String dataType : echange.getAll()) {
			Object dataValue = echange.get(dataType);
			
			if (Communication.enReseau() && (joueurCourant.getJoueurID() != joueurReception)
					&& (dataType.equals("point") || dataType.equals("annuler") || dataType.equals("refaire") || dataType.equals("finTour")))
				return;
			switch (dataType) {
			case "nouvellePartie":
				init(dataValue);
				break;
			case "point":
				actionPoint(dataValue);
				break;
			case "terrain":
				gestionEvenementGraphique();
				break;
			case "annuler":
				actionAnnuler();
				break;
			case "refaire":
				actionRefaire();
				break;
			case "finTour":
				if (tourEnCours && e == EtatTour.selectionDestination)
					finTour();
				break;
			case "sauvegarder":
				actionSauvegarder(dataValue);
				break;
			case "charger":
				actionCharger(dataValue);
				break;
			case "parametres":
				actionParametre(dataValue);
				break;
			case "aide":
				if (e != EtatTour.jeuxIa)
					actionAide();
				break;
			}
		}
	}
}
