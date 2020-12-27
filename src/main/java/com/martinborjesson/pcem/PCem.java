package com.martinborjesson.pcem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.bridj.BridJ;
import org.bridj.Callback;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.ann.Name;

import com.martinborjesson.pcem.callbacks.PCemAudioCallbacks;
import com.martinborjesson.pcem.callbacks.PCemCallbacks;
import com.martinborjesson.pcem.callbacks.PCemVideoCallbacks;
import com.martinborjesson.pcem.enums.PCemConfigType;
import com.martinborjesson.pcem.enums.PCemDrive;

@Library("libpcem")
public class PCem {
	static {
		BridJ.addLibraryPath(Paths.get(".").toAbsolutePath().toString());
		BridJ.register();
	}
	
	private static PCem instance = new PCem();
	
	public static PCem getInstance() {
		return instance;
	}
	
	abstract protected class VideoSizeCallback extends Callback {
		abstract void apply(int width, int height);
	}

	private VideoSizeCallback videoSizeCallback = new VideoSizeCallback() {
		
		@Override
		void apply(int width, int height) {
			videoCallbacks.onVideoSize(width, height);
		}
	};

	abstract protected class VideoBlitDrawCallback extends Callback {
		abstract void apply(int x1, int x2, int y1, int y2, int offsetX, int offsetY, Pointer<Byte> buffer, int bufferWidth, int bufferHeight, int bufferBitsPerPixel);
	}

	private VideoBlitDrawCallback videoBlitDrawCallback = new VideoBlitDrawCallback() {
		
		@Override
		void apply(int x1, int x2, int y1, int y2, int offsetX, int offsetY, Pointer<Byte> buffer, int bufferWidth, int bufferHeight, int bufferBitsPerPixel) {
			videoCallbacks.onVideoDraw(x1, x2, y1, y2, offsetX, offsetY, buffer.getBytes(bufferWidth*bufferHeight*bufferBitsPerPixel), bufferWidth, bufferHeight, bufferBitsPerPixel);
		}
	};
	
	abstract protected class KeyboardPollCallback extends Callback {
		abstract void apply(Pointer<Byte> state);
	}
	
	private KeyboardPollCallback keyboardPollCallback = new KeyboardPollCallback() {
		final private byte[] buffer = new byte[272];
		
		@Override
		void apply(Pointer<Byte> state) {
			callbacks.onKeyboardPoll(buffer);
			state.setBytes(buffer);
		}
		
	};
	
	abstract protected class MousePollCallback extends Callback {
		abstract void apply(Pointer<Integer> x, Pointer<Integer> y, Pointer<Integer> z, Pointer<Integer> buttons);
	}
	
	private MousePollCallback mousePollCallback = new MousePollCallback() {
		final private PCemMouse mouse = new PCemMouse();
		
		@Override
		void apply(Pointer<Integer> x, Pointer<Integer> y, Pointer<Integer> z, Pointer<Integer> buttons) {
			mouse.x = mouse.y = mouse.z = mouse.buttons = 0;
			callbacks.onMousePoll(mouse);
			x.setInt(mouse.x);
			y.setInt(mouse.y);
			z.setInt(mouse.z);
			buttons.setInt(mouse.buttons);
		}
		
	};
	
	abstract protected class AudioStreamCreateCallback extends Callback {
		abstract void apply(int stream, int sampleRate, int sampleSizeInBits, int channels, int bufferLength);
	}
	
	private AudioStreamCreateCallback audioStreamCreateCallback = new AudioStreamCreateCallback() {
		
		@Override
		void apply(int stream, int sampleRate, int sampleSizeInBits, int channels, int bufferLength) {
			audioCallbacks.onAudioStreamCreate(stream, sampleRate, sampleSizeInBits, channels, bufferLength);
		}
		
	};

	abstract protected class AudioStreamDataCallback extends Callback {
		abstract void apply(int stream, Pointer<Byte> buffer, int bufferLength);
	}
	
	private AudioStreamDataCallback audioStreamDataCallback = new AudioStreamDataCallback() {
		
		@Override
		void apply(int stream, Pointer<Byte> buffer, int bufferLength) {
			audioCallbacks.onAudioStreamData(stream, buffer.getBytes(bufferLength));
		}
		
	};
	
