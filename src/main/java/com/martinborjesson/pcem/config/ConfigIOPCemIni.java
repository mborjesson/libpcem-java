package com.martinborjesson.pcem.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public class ConfigIOPCemIni extends ConfigIO {
	public ConfigIOPCemIni(Path path) {
		super(path);
	}
	
	@Override
	public Config create() {
		return new ConfigMap();
	}
	
	public Config load() throws IOException {
		if (!(Files.exists(getPath()) && Files.isRegularFile(getPath()))) {
			return null;
		}
		Config config = new ConfigMap();
		// PCem-configurations are like INI-files but missing a node at the start
		try (BufferedReader r = new BufferedReader(new InputStreamReader(Files.newInputStream(getPath())))) {
			List<String> lines = new ArrayList<>();
			String line = null;
			while ((line = r.readLine()) != null) {
				lines.add(line);
			}
			lines.add(0, "[" + Config.DEFAULT_SECTION + "]");
			
			try (StringReader reader = new StringReader(lines.stream().collect(Collectors.joining(System.lineSeparator())))) {
				Ini ini = new Ini(reader);
				
				for (String section : ini.keySet()) {
					Section s = ini.get(section);
					if (s != null) {
						for (String key : s.keySet()) {
							String value = s.get(key);
							config.setValue(section, key, value);
						}
					}
				}
				return config;
			}
		}
	}
	
	private void writeSection(Config config, String section, List<String> output) {
		for (String key : config.getSectionKeys(section)) {
			Object value = config.getValue(section, key);
			if (value != null) {
				output.add(key + " = " + value);
			}
		}
		output.add("");
	}
	
	public Config save(Config config) throws IOException {
		List<String> output = new ArrayList<>();
		
		Set<String> sections = new HashSet<>(config.getSections());
		sections.remove(Config.DEFAULT_SECTION);
		
		writeSection(config, Config.DEFAULT_SECTION, output);
		
		for (String section : sections) {
			output.add("[" + section + "]");
			writeSection(config, section, output);
		}
		
		Files.write(getPath(), output);
		
		return config;
	}
	
	@Override
	public Config close(Config config) throws IOException {
		return null;
	}
}
