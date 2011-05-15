package com.toraleap.collimator.bll;

import java.util.ArrayList;

import com.toraleap.collimator.model.BaseTag;

public class TagGenerator {
	
	private TagGenerator() { }
	
	public static ArrayList<BaseTag> generate(String filename) {
		ArrayList<BaseTag> tags = new ArrayList<BaseTag>();
		tags.add(new BaseTag(filename, BaseTag.TAG_TYPE_FILENAME));
		return tags;
	}
}
