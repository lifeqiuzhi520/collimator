package com.toraleap.collimator.dal;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	private static final int DB_VERSION = 2;
	private static final String DB_NAME = "collimator.db";

	public DBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db = super.getWritableDatabase();
        return db;
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT UNIQUE, %s INTEGER)", 
				DBColumns.TagTable, DBColumns.TagId, DBColumns.TagName, DBColumns.Type));
		db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s INTEGER, %s INTEGER)", 
				DBColumns.FileTable, DBColumns.FileId, DBColumns.Name, DBColumns.Path, DBColumns.Size, DBColumns.LastModified));
		db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER REFERENCES %s(%s), %s INTEGER REFERENCES %s(%s))", 
				DBColumns.LinkTable, DBColumns.LinkId, DBColumns.FileId, DBColumns.FileTable, DBColumns.FileId, DBColumns.TagId, DBColumns.TagTable, DBColumns.TagId));
		db.execSQL(String.format("CREATE VIEW %s AS SELECT f.%s, f.%s, f.%s, f.%s, f.%s, t.%s, t.%s, t.%s FROM %s f INNER JOIN %s l ON f.%s = l.%s INNER JOIN %s t ON l.%s = t.%s", 
				DBColumns.ViewTable,	DBColumns.FileId, DBColumns.Name, DBColumns.Path, DBColumns.Size, DBColumns.LastModified, DBColumns.TagId, DBColumns.TagName, DBColumns.Type, DBColumns.FileTable,
				DBColumns.LinkTable, DBColumns.FileId, DBColumns.FileId, DBColumns.TagTable, DBColumns.TagId, DBColumns.TagId));
		db.execSQL(String.format("CREATE INDEX tag_name_index ON %s (%s)", DBColumns.TagTable, DBColumns.TagName));
		Log.e("COLLIMATOR", "DB_CREATED");
		//db.execSQL(String.format("CREATE VIEW view_tag AS SELECT (SELECT %s )", TagColumns.Name))
//		db.execSQL(String.format("CREATE INDEX filetag_file_index ON filetag (%s)", FileTagColumns.FileId));
//		db.execSQL(String.format("CREATE INDEX filetag_tag_index ON filetag (%s)", FileTagColumns.TagId));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(String.format("DROP TABLE IF EXISTS %s", DBColumns.TagTable));
		db.execSQL(String.format("DROP TABLE IF EXISTS %s", DBColumns.FileTable));
		db.execSQL(String.format("DROP TABLE IF EXISTS %s", DBColumns.LinkTable));
	    onCreate(db);
	}

}
