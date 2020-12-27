package com.martinborjesson.pcem;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import com.martinborjesson.pcem.callbacks.PCemCallbacks;
import com.martinborjesson.pcem.config.Config;
import com.martinborjesson.pcem.config.ConfigIO;
import com.martinborjesson.pcem.config.ConfigIOJSON;
import com.martinborjesson.pcem.config.ConfigIOPCemIni;
import com.martinborjesson.pcem.enums.PCemConfigType;
import com.martinborjesson.pcem.enums.PCemDrive;

public class PCemWindow implements PCemCallbacks, NativeKeyListener {
	final private Path rootConfig;
	private Path machinesPath;
	private boolean keepConfigs;
	
	final private PCem pcem = PCem.getInstance();
	
	final private JFrame frame = new JFrame("PCem Java (Initializing...)");
	final private PCemCanvas canvas = new PCemCanvas();
	final private PCemAudio audio = new PCemAudio();
	
	private ScheduledExecutorService executor = null;
	
	private byte[] keyboardStates = new byte[272];
	
	private int mouseButtons = 0;
	private int[] mouseMovement = { 0, 0 };
	
	private int grabMouse = 0;
	final private Robot robot;
	
	final private Cursor blankCursor;
	
	private ConfigIO globalConfigIO;
	private ConfigIO machineConfigIO;
	private Config globalConfig;
	private Config machineConfig;
	
	public PCemWindow(Path rootConfig) throws Exception {
		this.rootConfig = rootConfig;
		this.robot = new Robot();
		
		pcem.setCallbacks(this);
		pcem.setVideoCallbacks(canvas);
		pcem.setAudioCallbacks(audio);
		
		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "Blank cursor");
		
		frame.setJMenuBar(createMenu());
		
