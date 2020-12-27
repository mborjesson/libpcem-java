package com.martinborjesson.pcem.callbacks;

public interface PCemVideoCallbacks {
	public void onVideoSize(int width, int height);
	public void onVideoDraw(int x1, int x2, int y1, int y2, int offsetX, int offsetY, byte[] buffer, int bufferWidth, int bufferHeight, int bufferBitsPerPixel);
}
