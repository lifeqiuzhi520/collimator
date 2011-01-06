package com.toraleap.collimator.util;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.toraleap.collimator.R;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 提供与文件信息相关的静态工具函数。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1025
 */
public class FileInfo {

	private static Context sContext;
	
	private FileInfo() { }
	
	/**
	 * 获取给定文件名的主文件名名部分
	 * @param filename	源文件名
	 * @return 源文件名的主文件名名部分(不含路径及扩展名)
	 */
	public static String mainName(String filename) {
		int start = filename.lastIndexOf("/");
		int stop = filename.lastIndexOf(".");
		if (stop < start) stop = filename.length();
		if (start >= 0) {
			return filename.substring(start + 1, stop);
		} else {
			return "";
		}
	}
	
	/**
	 * 获取给定文件名的扩展名部分
	 * @param filename	源文件名
	 * @return 源文件名的扩展名部分(不含小数点)
	 */
	public static String extension(String filename) {
		int start = filename.lastIndexOf("/");
		int stop = filename.lastIndexOf(".");
		if (stop < start || stop >= filename.length() - 1) return "";
		else return filename.substring(stop + 1, filename.length());
	}
	
	/**
	 * 获取给定文件名的 MIME 类型
	 * @param filename	源文件名
	 * @return 源文件名的 MIME 类型
	 */
	public static String mimeType(String filename) {
		String ext = extension(filename);
		String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
		return (mime == null) ? "*.*" : mime;
	}
	
	/**
	 * 获取文件长度的智能可读字符串形式。
	 * @param size	文件字节长度
	 * @return	文件长度的字符串表示
	 */
	public static String sizeString(long size) {
		if (size < 1024)
			return String.format("%d B", size);
		else if (size < 1024 * 1024)
			return String.format("%.2f KB", (double)size / 1024);
		else if (size < 1024 * 1024 * 1024)
			return String.format("%.2f MB", (double)size / (1024 * 1024));
		else if (size < 1024L * 1024 * 1024 * 1024)
			return String.format("%.2f GB", (double)size / (1024 * 1024 * 1024));
		else
			return String.format("%.2f EB", (double)size / (1024L * 1024 * 1024 * 1024));
	}
	
	/**
	 * 从文件长度的字符串形式转换为字节数表示。
	 * @param sizeString	文件长度的字符串表示
	 * @return	文件字节长度
	 * @throws ParseException	给定字符串不是支持的形式，解析失败
	 */
	public static long stringToSize(String sizeString) throws ParseException {
		Pattern pattern = Pattern.compile("(-?\\d+\\.?\\d*)([\\w]{0,2})", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(sizeString);
		if (matcher.matches()) {
			double baseSize = Double.parseDouble(matcher.group(1));
			String unit = matcher.group(2).toLowerCase();
			if (unit.equals("b") || unit.length() == 0) {
				return (long)baseSize;
			} else if (unit.equals("k") || unit.equals("kb")) {
				return (long)(baseSize * 1024);
			} else if (unit.equals("m") || unit.equals("mb")) {
				return (long)(baseSize * (1024 * 1024));
			} else if (unit.equals("g") || unit.equals("gb")) {
				return (long)(baseSize * (1024 * 1024 * 1024));
			} else if (unit.equals("e") || unit.equals("eb")) {
				return (long)(baseSize * (1024L * 1024 * 1024 * 1024));
			}
		}
		throw new ParseException(sizeString, 0);
	}
	
	/**
	 * 获取时间跨度的智能可读字符串形式。需要传入一个非负整数。
	 * @param millisec	毫秒单位的时间跨度，非负整数
	 * @return	时间跨度的字符串表示
	 */
	public static String timeString(long timeMillis) {
		if (timeMillis < 1000)
			return sContext.getString(R.string.util_fileinfo_milliseconds, timeMillis);
		else if (timeMillis < 1000 * 60)
			return sContext.getString(R.string.util_fileinfo_seconds, timeMillis / 1000);
		else if (timeMillis < 1000 * 60 * 60)
			return sContext.getString(R.string.util_fileinfo_minutes, timeMillis / (1000 * 60));
		else if (timeMillis < 1000 * 60 * 60 * 48)
			return sContext.getString(R.string.util_fileinfo_hours, timeMillis / (1000 * 60 * 60));
		else if (timeMillis < 1000L * 60 * 60 * 24 * 60)
			return sContext.getString(R.string.util_fileinfo_days, timeMillis / (1000L * 60 * 60 * 24));
		else if (timeMillis < 1000L * 60 * 60 * 24 * 30 * 12)
			return sContext.getString(R.string.util_fileinfo_months, timeMillis / (1000L * 60 * 60 * 24 * 30));
		else
			return sContext.getString(R.string.util_fileinfo_years, timeMillis / (1000L * 60 * 60 * 24 * 30 * 12));
	}
	
	/**
	 * 获取距今时间的智能可读字符串形式。
	 * @param millisec	毫秒单位的时间跨度
	 * @return	距今时间的字符串表示
	 */
	public static String timeSpanString(long timeMillis) {
		if (timeMillis > 0) {
			return sContext.getString(R.string.util_fileinfo_ago, timeString(timeMillis));
		} else {
			return sContext.getString(R.string.util_fileinfo_hence, timeString(-timeMillis));
		}
	}
	
	/**
	 * 从时间跨度的字符串形式转换为毫秒数表示。
	 * @param sizeString	时间跨度的字符串表示
	 * @return	毫秒数
	 * @throws ParseException	给定字符串不是支持的形式，解析失败
	 */
	public static long timespanToMillis(String timeString) throws ParseException {
		Pattern pattern = Pattern.compile("(-?\\d+\\.?\\d*)([\\w]{0,1})", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(timeString);
		if (matcher.matches()) {
			double baseMillis = Double.parseDouble(matcher.group(1));
			String unit = matcher.group(2).toLowerCase();
			if (unit.equals("d") || unit.length() == 0) {
				return (long)(baseMillis * 1000 * 3600 * 24);
			} else if (unit.equals("h")) {
				return (long)(baseMillis * 1000 * 3600);
			} else if (unit.equals("w")) {
				return (long)(baseMillis * 1000 * 3600 * 24 * 7);
			} else if (unit.equals("m")) {
				return (long)(baseMillis * 1000 * 3600 * 24 * 30);
			} else if (unit.equals("y")) {
				return (long)(baseMillis * 1000 * 3600 * 24 * 360);
			}
		}
		throw new ParseException(timeString, 0);
	}
	
	/**
	 * 初始化文件信息工具函数与程序的关联。主程序改变首选项后应再次调用此函数。
	 * @param prefs			程序的首选项对象，从这里获得索引设置
	 * @param context		程序上下文(建议在 Activity 及其派生类中用 getApplicationContext() 获得)
	 */
	public static void init(SharedPreferences prefs, Context context) {
		sContext = context;
	}
}
