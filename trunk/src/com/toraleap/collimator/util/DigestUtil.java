package com.toraleap.collimator.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.mozilla.universalchardet.UniversalDetector;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.toraleap.collimator.R;

/**
 * 包含获取文件摘要的相关静态工具函数。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1029
 */
public class DigestUtil extends SoftCache<String, Spanned> {

	private Spanned mDefaultDigest;
	private Spanned mLoadingDigest;
	private String mNullData;
	private String mCharset;
	private Context mContext;
	private SharedPreferences mPrefs;
	private boolean isDisplayDigest;
	
	/**
	 * 初始化文件摘要工具实例。
	 * @param context	程序上下文(建议在 Activity 及其派生类中用 getApplicationContext() 获得)
	 */
	public DigestUtil(Context context, Handler handler) {
		super(handler);
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mDefaultDigest = Html.fromHtml(context.getString(R.string.util_digest_none));
		mLoadingDigest = Html.fromHtml(context.getString(R.string.util_digest_loading));
		mNullData = context.getString(R.string.util_digest_nulldata);
		mCharset = context.getString(R.string.util_digest_charset);
		isDisplayDigest = mPrefs.getBoolean("display_digest", true);
	}

	@Override
	Spanned request(String key) {
		return fromFile(key);
	}
	
	@Override
	int getMaxQueueLength() {
		return 10;
	}

	@Override
	Spanned getDefault() {
		return mLoadingDigest;
	}
	
	/**
	 * 根据完整的路径文件名获取其对应的摘要信息。对于文本文件，返回前120字节的数据；对于图片文件，返回图片尺寸；对于音乐文件，返回艺术家及专辑信息；其他类型返回null。
	 * @param filename	完整的路径文件名
	 * @return 数据字符串
	 */
	private Spanned fromFile(String filename) {
		Spanned digest = null;
		if (isDisplayDigest) {
			String mimeType = FileInfo.mimeType(filename);
			if (mimeType.startsWith("image/")) {
				digest = loadFromImage(filename);
			} else if (mimeType.startsWith("video/")) {
				digest = loadFromVideo(filename);
			} else if (mimeType.startsWith("audio/")) {
				digest = loadFromAudio(filename);
			} else if (mimeType.startsWith("text/")) {
				digest = loadFromText(filename);
			} else if (mimeType.startsWith("application/zip")) {
				digest = loadFromArchive(filename);
			} else if (mimeType.equals("application/vnd.android.package-archive")) {
				digest = loadFromApk(filename);
			}
		}
		return (null == digest) ? mDefaultDigest : digest;
	}
	
	/**
	 * 从给定文本文件中加载前120字节的数据。
	 * @param path	完整的路径文件名
	 * @return 数据字符串
	 */
	private Spanned loadFromText(String path) {
		Spanned digest = null;
		byte[] buffer = new byte[256];
		try {
			FileInputStream input = new FileInputStream(path);
			int length = input.read(buffer);
			input.close();
			if (length >= 0) {
				String str = format(buffer, length).replaceAll("[\\s\\t\\r\\n]+", " ");
				digest = Html.fromHtml(str.substring(0, str.length() - 1));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return digest;
	}
	
	/**
	 * 从给定音频文件中加载摘要数据。
	 * @param path	完整的路径文件名
	 * @return 数据字符串
	 */
	private Spanned loadFromAudio(String path) {
		Spanned digest = null;
		String where = MediaStore.Audio.AudioColumns.DATA + " IN ('" + path.replace("'", "''") + "','/mnt" + path.replace("'", "''") + "')";
		Cursor cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
				null, where, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			String title = format(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
			String album = format(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));  
			String artist = format(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));  
			int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
			cursor.close();
			digest = Html.fromHtml(mContext.getString(R.string.util_digest_audio_format, title, artist, album, durationToString(duration)));
		}
		return digest;
	}
	
	/**
	 * 从给定视频文件中加载摘要数据。
	 * @param path	完整的路径文件名
	 * @return 数据字符串
	 */
	private Spanned loadFromVideo(String path) {
		Spanned digest = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setMode(MediaMetadataRetriever.MODE_GET_METADATA_ONLY);
            retriever.setDataSource(path);
            String width = format(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            String height = format(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            String duration = format(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
			digest = Html.fromHtml(mContext.getString(R.string.util_digest_video_format, width, height, durationToString(duration)));
        } catch (Exception ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodError err) {
        	err.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
		return digest;
	}
	
	/**
	 * 从给定图像文件中加载摘要数据。
	 * @param path	完整的路径文件名
	 * @return	 数据字符串
	 */
	private Spanned loadFromImage(String path) {
		Spanned digest = null;
		try {
			ExifInterface exif = new ExifInterface(path);
			String width = format(exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
			String height = format(exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
			String model = format(exif.getAttribute(ExifInterface.TAG_MODEL));
			String date = format(exif.getAttribute(ExifInterface.TAG_DATETIME));
			if (width == null || height == null || width.equals("0") || height.equals("0")) {
				Options options = new Options();
				options.inSampleSize = 1;
		        options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, options);
				width = String.valueOf(options.outWidth);
				height = String.valueOf(options.outHeight);
			}
			digest = Html.fromHtml(mContext.getString(R.string.util_digest_image_format, width, height, model, date));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return digest;
	}
	
	/**
	 * 从给定压缩文件中加载摘要数据。
	 * @param path	完整的路径文件名
	 * @return	 数据字符串
	 */
	private Spanned loadFromArchive(String path) {
		Spanned digest = null;
		StringBuilder sb = new StringBuilder();
		int count = 0;
		long size = 0;
		try {
			ZipFile zip = new ZipFile(path);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (count < 3) sb.append("<br>").append(entry.getName());
				count++;
				size += entry.getSize();
			}
			digest = Html.fromHtml(mContext.getString(R.string.util_digest_archive_format, count, FileInfo.sizeString(size), sb.toString()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return digest;
	}
	
	/**
	 * 从给定应用程序包文件中加载摘要数据。
	 * @param path	完整的路径文件名
	 * @return	 数据字符串
	 */
	private Spanned loadFromApk(String path) {
		Spanned digest = null;
		PackageManager pm = mContext.getPackageManager();      
        PackageInfo info = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);      
        if(info != null){
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = path;
            appInfo.publicSourceDir = path;
            String applabel = format(pm.getApplicationLabel(appInfo).toString());
            String packagename = format(info.packageName);
            String version = format(info.versionName);
            digest = Html.fromHtml(mContext.getString(R.string.util_digest_apk_format, applabel, version, packagename));
        }
		return digest;
	}
	
	private String format(String data) {
		if (data == null || data.trim().length() == 0) return mNullData;
		else return data;
	}
	
	private String format(byte[] data, int length) {
    	try {
			UniversalDetector detector = new UniversalDetector(null);
			detector.handleData(data, 0, data.length);
	        String encoding = detector.getDetectedCharset();
        	if (encoding == null) encoding = mCharset;
			return new String(data, 0, length, encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new String(data, 0, length);
		}
	}
	
	private String durationToString(int duration) {
		return String.format("%d:%02d", duration / (1000 * 60), (duration / 1000) % 60);
	}
	
	private String durationToString(String durationString) {
		return durationToString(Integer.parseInt(durationString));
	}
}