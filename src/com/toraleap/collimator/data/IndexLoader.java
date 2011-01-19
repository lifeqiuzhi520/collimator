package com.toraleap.collimator.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;

import com.toraleap.collimator.data.IndexData.DifferentVersionException;
import com.toraleap.collimator.util.FileInfo;
import com.toraleap.collimator.util.Unicode2Alpha;

/**
 * 包含文件索引的重建、序列化及反序列化的方法。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1025
 */
final class IndexLoader {
	
	// 首选项参数
	private boolean isIndexHidden = false;
	private boolean isIndexSystem = false;
	private boolean isIndexDotPrefix = false;
	private boolean isIndexFirstLetter = true;
	private boolean isIndexAllType = false;

	private final HashSet<String> blackList = new HashSet<String>();
	
	public IndexLoader(SharedPreferences prefs) {
		isIndexHidden = prefs.getBoolean("index_hidden", false);
		isIndexSystem = prefs.getBoolean("index_system", false);
		isIndexDotPrefix = prefs.getBoolean("index_dotprefix", false);
		isIndexFirstLetter = prefs.getBoolean("index_firstletter", true);
		isIndexAllType = prefs.getBoolean("index_alltype", false);
		getBlacklist();
	}
	
	/**
	 * 根据构造函数获取的设置，扫描SD卡，重建文件索引。
	 * @return 建立的索引对象
	 * @throws NoSDCardException 未检测到挂载的SD卡
	 */
	public IndexData reload() throws NoSDCardException {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new NoSDCardException();
		}
		// 准备初始栈
		Stack<String> stack = new Stack<String>();
		stack.push(Environment.getExternalStorageDirectory().getPath());
		// 准备缓存列表
		IndexData data = new IndexData();
		ArrayList<String> lName = new ArrayList<String>();
		ArrayList<String> lPath = new ArrayList<String>();
		ArrayList<String> lNameAlpha = new ArrayList<String>();
		ArrayList<String> lPathAlpha = new ArrayList<String>();
		ArrayList<Long> lSize = new ArrayList<Long>();
		ArrayList<Long> lTime = new ArrayList<Long>();
		// 开始文件遍历
		while (!stack.isEmpty()) {
			String parent = stack.pop();
			String parentAlpha = null;
			if (isIndexFirstLetter) {
				parentAlpha = Unicode2Alpha.toAlpha(parent);
			}
			File path = new File(parent);
			File[] files = path.listFiles();
			if (null == files) continue;
			for (File f : files)
			{
				if (f.isDirectory()) {
					if (isQualifiedDirectory(f)) stack.push(f.getPath());
				}
				else {
					if (isQualifiedFile(f)) {
						lName.add(f.getName());
						lPath.add(parent);
						lTime.add(f.lastModified());
						lSize.add(f.length());
						if (isIndexFirstLetter) {
							lNameAlpha.add(Unicode2Alpha.toAlpha(f.getName()));
							lPathAlpha.add(parentAlpha);
						} else {
							lNameAlpha.add("");
							lPathAlpha.add("");
						}
					}
				}
			}
		}
		int length = lName.size();
		data.indexTime = System.currentTimeMillis();
		data.availableSpace = getAvailableSpace();
		data.name = lName.toArray(new String[length]);
		data.path = lPath.toArray(new String[length]);
		data.nameAlpha = lNameAlpha.toArray(new String[length]);
		data.pathAlpha = lPathAlpha.toArray(new String[length]);
		data.size = new long[length];
		for (int i = 0; i < length; i++) data.size[i] = lSize.get(i).longValue();
		data.time = new long[length];
		for (int i = 0; i < length; i++) data.time[i] = lTime.get(i).longValue();
		return data;
    }
	
	/**
	 * 序列化索引对象。
	 * @param data	要序列化的对象
	 * @throws SerializingException 序列化失败
	 */
	public static void serialize(IndexData data) throws SerializingException {
		File f;
		DataOutputStream out = null;
		try {
			f = new File(Environment.getExternalStorageDirectory().getPath() + "/.collimator");
			if (!f.exists()) f.mkdirs();
			f = new File(Environment.getExternalStorageDirectory().getPath() + "/.collimator/index.dat");
			if (f.exists()) f.delete();
			out = new DataOutputStream(new FileOutputStream(f));
			data.write(out);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SerializingException();
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	/**
	 * 反序列化索引对象。
	 * @return 若成功，返回反序列化的索引对象；否则返回null。
	 * @throws NoSDCardException 未检测到挂载的SD卡
	 * @throws DeserializingException 索引文件格式不正确
	 * @throws DifferentVersionException 索引文件版本异常
	 * @throws ClassNotFoundException 反序列化异常
	 * @throws IOException 索引文件读取异常
	 */
	public static IndexData deserialize() throws NoSDCardException, DeserializingException, IOException, ClassNotFoundException, DifferentVersionException {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new NoSDCardException();
		}
		File f = new File(Environment.getExternalStorageDirectory().getPath() + "/.collimator/index.dat");
		if (!f.exists()) {
			throw new FileNotFoundException();
		}
		DataInputStream in = null;
		FileInputStream file = null;
		try {
			file = new FileInputStream(f.getPath());
			in = new DataInputStream(file);
			IndexData data = new IndexData();
			data.read(in);
			return data;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (file != null)
				try {
					file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	/**
	 * 准备系统黑名单列表。
	 */
	private void getBlacklist() {
		String root = Environment.getExternalStorageDirectory().getPath();
		blackList.add(root + "/lost.dir");
		blackList.add(root + "/android");
		blackList.add(root + "/brut.googlemaps");		
		blackList.add(root + "/navione");		
		blackList.add(root + "/picstore");		
	}
	
	/**
	 * 判断一个文件夹是否应该压入待索引栈。
	 * @param file	指向文件夹的文件对象
	 * @return 是否应该压栈
	 */
	private boolean isQualifiedDirectory(File file) {
		if (file.getName().equals(".") || file.getName().equals("..")) return false;
		if (!isIndexDotPrefix && file.getName().startsWith(".")) return false;
		if (!isIndexHidden && file.isHidden()) return false;
		if (!isIndexSystem && blackList.contains(file.getPath().toLowerCase())) return false;
		if (new File(file.getPath(), ".nomedia").exists()) return false;
		return true;
	}
	
	/**
	 * 判断一个文件是否应该进行索引。
	 * @param file	指向文件的文件对象
	 * @return 是否应该索引
	 */
	private boolean isQualifiedFile(File file) {
		if (!isIndexDotPrefix && file.getName().startsWith(".")) return false;
		if (!isIndexHidden && file.isHidden()) return false;
		if (!isIndexAllType && FileInfo.mimeType(file.getName()).equals("*.*")) return false;
		return true;
	}
	
	/**
	 * 获取外部存储上的可用空间大小。
	 * @return 可用空间的字节数
	 */
	private static long getAvailableSpace() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			long indexSize = 0;
			File indexFile = new File(Environment.getExternalStorageDirectory().getPath() + "/.collimator/index.dat");
			if (indexFile.exists()) indexSize = indexFile.length();
			StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
			return stat.getBlockSize() * stat.getAvailableBlocks() + indexSize;
		}
		return 0;
	}
	
	/**
	 * 根据剩余空间判断索引是否已过期。
	 * @param data	文件索引数据结构
	 * @return 是否已过期
	 */
	public static boolean neededReload(IndexData data) {
    	if (Math.abs(getAvailableSpace() - data.availableSpace) >= 1024L * 50)  return true;
    	return false;
	}
	
	@SuppressWarnings("serial")
	public static class NoSDCardException extends Exception {
		public NoSDCardException() {
			super("No SDCard was found.");
		}
		public NoSDCardException(String detailMessage) {
			super(detailMessage);
		}
	}
	
	@SuppressWarnings("serial")
	public static class DeserializingException extends Exception {
		public DeserializingException() {
			super("Deserializing failed.");
		}
		public DeserializingException(String detailMessage) {
			super(detailMessage);
		}
	}
	
	@SuppressWarnings("serial")
	public static class SerializingException extends Exception {
		public SerializingException() {
			super("Serializing failed.");
		}
		public SerializingException(String detailMessage) {
			super(detailMessage);
		}
	}
}
