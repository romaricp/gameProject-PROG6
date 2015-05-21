import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

//import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;


@SuppressWarnings("serial")
public class Bouton extends JButton implements MouseListener{
	static List<Bouton> boutons = new ArrayList<Bouton>();

	Image fondNormal, fondSurvol, fondClique;
	Image fond;
	Color couleur;
	
	String texte;
	
	Bouton(String texte){
		this.texte = texte;
		this.setBorderPainted(false);
		Bouton.boutons.add( this );
		this.addMouseListener(this);
		
	}
	
	
	
	 public void paintComponent(Graphics gr) {
		 
		System.out.println("paintComponent");
		Graphics2D g = (Graphics2D)gr;
		
		
		
		 
		Font font = new Font("Arial",Font.BOLD,20);
		g.setFont(font);		 
 	 	
		FontMetrics fm = g.getFontMetrics();		
 	     
		g.drawImage(fond,0,0,this.getWidth(), this.getHeight(),null);
		
		g.setColor(Color.WHITE);		
		
		g.drawString(texte,this.getWidth()/2-fm.stringWidth(texte)/2,this.getHeight()/2+fm.getHeight()/4);
		
 	   
	 }
	 
	 public void setTheme(Theme.Type pTheme){
		 System.out.println("Thème "+pTheme.getId()+" qz");
		 fondNormal = new ImageIcon("images/themes/"+pTheme.getId()+"/bouton_normal.png").getImage();
		 fondSurvol = new ImageIcon("images/themes/"+pTheme.getId()+"/bouton_survol.png").getImage();
		 fondClique = new ImageIcon("images/themes/"+pTheme.getId()+"/bouton_clique.png").getImage();
		 fond = fondNormal;
		 this.repaint();	
	 }
	 
	 public static void setThemeTous(Theme.Type pTheme){
		 
		 Iterator<Bouton> it = boutons.iterator();
		 while (it.hasNext()) {
			Bouton bt = (Bouton)it.next();	
			bt.setTheme( pTheme );
					
		 }	 
	
	 }

	@Override
	public void mouseClicked(MouseEvent arg0) {
		fond = fondClique;
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		System.out.println("mouseentered");
		fond = fondSurvol;		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {		
		fond = fondNormal;
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		fond = fondClique;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
		if(e.getY() > 0 && e.getY() < this.getHeight() && (e.getX() > 0 && e.getX() < this.getWidth())){
			fond = fondSurvol;			
		}
		else
			fond = fondNormal;
	}
}
