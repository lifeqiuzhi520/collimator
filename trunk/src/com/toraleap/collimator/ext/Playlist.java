package com.toraleap.collimator.ext;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.util.Log;

/**
 * 系统的媒体存储播放列表。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1028
 */
public final class Playlist {
	
	private final ContentResolver mResolver;
	private final String[] mItems;
	
	/**
	 * 使用完整路径文件名数组构造一个播放列表。
	 */
	public Playlist(ContentResolver resolver, String[] items) {
		mResolver = resolver;
		mItems = items;
	}
	
	/**
	 * 用给定名称在媒体库中创建一个新的播放列表，并尝试将数组里的文件加入到列表中。若存在同名播放列表，先试图删除原有列表。
	 * @param name	新播放列表名称
	 * @return 成功添加的条目数
	 */
	public int createNew(String name) {
		long[] ids = toMediaId();
		if (ids.length == 0) return 0;
		removeIfExist(name);
        ContentValues[] values = new ContentValues[ids.length];
        Uri uri = createPlaylist(name);
        for (int i = 0; i < ids.length; i++) {
            values[i] = new ContentValues();
            values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(i));
            values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, ids[i]);
        }		
        return mResolver.bulkInsert(uri, values);
	}
	
	/**
	 * 在媒体库中查询指定的播放列表，若存在则将其删除。
	 * @param name	播放列表名称
	 */
	public void removeIfExist(String name) {
        String whereclause = MediaStore.Audio.Playlists.NAME + " == '" + name.replace("'", "''") + "'";
        Cursor cursor = mResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
        		new String[] { MediaStore.Audio.Playlists._ID }, whereclause, null, MediaStore.Audio.Playlists.NAME);
        if (cursor != null && cursor.getCount() > 0) {
        	cursor.moveToFirst();
            long id = cursor.getLong(0);
            mResolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MediaStore.Audio.Playlists._ID + " == " + id, null);
        }
        if (cursor != null) cursor.close();
	}
	
	public PlaylistPair[] getPlaylists() {
		List<PlaylistPair> list = new ArrayList<PlaylistPair>();
        String[] cols = new String[] {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };
        String whereclause = MediaStore.Audio.Playlists.NAME + " != ''";
        Cursor cursor = mResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            cols, whereclause, null,
            MediaStore.Audio.Playlists.NAME);
        if (cursor != null && cursor.getCount() > 0) {
        	cursor.moveToFirst();
            while (! cursor.isAfterLast()) {
                list.add(new PlaylistPair(cursor.getInt(0), cursor.getString(1)));
                cursor.moveToNext();
            }
        }
        if (cursor != null) cursor.close();
        return (PlaylistPair[]) list.toArray();
	}
	
	/**
	 * 以给定名字创建一个新的播放列表，并返回新播放列表的 URI。
	 * @param name	新播放列表的名字
	 * @return 新播放列表的 URI
	 */
	private Uri createPlaylist(String name) {
		ContentValues values = new ContentValues();
		values.put(MediaStore.Audio.PlaylistsColumns.NAME, name);
		return mResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
	}
	
	/**
	 * 查询媒体数据库，按顺序把每一个文件转换成媒体数据库中的 ID 表示。
	 * @return 整型 ID 数组
	 */
	private long[] toMediaId() {
		long[] list = new long[mItems.length];
		for (int i = 0; i < list.length; i++) list[i] = -1;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mItems.length; i++) {
			if (i > 0) sb.append(",");
			// Android 2.1 or earlier
			sb.append("'").append(mItems[i].replace("'", "''")).append("',");
			// Android 2.2
			sb.append("'/mnt").append(mItems[i].replace("'", "''")).append("'");
		}
		String where = MediaStore.Audio.AudioColumns.DATA + " IN (" + sb.toString() + ")";
		try {
			Cursor cursor = mResolver.query(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					new String[] { Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.DATA }, 
					where, null, null);
			if (cursor == null) return new long[0];
			for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				String data = cursor.getString(1);
				for (int i = 0; i < mItems.length; i++) {
					if (data.endsWith(mItems[i])) {
						list[i] = cursor.getLong(0);
						break;
					}
				}
			}
		}
		catch (SQLiteException e) {
			Log.e("SQL", e.getMessage());
		}
		return shrinkLongArray(list);
	}
	
	/**
	 * 压缩一个 long 型数组，保留其非负元素。
	 * @param source	输入数组
	 * @return 仅含非负元素的数组
	 */
	public long[] shrinkLongArray(long[] source) {
		int count = 0;
		for (int i = 0; i < source.length; i++)
			if (source[i] >= 0) count++;
		long[] result = new long[count];
		count = 0;
		for (int i = 0; i < source.length; i++)
			if (source[i] >= 0) result[count++] = source[i];
		return result;
	}
	
	public class PlaylistPair {
		private long mId;
		private String mName;
		
		public PlaylistPair(int id, String name) {
			mId = id;
			mName = name;
		}
		
		public long getId() { return mId; }
		public String getName() { return mName; }
	}
}
