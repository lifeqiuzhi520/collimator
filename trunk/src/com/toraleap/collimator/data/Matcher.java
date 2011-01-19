package com.toraleap.collimator.data;

import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import com.toraleap.collimator.util.FileInfo;

/**
 * 每个 Matcher 对象表示单个匹配判断条件。Matcher 类不可被外部实例化，请使用本类的静态方法进行匹配。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1023
 */
public final class Matcher {
	
	private static final int MESSAGE_FIRST = 0;
	public static final int MATCHER_START = MESSAGE_FIRST + 1;
	public static final int MATCHER_ENTRY = MESSAGE_FIRST + 2;
	public static final int MATCHER_FINISHED = MESSAGE_FIRST + 3;
	public static final int MATCHER_NODATA = MESSAGE_FIRST + 4;
	public static final int MATCHER_SYNTAX_ERROR = MESSAGE_FIRST + 5;
	private static final int MATCHER_TYPE_NAME = 1;
	private static final int MATCHER_TYPE_FOLDER = 2;
	private static final int MATCHER_TYPE_SIZELT = 3;
	private static final int MATCHER_TYPE_SIZEGT = 4;
	private static final int MATCHER_TYPE_DATEDURING = 5;
	private static final int MATCHER_TYPE_MIMETYPE = 6;
	
	private static Thread sThread;
	private static Handler sHandler;
	private static boolean isRegex = false;
	private static boolean isFuzzy = true;
	
	private Pattern mPattern;
	private long mSeparator;
	private int mType = MATCHER_TYPE_NAME;
	private boolean isReverse = false;
	private int mStart;
	private int mEnd;
	
	public Matcher(String regex) throws Exception {
		if (regex.startsWith("!")) {
			isReverse = true;
			regex = regex.substring(1, regex.length());
		}
		if (regex.startsWith("/") || regex.startsWith("\\")) {
			mType = MATCHER_TYPE_FOLDER;
			mPattern = toRegex(regex.substring(1, regex.length()));
		} else if (regex.endsWith("/") || regex.endsWith("\\")) {
			mType = MATCHER_TYPE_FOLDER;
			mPattern = toRegex(regex.substring(0, regex.length() - 1));
		} else if (regex.startsWith("<")) {
			mType = MATCHER_TYPE_SIZELT;
			mSeparator = FileInfo.stringToSize(regex.substring(1, regex.length()));
		} else if (regex.startsWith(">")) {
			mType = MATCHER_TYPE_SIZEGT;
			mSeparator = FileInfo.stringToSize(regex.substring(1, regex.length()));
		} else if (regex.startsWith(":")) {
			mType = MATCHER_TYPE_DATEDURING;
			mSeparator = FileInfo.timespanToMillis(regex.substring(1, regex.length()));
		} else if (regex.startsWith("mimetype:")) {
			mType = MATCHER_TYPE_MIMETYPE;
			mPattern = toRegex(regex.substring(9, regex.length()));
		} else if (regex.startsWith("mt:")) {
			mType = MATCHER_TYPE_MIMETYPE;
			mPattern = toRegex(regex.substring(3, regex.length()));
		} else { 
			mType = MATCHER_TYPE_NAME;
			mPattern = toRegex(regex);
		}
	}

	/**
	 * 依据匹配条件类型对测试索引条目进行匹配。
	 * @param i		正在测试的索引条目
	 * @return 是否成功匹配
	 */
	private boolean match(int i) {
		java.util.regex.Matcher matcher;
		switch (mType) {
		case MATCHER_TYPE_NAME:
			matcher = mPattern.matcher(Index.getName(i));
			if (matcher.find()) {
				mStart = matcher.start();
				mEnd = matcher.end();
				return !isReverse;
			} else if (null != Index.getNameAlpha(i)) {
				matcher = mPattern.matcher(Index.getNameAlpha(i));
				if (matcher.find()) {
					mStart = matcher.start();
					mEnd = matcher.end();
					return !isReverse;
				}
			}
			break;
		case MATCHER_TYPE_FOLDER:
			matcher = mPattern.matcher(Index.getPath(i));
			if (matcher.find()) {
				return !isReverse;
			} else if (null != Index.getPath(i)) {
				matcher = mPattern.matcher(Index.getPathAlpha(i));
				if (matcher.find()) {
					return !isReverse;
				}
			}
			break;
		case MATCHER_TYPE_SIZELT:
			if (Index.getSize(i) < mSeparator) return !isReverse;
			break;
		case MATCHER_TYPE_SIZEGT:
			if (Index.getSize(i) > mSeparator) return !isReverse;
			break;
		case MATCHER_TYPE_DATEDURING:
			if (Index.getTime(i) > System.currentTimeMillis() - mSeparator) return !isReverse;
			break;
		case MATCHER_TYPE_MIMETYPE:
			matcher = mPattern.matcher(FileInfo.mimeType(Index.getName(i)));
			if (matcher.find()) {
				return !isReverse;
			}
			break;
		}
		return isReverse;
	}
	
