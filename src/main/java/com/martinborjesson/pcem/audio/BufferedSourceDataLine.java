package com.martinborjesson.pcem.audio;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * A buffered source data line. It will not start consuming written streams until the queue is full.<p/>
 * If the queue becomes empty it will wait until full again.
 */
public class BufferedSourceDataLine implements SourceDataLine {
	static public interface BufferConsumer {
		public void accept(SourceDataLine sourceDataLine, byte[] b, int off, int len);
	}
	
	final private SourceDataLine out;
	final private Deque<byte[]> queue = new ConcurrentLinkedDeque<>();
	final private int numBuffers;
	private boolean running = false;
	
	public BufferedSourceDataLine(SourceDataLine out, int numBuffers) {
		this.out = out;
		this.numBuffers = numBuffers;
	}

	@Override
	public void drain() {
		out.drain();
	}

	@Override
	public void flush() {
		out.flush();
	}

	@Override
	public void start() {
		out.start();
		running = true;
		new Thread(() -> {
			while (running) {
				// are we there yet?
				if (queue.size() >= numBuffers) {
					// keep writing while the queue isn't empty
					while (!queue.isEmpty()) {
						byte[] buf = queue.poll();
						out.write(buf, 0, buf.length);
						Thread.yield();
					}
				}
				Thread.yield();
			}
		}).start();
	}

	@Override
	public void stop() {
		running = false;
		out.stop();
	}

	@Override
	public boolean isRunning() {
		return out.isRunning();
	}

	@Override
	public boolean isActive() {
		return out.isActive();
	}

	@Override
	public AudioFormat getFormat() {
		return out.getFormat();
	}

	@Override
	public int getBufferSize() {
		return out.getBufferSize();
	}

	@Override
	public int available() {
		return out.available();
	}

	@Override
	public int getFramePosition() {
		return out.getFramePosition();
	}

	@Override
	public long getLongFramePosition() {
		return out.getLongFramePosition();
	}

	@Override
	public long getMicrosecondPosition() {
		return out.getMicrosecondPosition();
	}

	@Override
	public float getLevel() {
		return out.getLevel();
	}

	@Override
	public javax.sound.sampled.Line.Info getLineInfo() {
		return out.getLineInfo();
	}

	@Override
	public void open() throws LineUnavailableException {
		out.open();
	}

	@Override
	public void close() {
		out.close();
	}

	@Override
	public boolean isOpen() {
		return out.isOpen();
	}

	@Override
	public Control[] getControls() {
		return out.getControls();
	}

	@Override
	public boolean isControlSupported(Type control) {
		return out.isControlSupported(control);
	}

	@Override
	public Control getControl(Type control) {
		return out.getControl(control);
	}

	@Override
	public void addLineListener(LineListener listener) {
		out.addLineListener(listener);
	}

	@Override
	public void removeLineListener(LineListener listener) {
		out.removeLineListener(listener);
	}

	@Override
	public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
		out.open(format, bufferSize);
	}

	@Override
	public void open(AudioFormat format) throws LineUnavailableException {
		out.open(format);
	}
	
	@Override
	public int write(byte[] b, int off, int len) {
		// copy buffer and add it to the queue
		byte[] copy = new byte[len];
		System.arraycopy(b, off, copy, 0, len);
		queue.add(copy);
		
		return len;
	}
	
	
}