	abstract protected class OnEventCallback extends Callback {
		abstract void apply(int event);
	}
	
	private OnEventCallback onEventCallback = new OnEventCallback() {
		
		@Override
		void apply(int event) {
			callbacks.onEvent(event);
		}
		
	};

	abstract protected class ConfigGetCallback extends Callback {
		abstract int apply(int type, int is_global, Pointer<Byte> section, Pointer<Byte> name, Pointer<?> value);
	}
	
	private ConfigGetCallback configGetCallback = new ConfigGetCallback() {

		int apply(int type, int is_global, Pointer<Byte> section, Pointer<Byte> name, Pointer<?> value) {
			boolean global = is_global != 0;
			String s = section != null ? section.getCString() : null;
			String n = name.getCString();
			if (type == 1) { // Int
				Integer v = (Integer) callbacks.onConfigGet(PCemConfigType.INT, global, s, n);
				if (v != null) {
					value.setInt(v);
					return 1;
				}
			} else if (type == 2) { // Float
				Float v = (Float) callbacks.onConfigGet(PCemConfigType.FLOAT, global, s, n);
				if (v != null) {
					value.setFloat(v);
					return 1;
				}
			} else if (type == 3) { // String
				String v = (String) callbacks.onConfigGet(PCemConfigType.STRING, global, s, n);
				if (v != null) {
					value.setCString(v);
					return 1;
				}
			}
			
			return 0;
		}
		
	};
	
	abstract protected class ConfigSetCallback extends Callback {
		abstract int apply(int type, int is_global, Pointer<Byte> section, Pointer<Byte> name, Pointer<?> value);
	}
	
	private ConfigSetCallback configSetCallback = new ConfigSetCallback() {

		int apply(int type, int is_global, Pointer<Byte> section, Pointer<Byte> name, Pointer<?> value) {
			boolean global = is_global != 0;
			String s = section != null ? section.getCString() : null;
			String n = name.getCString();
			if (type == 1) { // Int
				callbacks.onConfigSet(PCemConfigType.INT, global, s, n, value.getInt());
				return 1;
			} else if (type == 2) { // Float
				callbacks.onConfigSet(PCemConfigType.FLOAT, global, s, n, value.getFloat());
				return 1;
			} else if (type == 3) { // String
				callbacks.onConfigSet(PCemConfigType.STRING, global, s, n, value.getCString());
				return 1;
			}
			
			return 0;
		}
		
	};
	
	abstract protected class ConfigSaveCallback extends Callback {
		abstract void apply(int is_global);
	}
	
	private ConfigSaveCallback configSaveCallback = new ConfigSaveCallback() {

		void apply(int is_global) {
			callbacks.onConfigSave(is_global != 0);
		}
		
	};
	
	private PCemCallbacks callbacks;
	private PCemVideoCallbacks videoCallbacks;
	private PCemAudioCallbacks audioCallbacks;
	
	public PCem() {
		pcem_callback_video_size(Pointer.getPointer(videoSizeCallback));
		pcem_callback_video_blit_draw(Pointer.getPointer(videoBlitDrawCallback));
		pcem_callback_input_keyboard_poll(Pointer.getPointer(keyboardPollCallback));
		pcem_callback_input_mouse_poll(Pointer.getPointer(mousePollCallback));
		pcem_callback_audio_stream_create(Pointer.getPointer(audioStreamCreateCallback));
		pcem_callback_audio_stream_data(Pointer.getPointer(audioStreamDataCallback));
		pcem_callback_on_event(Pointer.getPointer(onEventCallback));
		pcem_callback_config_get(Pointer.getPointer(configGetCallback));
		pcem_callback_config_set(Pointer.getPointer(configSetCallback));
		pcem_callback_config_save(Pointer.getPointer(configSaveCallback));
	}
	
	static private String toPCemPath(Path path) {
		path = path.toAbsolutePath();
		String s = null;
		if (Files.isRegularFile(path)) {
			s = path.getParent().toString();
		} else {
			s = path.toString();
		}
		if (!s.endsWith("/")) {
			s += "/";
		}
		
		return s;
	}
	
