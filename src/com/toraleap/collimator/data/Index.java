package com.toraleap.collimator.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Spanned;

import com.toraleap.collimator.data.IndexData.DifferentVersionException;
import com.toraleap.collimator.data.IndexLoader.DeserializingException;
import com.toraleap.collimator.data.IndexLoader.NoSDCardException;
import com.toraleap.collimator.data.IndexLoader.SerializingException;
import com.toraleap.collimator.util.DigestUtil;
import com.toraleap.collimator.util.ThumbnailUtil;

/**
 * 包含文件索引的相关操作。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 */
public final class Index {

	private static final int MESSAGE_FIRST = 100;
	public static final int MESSAGE_NOSDCARD = MESSAGE_FIRST;
	public static final int MESSAGE_RELOAD_SUCCESS = MESSAGE_FIRST + 1;
	public static final int MESSAGE_RELOAD_FAILED = MESSAGE_FIRST + 2;
	public static final int MESSAGE_SERIALIZING_FAILED = MESSAGE_FIRST + 3;
	public static final int MESSAGE_DESERIALIZING_SUCCESS = MESSAGE_FIRST + 4;
	public static final int MESSAGE_DESERIALIZING_FAILED = MESSAGE_FIRST + 5;
	public static final int MESSAGE_DESERIALIZING_DIFFERENT_VERSION = MESSAGE_FIRST + 6;
	public static final int MESSAGE_UPDATE_THUMBNAIL = MESSAGE_FIRST + 10;
	public static final int MESSAGE_UPDATE_DIGEST = MESSAGE_FIRST + 11;
	public static final int STATUS_FAILED = 0;
	public static final int STATUS_OBSOLETE = 1;
	public static final int STATUS_READY = 2;
	public static final int STATUS_RELOADING = 3;
	public static final int STATUS_DESERIALIZING = 4;
	private static Handler sEventHandler;
	private static SharedPreferences sPrefs;
	
	private static IndexData data;
	private static int status;

	private static ThumbnailUtil thumbUtil;
	private static DigestUtil digestUtil;
	
	private Index() { }
	
	/**
	 * 返回文件索引的当前状态。
	 * @return 以STATUS为前缀的状态常数
	 */
	public static int getStatus() {
		return status;
	}
	
	/**
	 * 检查索引是否已过期，并设置当前状态。
	 */
	public static void checkObsolete() {
		if (status == STATUS_READY && (IndexLoader.neededReload(data) || sPrefs.getBoolean("index_is_obsolete", true))) {
    		sPrefs.edit().putBoolean("index_is_obsolete", true).commit();
    		status = STATUS_OBSOLETE;
		}
	}
	
	/**
	 * 获取文件索引中的条目总数。
	 * @return 条目总数
	 */
	public static int length() {
		if (data != null)
			return data.length();
		else
			return 0;
	}
	
	/**
	 * 获得文件索引的建立时间。
	 * @return	文件索引的建立时间
	 */
	public static long reloadTime() {
		return data.indexTime;
	}
	
	/**
	 * 获得SD卡在建立索引时的剩余空间。
	 * @return	剩余空间字节数
	 */
	public static long availableSpace() {
		return data.availableSpace;
	}
	
	/**
	 * 中断缩略图及摘要的读取工作。
	 */
	public static void interrupt() {
		thumbUtil.interrupt();
		digestUtil.interrupt();
	}
	
	/**
	 * 初始化文件索引与程序的关联。主程序改变首选项后应再次调用此函数。
	 * @param prefs		程序的首选项对象，从这里获得索引设置
	 * @param handler	主线程的消息处理器，索引线程产生的相关消息将发往此消息处理器
	 */
	public static void init(Context context, Handler handler) {
		sEventHandler = handler;
		sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		thumbUtil = new ThumbnailUtil(context, handler);
		digestUtil = new DigestUtil(context, handler);
	}
	
