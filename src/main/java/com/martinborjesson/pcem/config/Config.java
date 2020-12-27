package com.martinborjesson.pcem.config;

import java.util.Set;

abstract public class Config {
	static public final String DEFAULT_SECTION = "default";
	
	private Object object;
	
	void setObject(Object object) {
		this.object = object;
	}
	
	Object getObject() {
		return object;
	}
	
	abstract public Object getValue(String section, String key);
	abstract public void setValue(String section, String key, Object value);

	abstract public Integer getInt(String section, String key);
	abstract public Float getFloat(String section, String key);
	abstract public String getString(String section, String key);
	
	abstract public void setFloat(String section, String key, float value);
	abstract public void setInt(String section, String key, int value);
	abstract public void setString(String section, String key, String value);
	
	abstract public Set<String> getSections();
	abstract public Set<String> getSectionKeys(String section);
	
	public void copyTo(Config config) {
		for (String section : getSections()) {
			for (String key : getSectionKeys(section)) {
				Object value = getValue(section, key);
				
				if (value instanceof Float) {
					config.setFloat(section, key, (Float) value);
				} else if (value instanceof Integer) {
					config.setInt(section, key, (Integer) value);
				} else if (value instanceof String) {
					config.setString(section, key, (String) value);
				}
			}
		}
	}
}
