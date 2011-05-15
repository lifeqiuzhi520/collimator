package com.toraleap.collimator.model;

public class BaseTag {
	public static final int TAG_TYPE_FILENAME = 0;
	public static final int TAG_TYPE_FOLDER = 1;
	public static final int TAG_TYPE_METADATA = 2;
	public static final int TAG_TYPE_LOCAL = 3;
	public static final int TAG_TYPE_REMOTE = 4;
	
	public String Name;
	public int Type;
	public long Id;
	
	public BaseTag(String name, int type) {
		Name = name;
		Type = type;
	}
}