	/**
	 * 启动一个新线程异步重载索引。当索引完成时将自动进行序列化操作，并向消息处理器发送 RELOAD_FINISHED 消息。
	 */
	public static synchronized void reloadEntriesAsync() {
		status = STATUS_RELOADING;
		new Thread(new Runnable() {
        	public void run() {
        		interrupt();
    			data = null;
        		IndexLoader loader = new IndexLoader(sPrefs);
        		try {
	        		IndexData newData = loader.reload();
					IndexLoader.serialize(newData);
    				data = newData;
		    		sPrefs.edit().putBoolean("index_is_obsolete", false).commit();
		    		status = STATUS_READY;
		    		checkObsolete();
	        		sendHandlerMessage(MESSAGE_RELOAD_SUCCESS, 0, 0, null);
        		} catch (NoSDCardException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_NOSDCARD, 0, 0, null);
        		} catch (SerializingException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_SERIALIZING_FAILED, 0, 0, null);
        		}
            }
        }).start();
	}
	
	/**
	 * 启动新线程，反序列化唯一索引对象。当反序列化完成时会向消息处理器发送 DESERIALIZED_SUCCESS 或 DESERIALIZED_FAILED 消息。
	 */
	public static synchronized void deserialization() {
		status = STATUS_DESERIALIZING;
		new Thread(new Runnable() {
        	public void run() {
        		interrupt();
        		try {
        			data = IndexLoader.deserialize();
	        		status = STATUS_READY;
	        		checkObsolete();
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_SUCCESS, 0, 0, null);    			
        		} catch (NoSDCardException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_NOSDCARD, 0, 0, null);
        		} catch (FileNotFoundException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_FAILED, 0, 0, null);
				} catch (DeserializingException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_FAILED, 0, 0, null);
				} catch (IOException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_FAILED, 0, 0, null);
				} catch (ClassNotFoundException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_FAILED, 0, 0, null);
				} catch (DifferentVersionException e) {
        			status = STATUS_FAILED;
	        		sendHandlerMessage(MESSAGE_DESERIALIZING_DIFFERENT_VERSION, 0, 0, null);
				} 
            }
        }).start();
	}

	/**
	 * 向消息处理器发送一条消息。
	 * @param what	消息类型
	 * @param arg1	消息参数1 (依消息类型而定)
	 * @param arg2	消息参数2 (依消息类型而定)
	 * @param obj	消息附加对象 (依消息类型而定)
	 */
	private static void sendHandlerMessage(int what, int arg1, int arg2, Object obj) {
		if (null != sEventHandler) {
			Message msg = Message.obtain();
			msg.what = what;
			msg.arg1 = arg1;
			msg.arg2 = arg2;
			msg.obj = obj;
			sEventHandler.sendMessage(msg);
		}
	}
		
	/**
	 * 获取指定索引条目的文件名。
	 * @return	文件名
	 */
	public static String getName(int i) { return data.name[i]; }
	/**
	 * 获取指定索引条目文件名的首字母表示。若没有为首字母建立索引，函数返回空字符串。
	 * @return	文件名的首字母表示
	 */
	public static String getNameAlpha(int i) { return data.nameAlpha[i]; }//Unicode2Alpha.toAlpha(data.name[i]); }
	/**
	 * 获取指定索引条目文件所在的路径。
	 * @return	路径字符串
	 */
	public static String getPath(int i) { return data.path[i]; }
	/**
	 * 获取指定索引条目文件所在路径的首字母表示。若没有为首字母建立索引，函数返回 null。
	 * @return	路径字符串的首字母表示
	 */
	public static String getPathAlpha(int i) { return data.pathAlpha[i]; }//Unicode2Alpha.toAlpha(data.path[i]); }
	/**
	 * 获取指定索引条目文件的上次修改时间，以长整型表示的自 1970年1月1日 算起的毫秒值。
	 * @return	文件的上次修改时间
	 */
	public static long getTime(int i) { return data.time[i]; }
	/**
	 * 获取指定索引条目文件以字节为单位表示的长度。
	 * @return	文件长度
	 */
	public static long getSize(int i) { return data.size[i]; }
	
	/***
	 * 获取指定索引条目文件的缩略图。如果缩略图已缓存，则直接返回缩略图；否则返回代表载入中的图像，并启动异步请求获取缩略图。当异步请求完成后向消息处理器发送 UPDATE_THUMBNAIL 消息，通知缩略图已更新。
	 * @return 文件缩略图位图
	 */
	public static Bitmap getThumbnail(int i) {
		return thumbUtil.get(data.path[i] + "/" + data.name[i]);
	}
	
	/***
	 * 获取指定索引条目文件的摘要信息。如果摘要信息已缓存，则直接返回信息；否则返回代表载入中的文字，并启动异步请求获取文件摘要。当异步请求完成后向消息处理器发送 RELOAD_UPDATE_DIGEST 消息，通知文件摘要已更新。
	 * @return 文件摘要字符串
	 */
	public static Spanned getDigest(int i) {
		return digestUtil.get(data.path[i] + "/" + data.name[i]);
	}

	/**
	 * 物理删除指定索引条目的文件。
	 * @param i	 索引编号
	 * @return 文件是否成功删除
	 */
	public static boolean delete(int i) {
		File file = new File(data.path[i], data.name[i]);
		boolean result = file.delete();
		if (result == true) {
			data.name[i] = null;
		}
		return result;
	}
}
