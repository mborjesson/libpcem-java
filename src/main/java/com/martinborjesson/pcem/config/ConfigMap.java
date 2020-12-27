package com.martinborjesson.pcem.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ConfigMap extends Config {
	static private class Section extends HashMap<String, Object> {
		private static final long serialVersionUID = 1L;
	}

	final private Map<String, Section> map = new HashMap<>();
	
	ConfigMap() {
	}
	
	private Section getSection(String section, boolean create) {
		if (section == null) {
			section = DEFAULT_SECTION;
		}
		Section s = (Section) map.get(section);
		if (s == null && create) {
			s = new Section();
			map.put(section, s);
		}
		return s;
	}

	@Override
	public Object getValue(String section, String key) {
		return Optional.ofNullable(getSection(section, false)).map(s -> s.get(key)).orElse(null);
	}
	
	@Override
	public void setValue(String section, String key, Object value) {
		getSection(section, true).put(key, value);
	}

	@Override
	public Integer getInt(String section, String key) {
		Object o = getValue(section, key);
		if (o instanceof Number) {
			return ((Number) o).intValue();
		}
		if (o != null) {
			try {
				return Integer.valueOf(String.valueOf(o));
			} catch (NumberFormatException e) {}
		}
		return null;
	}

	@Override
	public Float getFloat(String section, String key) {
		Object o = getValue(section, key);
		if (o instanceof Number) {
			return ((Number) o).floatValue();
		}
		if (o != null) {
			try {
				return Float.valueOf(String.valueOf(o));
			} catch (NumberFormatException e) {}
		}
		return null;
	}

	@Override
	public String getString(String section, String key) {
		Object o = getValue(section, key);
		if (o instanceof String) {
			return (String) o;
		}
		if (o != null) {
			return String.valueOf(o);
		}
		return null;
	}
	
	@Override
	public void setFloat(String section, String key, float value) {
		getSection(section, true).put(key, value);
	}
	
	@Override
	public void setInt(String section, String key, int value) {
		getSection(section, true).put(key, value);
	}
	
	@Override
	public void setString(String section, String key, String value) {
		getSection(section, true).put(key, value);
	}
	
	@Override
	public Set<String> getSections() {
		return Collections.unmodifiableSet(map.keySet());
	}
	
	@Override
	public Set<String> getSectionKeys(String section) {
		return Optional.ofNullable(getSection(section, false)).map(Section::keySet).map(Collections::unmodifiableSet).orElseGet(() -> Collections.emptySet());
	}
	
}
