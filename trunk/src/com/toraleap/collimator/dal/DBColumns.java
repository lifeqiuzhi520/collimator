package com.toraleap.collimator.dal;

import android.provider.BaseColumns;

public interface DBColumns extends BaseColumns {
    /**
     * The name for file table.
     * <P>Type: TEXT</P>
     */
	public static final String FileTable = "file";
    /**
     * The name for tag table.
     * <P>Type: TEXT</P>
     */
	public static final String TagTable = "tag";
    /**
     * The name for link table.
     * <P>Type: TEXT</P>
     */
	public static final String LinkTable = "link";
	/**
     * The name for this view.
     * <P>Type: TEXT</P>
     */
	public static final String ViewTable = "view_file_tags";
    /**
     * The unique id for a file.
     * <P>Type: TEXT</P>
     */
	public static final String FileId = "_file_id";
    /**
     * The name for a file.
     * <P>Type: TEXT</P>
     */
	public static final String Name = "name";
    /**
     * The full path for a file.
     * <P>Type: TEXT</P>
     */
	public static final String Path = "path";
    /**
     * The size for a file.
     * <P>Type: INTEGER</P>
     */
	public static final String Size = "size";
    /**
     * The last modified timestamp for a file.
     * <P>Type: INTEGER</P>
     */
	public static final String LastModified = "modified";
    /**
     * The id for a tag.
     * <P>Type: TEXT</P>
     */
	public static final String TagId = "_tag_id";
    /**
     * The name for a tag.
     * <P>Type: TEXT</P>
     */
	public static final String TagName = "tagname";
    /**
     * The type for a tag.
     * <P>Type: INTEGER</P>
     */
	public static final String Type = "type";
    /**
     * The unique id for a link.
     * <P>Type: TEXT</P>
     */
	public static final String LinkId = "_link_id";
}
