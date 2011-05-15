package com.toraleap.collimator.dal;

import java.io.File;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.toraleap.collimator.model.BaseTag;

public class DBOperation {
	SQLiteDatabase mDb;
	
	public DBOperation(Context context) {
		mDb = new DBHelper(context).getWritableDatabase();
	}
	
	public void close() {
		mDb.close();
	}
	
	public Cursor queryFilesByTag(String tag) {
		return mDb.rawQuery(String.format("SELECT f.%s, f.%s, f.%s FROM %s f WHERE f.%s IN (SELECT DISTINCT l.%s FROM %s l WHERE l.%s IN (SELECT t.%s FROM %s t WHERE t.%s LIKE %s))",
				DBColumns.FileId, DBColumns.Name, DBColumns.Path, DBColumns.FileTable, DBColumns.FileId,
				DBColumns.FileId, DBColumns.LinkTable, DBColumns.TagId,
				DBColumns.TagId, DBColumns.TagTable, DBColumns.TagName,
				DatabaseUtils.sqlEscapeString("_" + tag + "_")), null);
	}
	
	public Cursor queryTagsByFile(String filename) {
		return mDb.rawQuery(String.format("SELECT t.%s, t.%s, t.%s FROM %s t WHERE t.%s IN (SELECT l.tagid FROM filetag l WHERE l.fileid = (SELECT f.id FROM file f WHERE f.path = %s))",
				DBColumns.TagId, DBColumns.Type, DBColumns.TagName, DBColumns.TagTable, DBColumns.TagId,
				DBColumns.TagId, DBColumns.LinkTable, DBColumns.FileId,
				DBColumns.FileId, DBColumns.FileTable, DBColumns.Path,
				DatabaseUtils.sqlEscapeString(filename)), null);
	}
	
	public boolean isFileExists(File file) {
		return mDb.rawQuery(String.format("SELECT %s FROM %s WHERE %s = %s",
				DBColumns.FileId, DBColumns.FileTable, DBColumns.Path,
				DatabaseUtils.sqlEscapeString(file.getPath())), null).getCount() > 0;
	}
	
	public boolean isFileModified(File file) {
		return mDb.rawQuery(String.format("SELECT %s FROM %s WHERE %s = %s AND %s = %s AND %s = %s",
				DBColumns.FileId, DBColumns.FileTable, DBColumns.Path,
				DatabaseUtils.sqlEscapeString(file.getPath()),
				DBColumns.Size, Long.toString(file.length()),
				DBColumns.LastModified, Long.toString(file.lastModified())), null).getCount() > 0;
	}
	
	public boolean insertFile(File file, ArrayList<BaseTag> tags) {
		Cursor cursor;
		ContentValues cv = new ContentValues();
		mDb.beginTransaction();
		// Create tags
		for (BaseTag tag : tags) {
			cv.clear();
			cv.put(DBColumns.TagName, tag.Name);
			cv.put(DBColumns.Type, tag.Type);
			mDb.insert(DBColumns.TagTable, null, cv);
			cursor = mDb.rawQuery("SELECT last_insert_rowid()", null);
			cursor.moveToFirst();
			tag.Id = cursor.getLong(0);
		}
		// Insert file
		cv.clear();
		cv.put(DBColumns.Name, file.getName());
		cv.put(DBColumns.Path, file.getPath());
		cv.put(DBColumns.Size, file.length());
		cv.put(DBColumns.LastModified, file.lastModified());
		mDb.insert(DBColumns.FileTable, null, cv);
		cursor = mDb.rawQuery("SELECT last_insert_rowid()", null);
		cursor.moveToFirst();
		long lastid = cursor.getLong(0);
		// Link file with tags
		for (BaseTag tag : tags) {
			cv.clear();
			cv.put(DBColumns.FileId, lastid);
			cv.put(DBColumns.TagId, tag.Id);
			mDb.insert(DBColumns.LinkTable, null, cv);
		}
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
		return false;
	}
	
	public boolean updateFile(File file, ArrayList<BaseTag> tags) {
		return false;
	}
	
	public boolean removeFile(File file) {
		return mDb.delete(DBColumns.FileTable, String.format("%s = %s", DBColumns.Path, DatabaseUtils.sqlEscapeString(file.getPath())), null) > 0;
	}
}
