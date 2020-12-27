package com.martinborjesson.pcem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import org.jnativehook.GlobalScreen;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name="pcem-java", version="pcem-java 0.1", mixinStandardHelpOptions=true)
public class PCemMain implements Runnable {
	
	@Option(names = { "-c", "--config" }, description = "Path to the PCem global configuration file.", required=true)
    private Path config;

	@Option(names = { "--path-machines" }, description = "Path to the PCem machine configurations folder, if not specified it will use it from the global config.")
    private Path pathMachines;

	@Option(names = { "--path-roms" }, description = "Path to the PCem roms folder, if not specified it will use it from the global config.")
    private Path pathROMs;

	@Option(names = { "--path-nvr" }, description = "Path to the PCem nvr folder, if not specified it will use it from the global config.")
    private Path pathNVR;

	@Option(names = { "--path-logs" }, description = "Path to the PCem logs folder, if not specified it will use it from the global config.")
    private Path pathLogs;

	@Option(names = { "--keep-configs" }, description = "Do not convert configs to JSON.")
    private boolean keepConfigs;

	@Parameters(index = "0", description = "The name of the configuration to run.")
    private String configuration;

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
			// Get the logger for "org.jnativehook" and set the level to warning.
			Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
			logger.setLevel(Level.WARNING);

			// Don't forget to disable the parent handlers.
			logger.setUseParentHandlers(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		new CommandLine(new PCemMain()).execute(args);
	}

	@Override
	public void run() {
		try {
			PCemWindow main = new PCemWindow(config);
			main.setMachinesPath(pathMachines);
			main.setKeepConfigs(keepConfigs);
			
			PCem pcem = PCem.getInstance();
			
			Path configPath = !Files.isDirectory(config) ? config.getParent() : config;
			
			pcem.setRomsPaths(pathROMs != null ? pathROMs : configPath.resolve("roms"));
			pcem.setNVRPath(pathNVR != null ? pathNVR : configPath.resolve("nvr"));
			pcem.setLogsPath(pathLogs != null ? pathLogs : configPath.resolve("logs"));

			main.showWindow();
			main.start(configuration);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
