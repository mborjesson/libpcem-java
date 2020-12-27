package com.martinborjesson.pcem.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;

public class ConfigIOJSON extends ConfigIO {
	public ConfigIOJSON(Path path) {
		super(path);
	}
	
	public Config create() {
		return new ConfigMap();
	}
	
	public Config load() throws IOException {
		if (!(Files.exists(getPath()) && Files.isRegularFile(getPath()))) {
			return null;
		}
		
		Config config = new ConfigMap();
		
		JSONObject json = new JSONObject(Files.readString(getPath()));
		
		for (String section : json.keySet()) {
			JSONObject s = json.optJSONObject(section);
			if (s != null) {
				for (String key : s.keySet()) {
					Object value = s.opt(key);
					if (value != null) {
						config.setValue(section, key, value);
					}
				}
			}
		}
		
		return config;
	}
	
	public Config save(Config config) throws IOException {
		JSONObject json = new JSONObject();
		
		for (String section : config.getSections()) {
			JSONObject sectionJSON = new JSONObject();
			
			for (String key : config.getSectionKeys(section)) {
				Object value = config.getValue(section, key);
				
				sectionJSON.put(key, value);
			}
			
			json.put(section, sectionJSON);
		}
		
		Files.writeString(getPath(), json.toString(4));
		
		return config;
	}
	
	@Override
	public Config close(Config config) throws IOException {
		return null;
	}
}
