package com.martinborjesson.pcem;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import com.martinborjesson.pcem.audio.BufferedSourceDataLine;
import com.martinborjesson.pcem.callbacks.PCemAudioCallbacks;

public class PCemAudio implements PCemAudioCallbacks, Closeable {
	private Mixer mixer;
	private class StreamData {
		final private SourceDataLine sourceDataLine;
		
		public StreamData(SourceDataLine sourceDataLine) {
			this.sourceDataLine = sourceDataLine;
		}
		
		public SourceDataLine getSourceDataLine() {
			return sourceDataLine;
		}
	}
	
	final private Map<Integer, StreamData> dataLineMap = new HashMap<>();
	
	public PCemAudio() {
		this.mixer = getDefaultOutputMixer();
		System.out.println("Selected audio device: " + mixer.getMixerInfo().getName());
		try {
			mixer.open();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This will return the first output mixer we can find. Hopefully it's the default :)
	 * @return
	 */
	static private Mixer getDefaultOutputMixer() {
		for (Mixer.Info info : AudioSystem.getMixerInfo()) {
			Mixer mixer = AudioSystem.getMixer(info);
			if (mixer.isLineSupported(new Line.Info(SourceDataLine.class))) {
				return mixer;
			}
		}
		return null;
	}
	
	private SourceDataLine createSourceDataLine() throws LineUnavailableException {
		Line.Info[] lineInfos = mixer.getSourceLineInfo(new Line.Info(SourceDataLine.class));
		if (lineInfos.length > 0) {
			return (SourceDataLine) mixer.getLine(lineInfos[0]);
		}
		throw new LineUnavailableException();
	}
	
	@Override
	public void onAudioStreamCreate(int stream, int sampleRate, int sampleSizeInBits, int channels, int bufferLength) {
		System.out.println("Create audio stream " + stream + ": " + sampleRate + " hz, " + sampleSizeInBits + " bit, " + channels + " channels (" + bufferLength + ")");
		
		AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, true, false);
		
		try {
			SourceDataLine dataLine = new BufferedSourceDataLine(createSourceDataLine(), 4);
			dataLine.open(format, sampleRate * sampleSizeInBits * channels / 8 / 10);
			dataLineMap.put(stream, new StreamData(dataLine));
			dataLine.start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onAudioStreamData(int stream, byte[] buffer) {
		StreamData streamData = dataLineMap.get(stream);
		if (streamData != null) {
			streamData.sourceDataLine.write(buffer, 0, buffer.length);
		}
	}
	
	@Override
	public void close() {
		dataLineMap.values().stream().map(StreamData::getSourceDataLine).forEach(dataLine -> {
			dataLine.flush();
			dataLine.drain();
			dataLine.close();
		});
		dataLineMap.clear();
	}
	
	public void dispose() {
		close();
		mixer.close();
	}

}
