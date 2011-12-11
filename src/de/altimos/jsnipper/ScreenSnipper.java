package de.altimos.jsnipper;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class ScreenSnipper extends JFrame implements Runnable {
	
	public static final Color UNSELECTED_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.5f);
	
	private GraphicsDevice gd            = null;
	private BufferedImage selectionImage = null;
	private BufferedImage screenImage    = null;
	private BufferedImage resultImage    = null;
	private Point selectionStart         = null;
	private Point selectionEnd           = null;
	private boolean wantRepaint			 = false;

	public ScreenSnipper(GraphicsDevice gd) {
		super(gd.getDefaultConfiguration());
		this.gd = gd;
		
		setTitle(JSnipper.TITLE);
		setUndecorated(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if(event.getKeyCode() == KeyEvent.VK_ESCAPE) {
					setVisible(false);
				}
			}
		});
		
		addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseReleased(MouseEvent event) {
				int posx   = (int) selectionStart.getX();
				int posy   = (int) selectionStart.getY();
				int width  = (int) (event.getX() - posx);
				int height = (int) (event.getY() - posy);
				
				if(width < 0) {
					posx  = event.getX();
					width = -width;
				}
				if(height < 0) {
					posy   = event.getY();
					height = -height;
				}
				
				if(height == 0 && width == 0) {
					resultImage = screenImage;
				} else {
					resultImage = screenImage.getSubimage(posx, posy, width, height);
				}
				setVisible(false);
			}
			
			@Override
			public void mousePressed(MouseEvent event) {
				selectionStart = new Point(event.getX(), event.getY());
				selectionEnd   = new Point(event.getX(), event.getY());
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				setVisible(false);
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent event) {
				int ex = event.getX();
				int ey = event.getY();
				
				if(selectionStart != null && (selectionEnd.getX() != ex || selectionEnd.getY() != ey)) {
					selectionEnd.setLocation(ex, ey);
					wantRepaint = true;
				}
			}
		});
	}
	
	public BufferedImage snip() throws AWTException {
		
		Robot robot = new Robot();
		DisplayMode dm = gd.getDisplayMode();
		Rectangle captureSize = new Rectangle(0, 0, dm.getWidth(), dm.getHeight());
		screenImage   = robot.createScreenCapture(captureSize);
		selectionImage = new BufferedImage(screenImage.getWidth(), screenImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

		setBounds(0, 0, dm.getWidth(), dm.getHeight());
		setAlwaysOnTop(true);
		setVisible(true);
		
		Thread t = new Thread(this, "JSnip-Repainter");
		t.setDaemon(true);
		t.start();
		
		while(isVisible()) { 
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { }
		}
		
		return resultImage;
	}
	
	public void paint(Graphics gd) {
		if(screenImage != null && selectionImage != null) {
			gd.drawImage(screenImage, 0, 0, null);
			paintSelection(gd);
		} else {
			super.paint(gd);
		}
	}
	
	protected void paintSelection(Graphics gd) {
		if(selectionStart == null || selectionEnd == null) {
			return;
		}
		
		int sx = (int) selectionStart.getX();
		int sy = (int) selectionStart.getY();
		int ex = (int) selectionEnd.getX();
		int ey = (int) selectionEnd.getY();

		if(sx > ex) {
			sx = (int) selectionEnd.getX();
			ex = (int) selectionStart.getX();
		}
		if(sy > ey) {
			sy = (int) selectionEnd.getY();
			ey = (int) selectionStart.getY();
		}
		
		gd.setColor(Color.RED);
		gd.drawRect(sx, sy, ex - sx, ey - sy);
	}
	
	public void run() {
		while(isVisible()) {
			if(wantRepaint) {
				repaint();
				wantRepaint = false;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { }
		}
	}
}
