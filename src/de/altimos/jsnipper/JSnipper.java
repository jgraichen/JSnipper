package de.altimos.jsnipper;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

public class JSnipper extends JFrame {

	public static final String TITLE = "JSnipper";
	
	public static void main(String[] args) throws AWTException {
	    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	    BufferedImage image = new ScreenSnipper(gd).snip();
	    
	    if(image != null) {
	    	new JSnipper(image);
	    } else {
	    	System.exit(0);
	    }
	}
	
	public static Dimension calculateDimension(Dimension actual, Dimension max) {
		Dimension newd = new Dimension(actual);
		
		double scaleWidth  = (double)newd.width / (double)newd.height;
		double scaleHeight = (double)newd.height / (double)newd.width;
		
		if(newd.width > newd.height) {
			if(newd.width > max.width) {
				newd.width  = max.width;
				newd.height = (int)(newd.width * scaleHeight); 
			}
			if(newd.height > max.height) {
				newd.height  = max.height;
				newd.width = (int)(newd.height * scaleWidth); 
			}
		} else {
			if(newd.height > max.height) {
				newd.height  = max.height;
				newd.width = (int)(newd.height * scaleWidth); 
			}
			if(newd.width > max.width) {
				newd.width  = max.width;
				newd.height = (int)(newd.width * scaleHeight); 
			}
		}
		return newd;
	}
	
	private BufferedImage image;
	private ImageEditor editor;
	private JSnipper self;
	
	public JSnipper(BufferedImage image) {
		super(TITLE);
    	this.image = image;
    	this.self  = this;
    	
		setLayout(new BorderLayout());
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setResizable(false);
		
		int padding     = 40;

		Dimension actual   = new Dimension(image.getWidth(), image.getHeight());
		Dimension max      = new Dimension((int)((d.getWidth() - padding*2) * 0.8), (int)((d.getHeight() - padding*2) * 0.8));
		Dimension imageDim = calculateDimension(actual, max);
		
    	
    	BufferedImage editImage = new BufferedImage(imageDim.width + padding*2, imageDim.height + padding*2, BufferedImage.TYPE_INT_RGB);
    	Graphics g = editImage.getGraphics();
    	
    	g.setColor(Color.WHITE);
    	g.fillRect(0, 0, editImage.getWidth(), editImage.getHeight());
    	g.drawImage(image.getScaledInstance(imageDim.width, imageDim.height, BufferedImage.SCALE_SMOOTH), padding, padding, null);
		
		
		editor = new ImageEditor(editImage);
		
		add(editor, BorderLayout.CENTER);
		createToolbar();
		pack();
		
		setBounds((int)(d.getWidth() - getWidth())/2, (int)(d.getHeight() - getHeight())/2, getWidth(), getHeight());
		setVisible(true);
	}
	
	private void createToolbar() {
		JToolBar tb = new JToolBar();
		tb.setFloatable(false);

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser(FileSystemView.getFileSystemView());
				chooser.addChoosableFileFilter(new FileFilter() {
					
					@Override
					public String getDescription() {
						return "PNG";
					}
					
					@Override
					public boolean accept(File file) {
						return file.getPath().endsWith(".png") ||
								file.isDirectory();
					}
				});
				
				int answer = chooser.showSaveDialog(null);
				if(answer == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					try {
						ImageIO.write(editor.getImage(), "png", file);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(null, e.getMessage());
					}
					
					System.exit(0);
				}
			}
		});
		tb.add(saveButton);
		

		JButton copyButton = new JButton("Copy to Clipboard");
		copyButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				self.getToolkit().getSystemClipboard().setContents(new ImageTransferable(editor.getImage()), null);
			}
		});
		tb.add(copyButton);
		
		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				editor.clear();
			}
		});
		tb.add(clearButton);

		tb.addSeparator();
		createToolbarSizeButtons(tb);
		tb.addSeparator();
		createToolbarColorButtons(tb);
		tb.addSeparator();
		JToggleButton aaButton = new JToggleButton("AA", editor.isDrawAntialiased());
		aaButton.addActionListener(new AAButtonActionListener(aaButton));
		tb.add(aaButton);
		
		add(tb, BorderLayout.NORTH);
	}
	
	private void createToolbarColorButtons(JToolBar tb) {
		Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.BLACK, Color.WHITE};
		
		for (Color c : colors) {
			BufferedImage i = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
			Graphics g = i.getGraphics();
			g.setColor(c);
			g.fillRect(0, 0, i.getWidth(), i.getHeight());
			
			JButton b = new JButton(new ImageIcon(i));
			b.addActionListener(new ColorButtonActionListener(c));
			tb.add(b);
		}
	}
	
	private void createToolbarSizeButtons(JToolBar tb) {
		int[] sizes = {3, 5, 7, 9};
		
		for (int s : sizes) {
			BufferedImage i = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			Graphics g = i.getGraphics();
			g.setColor(Color.BLACK);
			g.fillOval((i.getWidth() - s) / 2, (i.getHeight() - s) / 2, s, s);
			
			JButton b = new JButton(new ImageIcon(i));
			b.addActionListener(new SizeButtonActionListener(s));
			tb.add(b);
		}
	}
	
	private class ColorButtonActionListener implements ActionListener {

		private Color color;
		
		public ColorButtonActionListener(Color color) {
			this.color = color;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			editor.setDrawColor(color);
		}
	}
	
	private class SizeButtonActionListener implements ActionListener {

		private int size;
		
		public SizeButtonActionListener(int size) {
			this.size = size;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			editor.setDrawSize(size);
		}
	}
	
	private class AAButtonActionListener implements ActionListener {

		private JToggleButton button;
		
		public AAButtonActionListener(JToggleButton button) {
			this.button = button;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			editor.setDrawAntialiased(button.isSelected());
		}
	}
}
