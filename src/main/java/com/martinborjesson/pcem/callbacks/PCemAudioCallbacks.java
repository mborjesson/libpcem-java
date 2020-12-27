package com.martinborjesson.pcem.callbacks;

public interface PCemAudioCallbacks {
	public void onAudioStreamCreate(int stream, int sampleRate, int sampleSizeInBits, int channels, int bufferLength);
	public void onAudioStreamData(int stream, byte[] buffer);

}
