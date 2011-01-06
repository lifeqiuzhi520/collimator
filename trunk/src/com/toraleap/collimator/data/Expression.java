package com.toraleap.collimator.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.toraleap.collimator.R;

/**
 * 代表一个检索式，包含检索范围及排序方式等信息。支持序列化到JSON字符串，以及从JSON字符串恢复。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1015
 */
public final class Expression {
	private Context mContext;
	private int mRange;
	private int mSortMode = Sorter.SORT_DATE;
	private boolean mSortReverse = true;
	private String mKey = "";
	private String mName;
	
	/**
	 * 构造一个空的默认检索式。
	 * @param context	应用程序上下文
	 */
	public Expression(Context context) { 
		mContext = context;
	}
	
	/**
	 * 从一个JSON对象字符串构造检索式。
	 * @param context	应用程序上下文
	 * @param json		JSON对象字符串
	 */
	public Expression(Context context, String json) {
		this(context);
		try {
			JSONObject obj = new JSONObject(json);
			mName = obj.optString("name");
			mRange = obj.optInt("range");
			mSortMode = obj.optInt("sortMode");
			mSortReverse = obj.optBoolean("sortReverse");
			mKey = obj.optString("key");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取此检索式的比较器。
	 * @return 比较器
	 */
	public Comparator<Match> getSorter() {
		return Sorter.getSorter(mSortMode, mSortReverse);
	}
	
	/**
	 * 获取此检索式的排序方式(常数位于Sorter类中)。
	 * @return 排序方式常数
	 */
	public int getSortMode() {
		return mSortMode;
	}
	
	/**
	 * 获取此检索式的排序方向。
	 * @return 是否降序
	 */
	public boolean getSortReverse() {
		return mSortReverse;
	}
	
	/**
	 * 获取此检索式的搜索范围(位于array.xml中)。
	 * @return 搜索范围整数
	 */
	public int getRange() {
		return mRange;
	}
	
	/**
	 * 获取此检索式用户输入的关键字。
	 * @return 关键字字符串
	 */
	public String getKey() {
		return mKey;
	}
	
	/**
	 * 获取此检索式的名称。
	 * @return 名称字符串
	 */
	public String getName() {
		if (mName == null) return mKey;
		return mName;
	}
	
	/**
	 * 设置此检索式的排序方式。
	 * @param sortMode 排序方式(常数位于Sorter类中)
	 * @param reverse	是否降序
	 */
	public void setSort(int sortMode, boolean reverse) {
		mSortMode = sortMode;
		mSortReverse = reverse;
	}
	
	/**
	 * 设置此检索式的排序方式。
	 * @param sortMode 排序方式(常数位于Sorter类中)
	 */
	public void setSort(int sortMode) {
		mSortMode = sortMode;
	}
	
	/**
	 * 设置此检索式的排序方式。
	 * @param reverse	是否降序
	 */
	public void setSort(boolean reverse) {
		mSortReverse = reverse;
	}
	
	/**
	 * 设置此检索式的搜索范围(位于array.xml中)。
	 * @param range		搜索范围整数
	 */
	public void setRange(int range) {
		mRange = range;
	}
	
	/**
	 * 设置此检索式用户输入的关键字。
	 * @param key	关键字字符串
	 */
	public void setKey(String key) {
		mKey = key;
	}
	
	/**
	 * 设置此检索式的名称。
	 * @return 名称字符串
	 */
	public void setName(String name) {
		mName = name;
	}
	
	/**
	 * 使用此检索式进行异步匹配。期间产生的任何消息都将发送到 Matcher 类的消息处理器，因此要截获消息，先调用 Matcher.init 方法注册消息处理器。如果已有一次匹配过程在进行中，将取消先前的匹配过程，然后启动新的匹配。
	 */
	public void matchAsync() {
		Matcher.matchAsync(matchers());
	}
	
	/**
	 * 解析给定的匹配表达式，并试图转换为 Matcher 对象数组。如果表达式有效，将返回解析得到的 Matcher 数组，否则返回 null。
	 * @return 解析得到的 Matcher 数组 或 null
	 */
	public Matcher[] matchers() {
		String[] keys = mKey.split(" ");
		List<Matcher> matchers = new ArrayList<Matcher>(keys.length + 1);
		try {
			if (mRange > 0) {
				matchers.add(new Matcher(mContext.getResources().getStringArray(R.array.dialog_filter_range_entriesvalue)[mRange]));
			}
			for (int i = 0; i < keys.length; i++) {
				matchers.add(new Matcher(keys[i]));
			}
			return matchers.toArray(new Matcher[matchers.size()]);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 将检索式转化为JSON对象字符串表示。
	 * @return JSON对象字符串
	 */
	public String toJSON() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("name", mName);
			obj.put("range", mRange);
			obj.put("sortMode", mSortMode);
			obj.put("sortReverse", mSortReverse);
			obj.put("key", mKey);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj.toString();
	}
}
