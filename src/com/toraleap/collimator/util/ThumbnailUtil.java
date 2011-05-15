package com.toraleap.collimator.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * 缩略图获取工具类。内部包含缩略图软引用缓存器，当内存不足时能自动释放缓存以保证系统正常运行。内建缩略图尺寸为 96x96。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1029
 */
public class ThumbnailUtil extends SoftCache<String, Bitmap> {

	private Bitmap mDefaultThumbnail;
	private Bitmap mLoadingThumbnail;
	private Context mContext;
	private SharedPreferences mPrefs;
	private boolean isDisplayThumbnail;
	
	/**
	 * 初始化缩略图工具实例。
	 * @param context	程序上下文(建议在 Activity 及其派生类中用 getApplicationContext() 获得)
	 */
	public ThumbnailUtil(Context context, Handler handler) {
		super(handler);
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		refresh();
	}
	
	/**
	 * 刷新缩略图获取设置。
	 */
	public void refresh() {
		isDisplayThumbnail = mPrefs.getBoolean("display_thumbnail", true);
	}

	@Override
	Bitmap request(String key) {
		return fromFile(key);
	}
	
	@Override
	int getMaxQueueLength() {
		return 10;
	}
	
	@Override
	Bitmap getDefault() {
		if (null == mLoadingThumbnail) {
			try {
				InputStream is = mContext.getAssets().open("icons/loading.png");
				mLoadingThumbnail = BitmapFactory.decodeStream(is);
				is.close();
			} catch (IOException e) {
				mLoadingThumbnail = getUndefined();
			}
		}
		return mLoadingThumbnail;
	}
	
	/**
	 * 根据完整的路径文件名获取其对应的缩略图。对于图片文件，尝试加载该图片并转换为缩略图；对于音乐文件，尝试加载对应封面图片；其他类型尝试加载资源文件里与扩展名对应的图标作为缩略图。如果未找到对应图标，则返回默认图标。
	 * @param filename	完整的路径文件名
	 * @return 缩略图位图
	 */
	private Bitmap fromFile(String filename) {
		Bitmap thumbnail = null;
		if (isDisplayThumbnail) {
			String mimeType = FileInfo.mimeType(filename);
			if (mimeType.startsWith("image/")) {
				thumbnail = loadFromImage(filename);
			} else if (mimeType.startsWith("video/")) {
				thumbnail = loadFromVideo(filename);
			} else if (mimeType.startsWith("audio/")) {
				thumbnail = loadFromAudio(filename);
			} else if (mimeType.startsWith("text/plain")) {
				thumbnail = loadFromAudio(filename);
			} else if (mimeType.equals("application/vnd.android.package-archive")) {
				thumbnail = loadFromApk(filename);
			}
		}
		return (null == thumbnail) ? fromExt(filename) : thumbnail;
	}
	
