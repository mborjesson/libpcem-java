package com.martinborjesson.pcem.callbacks;

import com.martinborjesson.pcem.PCemMouse;
import com.martinborjesson.pcem.enums.PCemConfigType;

public interface PCemCallbacks {
	public void onKeyboardPoll(byte[] state);
	public void onMousePoll(PCemMouse mouseState);
	
	public Object onConfigGet(PCemConfigType type, boolean global, String section, String name);
	public void onConfigSet(PCemConfigType type, boolean global, String section, String name, Object value);
	
	public void onConfigSave(boolean global);
	
	public void onEvent(int event);

}
