package com.martinborjesson.pcem.enums;

public enum PCemConfigType {
	INT(1),
	FLOAT(2),
	STRING(3);
	
	final int value;
	
	private PCemConfigType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