	/**
	 * 从给定图片文件中加载图片，并转换为缩略图。
	 * @param path	完整的路径文件名
	 * @return	 缩略图位图
	 */
	private Bitmap loadFromImage(String path) {
		Bitmap thumbnail = null;
		try{
			Options options = new Options();
			// 读取边界以确定图像大小
			options.inSampleSize = 1;
	        options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
	        if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) return fromExt(path);
	        // 设置采样率并真正读取图像
	        setSampleSize(options);
	        options.inDither = true;
	        options.inJustDecodeBounds = false;
	        //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			Bitmap bitmap = BitmapFactory.decodeFile(path, options);
			if (null != bitmap) {
				thumbnail = scaleBitmap(bitmap);
				bitmap.recycle();
				bitmap = null;
			}
			return thumbnail;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 从给定音频文件所在文件夹中加载封面图片，并转换为缩略图。
	 * @param path	完整的路径文件名
	 * @return	 缩略图位图
	 */
	private Bitmap loadFromAudio(String path) {
		String folder = path.substring(0, path.lastIndexOf("/"));
		if (new File(folder + "/AlbumArt.jpg").exists()) {
			return requestAndCache(folder + "/AlbumArt.jpg");
		} else if (new File(folder + "/cover.jpg").exists()) {
			return requestAndCache(folder + "/cover.jpg");
		} else if (new File(folder + "/folder.jpg").exists()) {
			return requestAndCache(folder + "/folder.jpg");
		}
		return null;
	}
	
	/**
	 * 从给定视频文件中读取帧，并转换为缩略图。
	 * @param path	完整的路径文件名
	 * @return	 缩略图位图
	 */
	private Bitmap loadFromVideo(String path) {
		Bitmap thumbnail = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setMode(MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
            retriever.setDataSource(path);
            Bitmap bitmap = retriever.captureFrame();
            thumbnail = scaleBitmap(bitmap);
			bitmap.recycle();
			bitmap = null;
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
		return thumbnail;
    }
	
	/**
	 * 从给定应用程序包文件中加载缩略图。
	 * @param path	完整的路径文件名
	 * @return	 缩略图位图
	 */
	private Bitmap loadFromApk(String path) {
		Bitmap thumbnail = null;
		PackageManager pm = mContext.getPackageManager();      
        PackageInfo info = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);      
        if(info != null){
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = path;
            appInfo.publicSourceDir = path;
            Drawable drawable = pm.getApplicationIcon(appInfo);
            thumbnail = scaleBitmap(((BitmapDrawable)drawable).getBitmap());
        }
		return thumbnail;
	}

	/**
	 * 按照扩展名加载资源文件里与扩展名对应的图标作为缩略图。如果未找到对应图标，则返回默认图标。
	 * @param filename	文件名
	 * @return	 缩略图位图
	 */
	private Bitmap fromExt(String filename) {
		String ext = FileInfo.extension(filename);
		Bitmap thumbnail = getCache(ext);
		if (thumbnail != null) return thumbnail;
		try {
			InputStream is = mContext.getAssets().open("icons/ext/" + ext + ".png");
			thumbnail = scaleBitmap(BitmapFactory.decodeStream(is));
			is.close();
		} catch (IOException e) {
			try {
				String mime = FileInfo.mimeType(filename).replace('/', '_');
				InputStream is = mContext.getAssets().open("icons/mime/" + mime + ".png");
				thumbnail = scaleBitmap(BitmapFactory.decodeStream(is));
				is.close();
			} catch (IOException ex) {
				try {
					String mimetype = FileInfo.mimeType(filename).split("/")[0];
					InputStream is = mContext.getAssets().open("icons/mime/" + mimetype + ".png");
					thumbnail = scaleBitmap(BitmapFactory.decodeStream(is));
					is.close();
				} catch (IOException exc) {
					exc.printStackTrace();
					thumbnail = getUndefined();
				}
			}
		}
		return putCache(ext, thumbnail);
	}
	
	/**
	 * 获取默认的图标作为缩略图。
	 * @return	缩略图位图
	 */
	public Bitmap getUndefined() {
		if (null == mDefaultThumbnail) {
			try {
				InputStream is = mContext.getAssets().open("icons/undefined.png");
				mDefaultThumbnail = scaleBitmap(BitmapFactory.decodeStream(is));
				is.close();
			} catch (IOException e) {
				mDefaultThumbnail = null;
			}
		}
		return mDefaultThumbnail;
	}

	private static void setSampleSize(Options options) {
		int maxSize = options.outWidth > options.outHeight ? options.outWidth : options.outHeight;
		int minSample = (maxSize / 96) >> 1 ;
		int sample = 1;
		while (sample <= minSample) sample <<= 1;
		options.inSampleSize = sample;
	}
	
	private static Bitmap scaleBitmap(Bitmap source) {
		int maxSize = source.getWidth() > source.getHeight() ? source.getWidth() : source.getHeight();
        return Bitmap.createScaledBitmap(source, source.getWidth() * 96 / maxSize, source.getHeight() * 96 / maxSize, true);
	}
}