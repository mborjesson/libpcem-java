package com.martinborjesson.pcem.config;

import java.util.Set;

import com.martinborjesson.pcem.PCem;

public class ConfigPCemNative extends Config {
	final private ConfigMap config = new ConfigMap();
	
	ConfigPCemNative() {
	}

	@Override
	public Object getValue(String section, String key) {
		return config.getValue(section, key);
	}

	@Override
	public void setValue(String section, String key, Object value) {
		config.setValue(section, key, value);
	}

	@Override
	public Integer getInt(String section, String key) {
		Integer v = PCem.configGetInt((long) getObject(), section, key);
		if (v != null) {
			config.setInt(section, key, v);
		}
		return v;
	}

	@Override
	public Float getFloat(String section, String key) {
		Float v = PCem.configGetFloat((long) getObject(), section, key);
		if (v != null) {
			config.setFloat(section, key, v);
		}
		return v;
	}

	@Override
	public String getString(String section, String key) {
		String v = PCem.configGetString((long) getObject(), section, key);
		if (v != null) {
			config.setString(section, key, v);
		}
		return v;
	}

	@Override
	public void setFloat(String section, String key, float value) {
		config.setFloat(section, key, value);
		PCem.configSetFloat((long) getObject(), section, key, value);
	}

	@Override
	public void setInt(String section, String key, int value) {
		config.setInt(section, key, value);
		PCem.configSetInt((long) getObject(), section, key, value);
	}

	@Override
	public void setString(String section, String key, String value) {
		config.setString(section, key, value);
		PCem.configSetString((long) getObject(), section, key, value);
	}

	@Override
	public Set<String> getSections() {
		return config.getSections();
	}

	@Override
	public Set<String> getSectionKeys(String section) {
		return config.getSectionKeys(section);
	}
	
}