	public void setVideoCallbacks(PCemVideoCallbacks videoCallbacks) {
		this.videoCallbacks = videoCallbacks;
	}
	
	public void setAudioCallbacks(PCemAudioCallbacks audioCallbacks) {
		this.audioCallbacks = audioCallbacks;
	}
	
	public void setCallbacks(PCemCallbacks callbacks) {
		this.callbacks = callbacks;
	}
	
	private native int pcem_start();
	private native int pcem_reset();
	private native int pcem_stop();
	
	private native void pcem_callback_video_size(Pointer<?> pointer);
	private native void pcem_callback_video_blit_draw(Pointer<?> pointer);
	private native void pcem_callback_input_keyboard_poll(Pointer<?> pointer);
	private native void pcem_callback_input_mouse_poll(Pointer<?> pointer);
	private native void pcem_callback_audio_stream_create(Pointer<?> pointer);
	private native void pcem_callback_audio_stream_data(Pointer<?> pointer);

	private native void pcem_callback_on_event(Pointer<?> pointer);

	private native void pcem_callback_config_get(Pointer<?> pointer);
	private native void pcem_callback_config_set(Pointer<?> pointer);
	private native void pcem_callback_config_save(Pointer<?> pointer);

	private native void pcem_set_nvr_path(Pointer<Byte> pointer);
	private native void pcem_set_roms_paths(Pointer<Byte> pointer);
	private native void pcem_set_logs_path(Pointer<Byte> pointer);
	
	private native void pcem_drive_load_image(int drive, Pointer<Byte> pointer);
	private native void pcem_drive_eject(int drive);
	
	static private native Pointer<?> pcem_config_load(Pointer<Byte> fn);
	static private native void pcem_config_save(Pointer<?> handle, Pointer<Byte> fn);
	static private native void pcem_config_free(Pointer<?> handle);

	static private native void pcem_config_set(Pointer<?> handle, int type, Pointer<Byte> section, Pointer<Byte> key, Pointer<?> value);
	static private native int pcem_config_get(Pointer<?> handle, int type, Pointer<Byte> section, Pointer<Byte> key, Pointer<?> value);

	static private native int pcem_config_simple_init(Pointer<Byte> global_config, Pointer<Byte> machine_config);
	static private native void pcem_config_simple_close();

	public int start() {
		BridJ.protectFromGC(videoSizeCallback);
		BridJ.protectFromGC(videoBlitDrawCallback);
		BridJ.protectFromGC(keyboardPollCallback);
		BridJ.protectFromGC(mousePollCallback);
		BridJ.protectFromGC(audioStreamCreateCallback);
		BridJ.protectFromGC(audioStreamDataCallback);
		BridJ.protectFromGC(onEventCallback);
		BridJ.protectFromGC(configGetCallback);
		BridJ.protectFromGC(configSetCallback);
		BridJ.protectFromGC(configSaveCallback);
		
		return pcem_start();
	}

	public int stop() {
		int res = pcem_stop();
		
		BridJ.unprotectFromGC(videoSizeCallback);
		BridJ.unprotectFromGC(videoBlitDrawCallback);
		BridJ.unprotectFromGC(keyboardPollCallback);
		BridJ.unprotectFromGC(mousePollCallback);
		BridJ.unprotectFromGC(audioStreamCreateCallback);
		BridJ.unprotectFromGC(audioStreamDataCallback);
		BridJ.unprotectFromGC(onEventCallback);
		BridJ.unprotectFromGC(configGetCallback);
		BridJ.unprotectFromGC(configSetCallback);
		BridJ.unprotectFromGC(configSaveCallback);

		return res;
	}
	
	public int reset() {
		return pcem_reset();
	}
	
	@Name("pcem_get_emulation_speed")
	public native int getEmulationSpeed();
	
	@Name("pcem_get_emulation_state")
	public native int getEmulationState();
	
	@Name("pcem_pause")
	public native int pause();
	
	@Name("pcem_resume")
	public native int resume();

	@Name("pcem_send_action")
	public native void sendAction(int action);