		frame.add(canvas);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setFocusTraversalKeysEnabled(false);
		
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosed(WindowEvent e) {
				exit();
			}
		});
		
		frame.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				GlobalScreen.addNativeKeyListener(PCemWindow.this);
			}
			
			@Override
			public void windowLostFocus(WindowEvent e) {
				GlobalScreen.removeNativeKeyListener(PCemWindow.this);
				grabMouse(false);
			}
		});
		
		frame.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!isMouseGrabbed()) {
					grabMouse(true);
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (isMouseGrabbed()) {
					int button = e.getButton();
					if (button == MouseEvent.BUTTON1) {
						mouseButtons |= 1;
					} else if (button == MouseEvent.BUTTON2) {
						mouseButtons |= 2;
					} else if (button == MouseEvent.BUTTON3) {
						mouseButtons |= 4;
					}
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (isMouseGrabbed()) {
					int button = e.getButton();
					if (button == MouseEvent.BUTTON1) {
						mouseButtons ^= 1;
					} else if (button == MouseEvent.BUTTON2) {
						mouseButtons ^= 2;
					} else if (button == MouseEvent.BUTTON3) {
						mouseButtons ^= 4;
					}
				}
			}
		});
		
		frame.addMouseMotionListener(new MouseAdapter() {
			
			@Override
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			
			@Override
			public void mouseMoved(MouseEvent e) {
				if (isMouseGrabbed()) {
					int centerX = frame.getX() + frame.getWidth() / 2;
					int centerY = frame.getY() + frame.getHeight() / 2;
					
					synchronized(mouseMovement) {
						mouseMovement[0] += e.getXOnScreen() - centerX;
						mouseMovement[1] += e.getYOnScreen() - centerY;
					}
					
					robot.mouseMove(centerX, centerY);
				}
			}
		});
		
		// clear all inputs
		InputMap im = frame.getJMenuBar().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); 
		while (im != null) {
			im.clear();
			im = im.getParent();
		}
		
		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException e1) {
			e1.printStackTrace();
		}
	}
	
	public void setMachinesPath(Path machinesPath) {
		this.machinesPath = machinesPath;
	}
	
	public void setKeepConfigs(boolean keepConfigs) {
		this.keepConfigs = keepConfigs;
	}
	
	public void showWindow() {
		frame.setVisible(true);
	}
	
	static private Path convertConfig(Path inputPath) throws IOException {
		if (inputPath.getFileName().toString().toLowerCase().endsWith(".cfg")) {
			ConfigIO ioPCem = new ConfigIOPCemIni(inputPath);
			
			String baseName = FilenameUtils.getBaseName(inputPath.getFileName().toString());
			
			Path outputPath = inputPath.getParent().resolve(baseName + ".json");
			
			System.out.println("Copying " + inputPath + " to " + outputPath + "...");
			
			ConfigIO ioJSON = new ConfigIOJSON(outputPath);
			
			Config inputConfig = ioPCem.load();
			Config outputConfig = ioJSON.create();
		
			inputConfig.copyTo(outputConfig);
			
			ioJSON.save(outputConfig);
			
			ioPCem.close(inputConfig);
			ioJSON.close(outputConfig);
			
			return outputPath;
		}
		
		return inputPath;
	}
	
	public void start(String configuration) throws IOException {
		Path pcemConfig = rootConfig;
		if (Files.isDirectory(pcemConfig)) {
			if (Files.exists(pcemConfig.resolve("pcem.json"))) {
				pcemConfig = pcemConfig.resolve("pcem.json");
			} else {
				pcemConfig = pcemConfig.resolve("pcem.cfg");
			}
		}
		if (!Files.exists(pcemConfig)) {
			System.err.println("PCem global configuration " + pcemConfig + " does not exist.");
			System.exit(2);
		}
		
		if (!keepConfigs) {
			pcemConfig = convertConfig(pcemConfig);
		}
		
		globalConfigIO = ConfigIO.createFromPath(pcemConfig);
		if ((globalConfig = globalConfigIO.load()) == null) {
			System.err.println("Could not load PCem global configuration " + pcemConfig + ".");
			System.exit(3);
		}
		System.out.println("PCem global configuration loaded from " + pcemConfig);
		
		Path machineConfig = machinesPath;
		if (machineConfig == null) {
			machineConfig = Optional.ofNullable(globalConfig.getString("Paths", "configs_path")).map(Paths::get).filter(Files::exists).orElse(null);
		}
		if (machineConfig == null) {
			machineConfig = pcemConfig.getParent().resolve("configs");
			if (!Files.exists(machineConfig) || !Files.isDirectory(machineConfig)) {
				System.out.println("Could not automatically find machine configurations path, please specity it with --machines-path");
				System.exit(6);
			}
		}
		if (Files.isDirectory(machineConfig)) {
			if (Files.exists(machineConfig.resolve(configuration + ".json"))) {
				machineConfig = machineConfig.resolve(configuration + ".json");
			} else {
				machineConfig = machineConfig.resolve(configuration + ".cfg");
			}
		}
		if (!Files.exists(machineConfig)) {
			System.err.println("PCem machine configuration " + machineConfig + " does not exist.");
			System.exit(4);
		}
		
		if (!keepConfigs) {
			machineConfig = convertConfig(machineConfig);
		}

		machineConfigIO = ConfigIO.createFromPath(machineConfig);
		
		if ((this.machineConfig = machineConfigIO.load()) == null) {
			System.err.println("Could not load PCem machine configuration " + machineConfig + ".");
			System.exit(5);
		}
		System.out.println("PCem machine configuration loaded from " + machineConfig);
		
		int result = pcem.start();
		if (result != 0) {
			System.err.println("Start error: " + result);
			System.exit(1);
		}
		
		executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(() -> {
			int emulationSpeed = pcem != null ? pcem.getEmulationSpeed() : 0;
			int state = pcem.getEmulationState();
			String stateStr = "Unknown";
			if (state == 1) {
				stateStr = "Stopped";
			} else if (state == 2) {
				stateStr = "Running";
			} else if (state == 3) {
				stateStr = "Paused";
			}
			frame.setTitle("PCem Java (" + canvas.getFPS() + " FPS - " + emulationSpeed + "%) " + stateStr);
		}, 1, 1, TimeUnit.SECONDS);
	}
	
	public void stop() {
		System.out.println("Shutting down PCem...");
		if (pcem != null) {
			pcem.stop();
		}
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
		
		if (audio != null) {
			audio.close();
		}
		
		System.out.println("Shut down successfully.");
	}
	
	public void exit() {
		stop();
		audio.dispose();
		frame.dispose();
		
		try {
			GlobalScreen.unregisterNativeHook();
		} catch (NativeHookException e) {
			e.printStackTrace();
		}
	}

	private Path getFile(Path path, String ... filters) {
		Path p = path.toAbsolutePath();
		if (!Files.isDirectory(p)) {
			p = p.getParent();
		}
		JFileChooser fc = new JFileChooser(p.toFile());
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		for (int i = 0; i < filters.length; ++i) {
			String[] split = filters[i].split("\\|");
			String desc = split[0];
			String[] exts = split[1].split(":");
			
			FileFilter ff = new FileFilter() {
				
				@Override
				public String getDescription() {
					return desc;
				}
				
				@Override
				public boolean accept(File f) {
					for (String e : exts) {
						if (f.isDirectory() || f.getName().toLowerCase().endsWith(e.toLowerCase())) {
							return true;
						}
					}
					return false;
				}
			};
			
			fc.addChoosableFileFilter(ff);
			if (i == 0) {
				fc.setFileFilter(ff);
			}
		}
		
		int res = fc.showOpenDialog(frame);
		if (res == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile().toPath();
		}
		
		return null;
	}
	
	private JMenuItem createMenuItem(String label, Consumer<ActionEvent> consumer) {
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(e -> consumer.accept(e));
		return item;
	}
	
	private JMenuItem createCheckBoxMenuItem(String label, boolean selected, Consumer<ActionEvent> consumer) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, selected);
		item.addActionListener(e -> consumer.accept(e));
		return item;
	}
	
	private JMenu createMenu(String label, Consumer<JMenu> consumer) {
		JMenu menu = new JMenu(label);
		consumer.accept(menu);
		return menu;
	}
	
	private JMenuBar createMenu() {
		JMenuBar mb = new JMenuBar();
		
		mb.add(createMenu("Machine", m -> {
			m.add(createMenuItem("Reset", e -> {
				pcem.reset();
			}));
			m.add(createMenuItem("Send CTRL-ALT-DEL", e -> {
				pcem.sendAction(2);
			}));
			m.add(createMenuItem("Pause / Resume", e -> {
				int state = pcem.getEmulationState();
				if (state == 2) { // Running
					pcem.pause();
				} else if (state == 3) { // Paused
					pcem.resume();
				}
			}));
			m.add(createMenuItem("Shutdown", e -> {
				exit();
			}));
		}));
		
		mb.add(createMenu("Drives", menu -> {
			menu.add(createMenu("Floppy A", m -> {
				m.add(createMenuItem("Load", e -> {
					Path path = getFile(Paths.get(machineConfig.getString(null, "disc_a")), "Disc image (*.img;*.ima;*.fdi)|img:ima:fdi");
					if (path != null && pcem != null) {
						pcem.driveLoadImage(PCemDrive.FLOPPY_A, path);
					}
				}));
				m.add(createMenuItem("Eject", e -> {
					if (pcem != null) pcem.driveEject(PCemDrive.FLOPPY_A);
				}));
			}));
			menu.add(createMenu("Floppy B", m -> {
				m.add(createMenuItem("Load", e -> {
					Path path = getFile(Paths.get(machineConfig.getString(null, "disc_b")), "Disc image (*.img;*.ima;*.fdi)|img:ima:fdi");
					if (path != null && pcem != null) {
						pcem.driveLoadImage(PCemDrive.FLOPPY_B, path);
					}
				}));
				m.add(createMenuItem("Eject", e -> {
					if (pcem != null) pcem.driveEject(PCemDrive.FLOPPY_B);
				}));
			}));
			menu.add(createMenu("CD-ROM", m -> {
				m.add(createMenuItem("Load", e -> {
					Path path = getFile(Paths.get(machineConfig.getString(null, "cdrom_path")), "CD-ROM image (*.iso;*.cue)|iso:cue");
					if (path != null && pcem != null) {
						pcem.driveLoadImage(PCemDrive.CD, path);
					}
				}));
				m.add(createMenuItem("Eject", e -> {
					if (pcem != null) pcem.driveEject(PCemDrive.CD);
				}));
			}));
		}));
		
		mb.add(createMenu("Input", menu -> {
			menu.add(createMenuItem("Grab mouse", e -> {
				grabMouse(true);
			}));
		}));
		
		mb.add(createMenu("Output", menu -> {
			menu.add(createCheckBoxMenuItem("Video", true, e -> {
				canvas.setRunning(((JCheckBoxMenuItem)e.getSource()).isSelected());
			}));
		}));
		
		return mb;
	}
	
	private void grabMouse(boolean grab) {
		if (grab) {
			frame.getContentPane().setCursor(blankCursor);
			grabMouse = 1;
		} else {
			frame.getContentPane().setCursor(Cursor.getDefaultCursor());
			grabMouse = 0;
		}
	}
	
	private boolean isMouseGrabbed() {
		return grabMouse > 0;
	}

	/**
	 * All of these are according to my own keyboard running Linux so might not work for everyone
	 * @param keyEvent
	 * @return
	 */
	static public int convertKeyCode(NativeKeyEvent keyEvent) {
		int keyCode = keyEvent.getKeyCode();
		int keyLoc = keyEvent.getKeyLocation();
		
		if (keyLoc == NativeKeyEvent.KEY_LOCATION_NUMPAD) {
			switch (keyCode) {
			case NativeKeyEvent.VC_ENTER:
				return 0x9c;
			case NativeKeyEvent.VC_SLASH:
				return 0xb5;
			case NativeKeyEvent.VC_INSERT:
				return 0xd2;
			case NativeKeyEvent.VC_0:
				return 0x52;
			case NativeKeyEvent.VC_1:
				return 0x4f;
			case NativeKeyEvent.VC_2:
				return 0x50;
			case NativeKeyEvent.VC_3:
				return 0x51;
			case NativeKeyEvent.VC_4:
				return 0x4b;
			case NativeKeyEvent.VC_5:
				return 0x4c;
			case NativeKeyEvent.VC_6:
				return 0x4d;
			case NativeKeyEvent.VC_7:
				return 0x47;
			case NativeKeyEvent.VC_8:
				return 0x48;
			case NativeKeyEvent.VC_9:
				return 0x49;
			case NativeKeyEvent.VC_PRINTSCREEN: // Multiply key
				return 0x37;
			}
		}
		
		switch (keyCode) {
		case NativeKeyEvent.VC_SHIFT: {
			if (keyLoc == NativeKeyEvent.KEY_LOCATION_LEFT) {
				return 0x2a;
			} else if (keyLoc == NativeKeyEvent.KEY_LOCATION_RIGHT) {
				return 0x36;
			}
			return -1;
		}
		case NativeKeyEvent.VC_CONTROL: {
			if (keyLoc == NativeKeyEvent.KEY_LOCATION_LEFT) {
				return 0x1d;
			} else if (keyLoc == NativeKeyEvent.KEY_LOCATION_RIGHT) {
				return 0x9d;
			}
			return -1;
		}
		case NativeKeyEvent.VC_ALT: {
			if (keyLoc == NativeKeyEvent.KEY_LOCATION_LEFT) {
				return 0x38;
			} else if (keyLoc == NativeKeyEvent.KEY_LOCATION_RIGHT) {
				return 0xb8;
			}
			return -1;
		}
		case NativeKeyEvent.VC_INSERT:
			return 0xd2;
		case NativeKeyEvent.VC_HOME:
			return 0xc7;
		case NativeKeyEvent.VC_PAGE_UP:
			return 0xc9;
		case NativeKeyEvent.VC_DELETE:
			return 0xd3;
		case NativeKeyEvent.VC_END:
			return 0xcf;
		case NativeKeyEvent.VC_PAGE_DOWN:
			return 0xd1;
		case NativeKeyEvent.VC_UP:
			return 0xc8;
		case NativeKeyEvent.VC_LEFT:
			return 0xcb;
		case NativeKeyEvent.VC_DOWN:
			return 0xd0;
		case NativeKeyEvent.VC_RIGHT:
			return 0xcd;
		case NativeKeyEvent.VC_PAUSE:
			return 0xc5;
		case NativeKeyEvent.VC_PRINTSCREEN:
			return 0xb7;
		default: {
			if (keyCode > 0xe00 && keyCode < 0xf00) {
				return keyCode-0xe00;
			}
			return keyCode;
		}
		}

	}
	
	@Override
	public void nativeKeyTyped(NativeKeyEvent nativeEvent) {}
	
	@Override
	public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
		int keyCode = convertKeyCode(nativeEvent);
		if (keyCode >= 0 && pcem != null && keyCode < keyboardStates.length) {
			keyboardStates[keyCode] = 0;
		}
	}
	
	@Override
	public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
		if (nativeEvent.getKeyCode() == NativeKeyEvent.VC_END && (nativeEvent.getModifiers()&NativeKeyEvent.CTRL_MASK) != 0) {
			grabMouse(false);
			return;
		}
		
		int keyCode = convertKeyCode(nativeEvent);
		if (keyCode >= 0 && keyCode < keyboardStates.length) {
			if (pcem != null) {
				keyboardStates[keyCode] = 1;
			}
		} else {
			System.out.println("Unhandled key code: " + nativeEvent.getKeyCode());
		}
