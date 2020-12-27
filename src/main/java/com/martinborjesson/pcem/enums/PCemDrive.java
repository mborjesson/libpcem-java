package com.martinborjesson.pcem.enums;

public enum PCemDrive {
	FLOPPY_A(1),
	FLOPPY_B(2),
	CD(3),
	ZIP(4),
	CASSETTE(5);
	
	final int value;
	
	private PCemDrive(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