	public void setNVRPath(Path path) {
		pcem_set_nvr_path(Pointer.pointerToCString(toPCemPath(path)));
	}
	
	public void setRomsPaths(Path ... paths) {
		String p = Arrays.stream(paths).map(PCem::toPCemPath).collect(Collectors.joining(File.pathSeparator));
		pcem_set_roms_paths(Pointer.pointerToCString(p));
	}
	
	public void setLogsPath(Path path) {
		pcem_set_logs_path(Pointer.pointerToCString(toPCemPath(path)));
	}
	
	public void driveEject(PCemDrive drive) {
		pcem_drive_eject(drive.getValue());
	}
	
	public void driveLoadImage(PCemDrive drive, Path path) {
		if (Files.isRegularFile(path)) {
			pcem_drive_load_image(drive.getValue(), Pointer.pointerToCString(path.toString()));
		}
	}

	/* PCem Native Config */
	
	static public long configLoad(Path path) {
		Pointer<?> ptr = pcem_config_load(Pointer.pointerToCString(path.toString()));
		return ptr.getCLong();
	}
	
	static public void configSave(long handle, Path path) {
		pcem_config_save(Pointer.pointerToCLong(handle), Pointer.pointerToCString(path.toString()));
	}
	
	static public void configFree(long handle) {
		pcem_config_free(Pointer.pointerToCLong(handle));
	}
	
	static public void configSetInt(long handle, String section, String key, Object value) {
		if (value instanceof Number) {
			pcem_config_set(Pointer.pointerToCLong(handle), PCemConfigType.INT.getValue(), Pointer.pointerToCString(section), Pointer.pointerToCString(key), Pointer.pointerToInt(((Number)value).intValue()));
		}
	}
	
	static public void configSetFloat(long handle, String section, String key, Object value) {
		if (value instanceof Number) {
			pcem_config_set(Pointer.pointerToCLong(handle), PCemConfigType.FLOAT.getValue(), Pointer.pointerToCString(section), Pointer.pointerToCString(key), Pointer.pointerToFloat(((Number)value).floatValue()));
		}
	}

	static public void configSetString(long handle, String section, String key, Object value) {
		if (value instanceof String) {
			pcem_config_set(Pointer.pointerToCLong(handle), PCemConfigType.STRING.getValue(), Pointer.pointerToCString(section), Pointer.pointerToCString(key), Pointer.pointerToCString((String)value));
		}
	}
	
	static public Integer configGetInt(long handle, String section, String key) {
		Pointer<Integer> ptr = Pointer.allocateInt();
		if (pcem_config_get(Pointer.pointerToCLong(handle), PCemConfigType.INT.getValue(), Pointer.pointerToCString(section), Pointer.pointerToCString(key), ptr) != 0) {
			try {
				return ptr.get();
			} finally {
				Pointer.release(ptr);
			}
		}
		return null;
	}

	static public Float configGetFloat(long handle, String section, String key) {
		Pointer<Float> ptr = Pointer.allocateFloat();
		if (pcem_config_get(Pointer.pointerToCLong(handle), PCemConfigType.FLOAT.getValue(), Pointer.pointerToCString(section), Pointer.pointerToCString(key), ptr) != 0) {
			try {
				return ptr.get();
			} finally {
				Pointer.release(ptr);
			}
		}
		return null;
	}

	static public String configGetString(long handle, String section, String key) {
		Pointer<Byte> ptr = Pointer.allocateBytes(256);
		if (pcem_config_get(Pointer.pointerToCLong(handle), PCemConfigType.STRING.getValue(), Pointer.pointerToCString(section), Pointer.pointerToCString(key), ptr) != 0) {
			try {
				return ptr.getCString();
			} finally {
				Pointer.release(ptr);
			}
		}
		return null;
	}

	static public int configSimpleInit(Path globalConfig, Path machineConfig) {
		return pcem_config_simple_init(Pointer.pointerToCString(globalConfig.toAbsolutePath().toString()), Pointer.pointerToCString(machineConfig.toAbsolutePath().toString()));
	}
	
	static public void configSimpleClose() {
		pcem_config_simple_close();
	}

}
