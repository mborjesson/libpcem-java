package com.martinborjesson.pcem.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.martinborjesson.pcem.PCem;

public class ConfigIOPCemNative extends ConfigIO {
	public ConfigIOPCemNative(Path path) {
		super(path);
	}
	
	@Override
	public Config create() {
		return new ConfigPCemNative();
	}
	
	@Override
	public Config load() throws IOException {
		if (!(Files.exists(getPath()) && Files.isRegularFile(getPath()))) {
			return null;
		}
		
		ConfigPCemNative config = new ConfigPCemNative();
		
		if (config.getObject() instanceof Long) {
			PCem.configFree((long) config.getObject());
		}
		
		long handle = PCem.configLoad(getPath());
		
		if (handle == 0) {
			return null;
		}
		
		config.setObject(handle);
		
		return config;
	}
	
	@Override
	public Config save(Config config) throws IOException {
		
		if (config.getObject() instanceof Long) {
			PCem.configSave((long) config.getObject(), getPath());
			
			return config;
		}
		
		return null;
	}

	@Override
	public Config close(Config config) throws IOException {
		
		if (config.getObject() instanceof Long) {
			PCem.configFree((long) config.getObject());
			
			return config;
		}
		
		return null;
	}
}