//		System.out.println("Key Code: " + nativeEvent.getKeyCode() + " (" + Integer.toHexString(nativeEvent.getKeyCode()) + ")");
//		System.out.println("Key Location: " + nativeEvent.getKeyLocation());
//		System.out.println("PCem Key Code: " + keyCode + " (" + Integer.toHexString(keyCode) + ")");
	}

	@Override
	public void onKeyboardPoll(byte[] state) {
		System.arraycopy(keyboardStates, 0, state, 0, state.length);
	}

	@Override
	public void onMousePoll(PCemMouse mouseState) {
		if (isMouseGrabbed()) {
			synchronized(mouseMovement) {
				mouseState.x = mouseMovement[0];
				mouseState.y = mouseMovement[1];
				
				mouseMovement[0] = mouseMovement[1] = 0;
			}
			
			mouseState.z = 0;
			mouseState.buttons = mouseButtons;
		}
	}

	@Override
	public Object onConfigGet(PCemConfigType type, boolean global, String section, String name) {
		Config cfg = global ? globalConfig : machineConfig;
		
		Object v = null;
		if (type == PCemConfigType.INT) {
			v = cfg.getInt(section, name);
		} else if (type == PCemConfigType.FLOAT) {
			v = cfg.getFloat(section, name);
		} else if (type == PCemConfigType.STRING) {
			v = cfg.getString(section, name);
		}
		return v;
	}
	
	@Override
	public void onConfigSet(PCemConfigType type, boolean global, String section, String name, Object value) {
		Config cfg = global ? globalConfig : machineConfig;
		if (type == PCemConfigType.INT) {
			cfg.setInt(section, name, (Integer) value); 
		} else if (type == PCemConfigType.FLOAT) {
			cfg.setFloat(section, name, (Float) value); 
		} else if (type == PCemConfigType.STRING) {
			cfg.setString(section, name, (String) value); 
		}
	}
	
	@Override
	public void onConfigSave(boolean global) {
		try {
			if (global) {
				globalConfigIO.save(globalConfig);
			} else {
				machineConfigIO.save(machineConfig);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEvent(int event) {
	}
}
