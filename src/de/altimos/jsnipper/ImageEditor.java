package de.altimos.jsnipper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class ImageEditor extends JPanel implements Runnable {

	private BufferedImage sourceImage;
	private BufferedImage drawImage;
	
	private boolean wantRepaint = false;
	private Point lastDrawPoint = null;
	
	private Color drawColor = Color.RED;
	private int drawSize    = 3;
	private boolean drawAntialiased = false; 
	
	public ImageEditor(BufferedImage image) {
		sourceImage = image;
		clear();
		
		Dimension d = new Dimension(image.getWidth(), image.getHeight());
		setPreferredSize(d);
		setMaximumSize(d);
		setMinimumSize(d);
		setSize(d);
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				lastDrawPoint = null;
			}
			@Override
			public void mousePressed(MouseEvent e) {
				drawPoint(drawImage.getGraphics(), e.getX(), e.getY());
				lastDrawPoint = new Point(e.getX(), e.getY());
				wantRepaint = true;
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				Graphics g = drawImage.getGraphics();
				
				if(lastDrawPoint != null) {
					drawPointAtLine(g, (int) lastDrawPoint.getX(), (int) lastDrawPoint.getY(), e.getX(), e.getY());
				} else {
					drawPoint(g, e.getX(), e.getY());
				}
				
				lastDrawPoint = new Point(e.getX(), e.getY());
				wantRepaint = true;
			}
		});
		
		Thread t = new Thread(this, "JSnip-Repainter");
		t.setDaemon(true);
		t.start();
	}
	
	public void clear() {
		drawImage   = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		wantRepaint = true;
	}
	
	public Color getDrawColor() {
		return drawColor;
	}
	
	public void setDrawColor(Color color) {
		drawColor = color;
	}
	
	public int getDrawSize() {
		return drawSize;
	}
	
	public void setDrawSize(int size) {
		drawSize = size;
	}
	
	public boolean isDrawAntialiased() {
		return drawAntialiased;
	}
	
	public void setDrawAntialiased(boolean antialiased) {
		drawAntialiased = antialiased;
	}
	
	private void drawPointAtLine(Graphics g, int sx, int sy, int ex, int ey) {
		int dx = ex - sx;
		int dy = ey - sy;
		
		for(float i = 0f; i < 1f; i += 0.05f) {
			drawPoint(g, (int)(sx + i*dx), (int)(sy + i*dy));
		}
	}
	
	private void drawPoint(Graphics g, int px, int py) {
		int size = drawSize;
		
		float[] c = drawColor.getRGBColorComponents(null);
		
		if(!drawAntialiased) {
			g.setColor(drawColor);
			g.fillOval(px - size/2, py - size/2, size, size);
		} else {
			for (int x = -size/2; x < size/2; x++) {
				for (int y = -size/2; y < size/2; y++) {
					float difx = (float)size * .5f - Math.abs((float)x);
					float dify = (float)size * .5f - Math.abs((float)y);
					float dif  = difx/size * dify/size;
					
					
					g.setColor(new Color(c[0], c[1], c[2], dif));
					g.drawOval(px + x, py + y, 1, 1);
				}
			}
		}
	}
	
	public BufferedImage getImage() {
		BufferedImage image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB);
		image.getGraphics().drawImage(sourceImage, 0, 0, null);
		image.getGraphics().drawImage(drawImage, 0, 0, null);
		return image;
	}
	
	public void paint(Graphics g) {
		g.drawImage(sourceImage, 0, 0, null);
		g.drawImage(drawImage, 0, 0, null);
	}
	
	public void run() {
		while(true) {
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