	/**
	 * 获取此匹配结果的起始位置。
	 * @return	起始位置索引
	 */
	public int start() { return mStart; }
	/**
	 * 获取此匹结果配的结束位置。
	 * @return 结束位置索引
	 */
	public int end() { return mEnd; }
	/**
	 * 获取此匹配器的类型。
	 * @return 匹配器类型
	 */
	public int type() { return mType; }
	/**
	 * 获取此匹配器是否经过取反运算。
	 * @return 是否取反
	 */
	public boolean isReverse() { return isReverse; }
	
	/**
	 * 使用给定的表达式在文件索引中进行异步匹配，期间产生的任何消息都将发送到消息处理器。如果已有一次匹配过程在进行中，将取消先前的匹配过程，然后启动新的匹配。
	 * @param expression	匹配表达式
	 */
	public static void matchAsync(final Matcher[] matchers) {
		stopAsyncMatch();
		sThread = new Thread(new Runnable() {
        	public void run() {
        		MatchThread(matchers);
            }
        });
		sThread.start();
	}
	
	/**
	 * 如果异步匹配正在进行，发出终止信号并等待其终止。
	 */
	public static void stopAsyncMatch() {
		if (null != sThread && sThread.isAlive()) {
			sThread.interrupt();
			try {
				sThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 由 matchAsync 调用的匹配异步线程。匹配过程中产生的任何消息都将发送到注册的消息处理器。
	 * @param expression	匹配表达式
	 */
	private static void MatchThread(Matcher[] matchers) {
		if (null == matchers) {
			sHandler.sendEmptyMessage(MATCHER_SYNTAX_ERROR);
		} else {
			sHandler.sendEmptyMessage(MATCHER_START);
			LABEL_NEXTENTRY:
			for (int i = 0; i < Index.length(); i++) {
				if (sThread.isInterrupted()) return;
				if (Index.getName(i) == null) continue LABEL_NEXTENTRY;
				for (Matcher m : matchers) {
					if (!m.match(i)) continue LABEL_NEXTENTRY;
				}
				sendMatchEntry(i, matchers);
			}
			sHandler.sendEmptyMessage(MATCHER_FINISHED);
		}
	}
	
	/**
	 * 将字符串形式的常规通配符表达或正则表达翻译为正则表达式，并根据首选项进行一些必要的处理。
	 * @param key	输入的表达式
	 * @return	转换后的正则表达式
	 */
	private static Pattern toRegex(String key) {
		Pattern pattern;
		String patternKey;
		if (isRegex || key.startsWith("re:")) {
			if (key.startsWith("re:")) {
				patternKey = key.substring(3, key.length());
			} else {
				patternKey = key;
			}				
			if (!isFuzzy && !patternKey.startsWith("^")) {
				pattern = Pattern.compile("^" + patternKey, Pattern.CASE_INSENSITIVE);
			} else {
				pattern = Pattern.compile(patternKey, Pattern.CASE_INSENSITIVE);
			}
		} else {
			patternKey = key
				.replace("\\", "\\u005C")
				.replace(".", "\\u002E")
				.replace("$", "\\u0024")
				.replace("^", "\\u005E")
				.replace("{", "\\u007B")
				.replace("[", "\\u005B")
				.replace("(", "\\u0028")
				//.replace("|", "\\u007C")
				.replace(")", "\\u0029")
				.replace("+", "\\u002B")
				.replace("*", "[\\s\\S]*")
				.replace("?", "[\\s\\S]");
			if (isFuzzy) {
				pattern = Pattern.compile(patternKey, Pattern.CASE_INSENSITIVE);
			} else {
				pattern = Pattern.compile("^" + patternKey, Pattern.CASE_INSENSITIVE);
			}
		}
		return pattern;
	}
	
	/**
	 * 将一个成功的匹配项包装后发送给消息处理器。
	 * @param entry		匹配成功的 Entry 实例
	 * @param matchers	进行此次匹配的 Matcher 数组
	 */
	private static void sendMatchEntry(int index, Matcher[] matchers) {
		Match match = new Match(index);
		for (Matcher matcher : matchers) {
			if (matcher.type() == Matcher.MATCHER_TYPE_NAME && matcher.isReverse == false) match.setHilite(matcher.start(), matcher.end());
		}
		Message msg = Message.obtain();
		msg.what = MATCHER_ENTRY;
		msg.obj = match;
		sHandler.sendMessage(msg);
	}
	
	/**
	 * 初始化匹配条件与程序的关联。主程序改变首选项后应再次调用此函数。
	 * @param prefs		程序的首选项对象，从这里获得索引设置
	 * @param handler	主线程的消息处理器，匹配线程产生的相关消息将发往此消息处理器
	 */
	public static void init(SharedPreferences prefs, Handler handler) {
		isRegex = prefs.getBoolean("matching_regex", false);
		isFuzzy = prefs.getBoolean("matching_fuzzy", true);
		sHandler = handler;
	}
	
}
