package com.martinborjesson.pcem;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JPanel;

import com.martinborjesson.pcem.callbacks.PCemVideoCallbacks;

public class PCemCanvas extends JPanel implements PCemVideoCallbacks {

	private Dimension displaySize = new Dimension(800, 600);
	private Dimension screenSize = new Dimension(640, 480);
	final private Object screenLock = new Object();
	final private BufferedImage pcemImage = new BufferedImage(2048, 2048, BufferedImage.TYPE_3BYTE_BGR);
	final private byte[] imageBuffer = new byte[2048*2048*3];
	private int frames = 0;
	private long frameTime = System.currentTimeMillis();
	
	private double fps = 0;
	private boolean running = true;

	public PCemCanvas() {
		setSize(640, 480);
		setDoubleBuffered(true);
		setLayout(new BorderLayout());
	}
	
	@Override
	public Dimension getPreferredSize() {
		return displaySize;
	}
	
	public double getFPS() {
		return fps;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		frames++;
		long now = System.currentTimeMillis();
		if (now-frameTime >= 1000) {
			fps = frames*(now-frameTime)/1000.0;
			frames = 0;
			frameTime = now;
		}
		synchronized(screenLock) {
			System.arraycopy(imageBuffer, 0, ((DataBufferByte)pcemImage.getRaster().getDataBuffer()).getData(), 0, imageBuffer.length);
		}
		if (g instanceof Graphics2D) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		}
		g.drawImage(pcemImage, 0, 0, displaySize.width, displaySize.height, 0, 0, screenSize.width, screenSize.height, null);
	}

	@Override
	public void onVideoSize(int width, int height) {
		System.out.println("Resize to " + width + "x" + height);
		synchronized(this) {
			screenSize.setSize(width, height);
		}
	}

	@Override
	public void onVideoDraw(int x1, int x2, int y1, int y2, int offsetX, int offsetY, byte[] buffer, int bufferWidth, int bufferHeight, int bufferBitsPerPixel) {
		if (running) {
			synchronized(screenLock) {
				for (int y = y1; y < y2; ++y) {
					int start = y * pcemImage.getWidth() * 3;
					if ((offsetY + y) >= 0 && (offsetY + y) < bufferHeight) {
						for (int x = x1; x < x2; ++x) {
							int p = bufferBitsPerPixel*(y*bufferWidth+offsetX+x);

							for (int i = 0; i < 3; ++i) {
								imageBuffer[start + 3*x + i] = buffer[p + i];
							}
						}
					}
				}
			}
			repaint();
		}
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	public boolean isRunning() {
		return running;
	}
}
