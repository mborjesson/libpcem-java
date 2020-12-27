package com.martinborjesson.pcem.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

abstract public class ConfigIO {
	final private Path path;
	
	public ConfigIO(Path path) {
		this.path = path;
	}
	
	public Path getPath() {
		return path;
	}
	
	abstract public Config create();
	abstract public Config load() throws IOException;
	abstract public Config save(Config config) throws IOException;
	abstract public Config close(Config config) throws IOException;
	
	static public ConfigIO createFromPath(Path path) {
		if (path != null && Files.isRegularFile(path)) {
			String f = path.getFileName().toString().toLowerCase();
			if (f.endsWith(".cfg")) {
				return new ConfigIOPCemNative(path);
			} else if (f.endsWith(".json")) {
				return new ConfigIOJSON(path);
			}
		}
		return null;
	}
}
