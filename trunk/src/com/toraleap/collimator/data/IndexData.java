package com.toraleap.collimator.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.toraleap.collimator.util.Unicode2Alpha;

/**
 * 存储文件索引及相关信息的数据结构，实现索引的序列化及反序列化。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1025
 */
final class IndexData {
	
	private static final int version = 1;
	String[] name;
	String[] path;
	String[] nameAlpha;
	String[] pathAlpha;
	long[] size;
	long[] time;
	long indexTime;
	long availableSpace;
	
	int length() {
		if (name == null) {
			return 0;
		} else {
			return name.length;
		}
	}
	
	void read(DataInputStream in) throws IOException, ClassNotFoundException, DifferentVersionException {
		String lastPath = null;
		int ver = in.readInt();
		if (ver != version) throw new DifferentVersionException();
		indexTime = in.readLong();
		availableSpace = in.readLong();
		int count = in.readInt();
		name = new String[count];
		path = new String[count];
		nameAlpha = new String[count];
		pathAlpha = new String[count];
		size = new long[count];
		time = new long[count];
		for (int i = 0; i < count; i++) {
			name[i] = in.readUTF();
			if (in.readBoolean() == false) {
				path[i] = in.readUTF();
				lastPath = path[i];
			} else {
				path[i] = lastPath;
			}
			nameAlpha[i] = Unicode2Alpha.toAlpha(name[i]);
			pathAlpha[i] = Unicode2Alpha.toAlpha(path[i]);
			size[i] = in.readLong();
			time[i] = in.readLong();
		}
	}
	
	void write(DataOutputStream out) throws IOException {
		String lastPath = null;
		out.writeInt(version);
		out.writeLong(indexTime);
		out.writeLong(availableSpace);
		out.writeInt(name.length);
		for (int i = 0; i < name.length; i++) {
			out.writeUTF(name[i]);
			if (path[i] == lastPath) {
				out.writeBoolean(true);
			} else {
				out.writeBoolean(false);
				out.writeUTF(path[i]);
				lastPath = path[i];
			}
			out.writeLong(size[i]);
			out.writeLong(time[i]);
		}
	}
	
	@SuppressWarnings("serial")
	public class DifferentVersionException extends Exception {
		public DifferentVersionException() {
			super("Different version detected.");
		}
		public DifferentVersionException(String detailMessage) {
			super(detailMessage);
		}
	}
}
