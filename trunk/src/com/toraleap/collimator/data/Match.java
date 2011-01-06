package com.toraleap.collimator.data;

import android.graphics.Bitmap;
import android.text.Html;
import android.text.Spanned;

import com.toraleap.collimator.util.FileInfo;

/**
 * 每个 Match 对象表示成功的单个匹配项。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 */
public final class Match {
	private final int mIndex;
	private boolean[] mHilite;
	
	/**
	 * 匹配项构造函数。
	 * @param entry		此成功匹配的索引条目
	 */
	public Match(int index) {
		mIndex = index;
		mHilite = new boolean[Index.getName(index).length()];
	}
	
	/**
	 * 设置匹配项文件名的高亮范围。如果有多处高亮范围，可多次调用本函数。
	 * @param start		高亮范围起点
	 * @param end		高亮范围终点
	 */
	public void setHilite(int start, int end) {
		for (int i = start; i < end; i++)
			mHilite[i] = true;
	}
	
	/**
	 * 获取此匹配项对应的索引。
	 * @return 索引
	 */
	public int index() { return mIndex; }
	/**
	 * 获取文件名。
	 * @return	文件名
	 */
	public String name() { return Index.getName(mIndex); }
	/**
	 * 获取文件名的首字母表示。若没有为首字母建立索引，函数返回 null。
	 * @return	文件名的首字母表示
	 */
	public String nameAlpha() { return Index.getNameAlpha(mIndex); }
	/**
	 * 获取文件所在的路径。
	 * @return	路径字符串
	 */
	public String path() { return Index.getPath(mIndex); }
	/**
	 * 获取文件所在路径的首字母表示。若没有为首字母建立索引，函数返回 null。
	 * @return	路径字符串的首字母表示
	 */
	public String pathAlpha() { return Index.getPathAlpha(mIndex); }
	/**
	 * 获取文件以字节为单位表示的文件长度。
	 * @return	文件长度
	 */
	public long size() { return Index.getSize(mIndex); }
	/**
	 * 获取文件长度的智能可读字符串形式。
	 * @return	文件长度的字符串表示
	 */
	public String sizeString() { return FileInfo.sizeString(Index.getSize(mIndex)); }
	/**
	 * 获取文件上次修改时间距 1970-01-01 的毫秒数。
	 * @return	上次修改时间的毫秒数
	 */
	public long time() { return Index.getTime(mIndex); }
	/**
	 * 获取文件上次修改时间距今的本地化字符串表示。
	 * @return	距今时间的本地化表示
	 */
	public String timeString() { return FileInfo.timeSpanString(System.currentTimeMillis() - Index.getTime(mIndex)); }
	/***
	 * 获取文件缩略图。如果缩略图已缓存，则直接返回缩略图；否则返回 null，并启动一个新线程后台获取缩略图。当缩略图成功取得时向消息处理器发送 RELOAD_UPDATE_THUMBNAIL 消息，通知缩略图已更新。
	 * @return 文件缩略图位图
	 */
	public Bitmap thumbnail() { return Index.getThumbnail(mIndex); }
	/***
	 * 获取文件的摘要信息。如果摘要信息已缓存，则直接返回信息；否则返回 null，并启动一个新线程后台获取文件摘要。当文件摘要成功取得时向消息处理器发送 RELOAD_UPDATE_DIGEST 消息，通知文件摘要已更新。
	 * @return 文件摘要字符串
	 */
	public Spanned digest() { return Index.getDigest(mIndex); }
	/***
	 * 获取高亮处理后的文件名显示串。
	 * @return 高亮文本串
	 */
	public Spanned highlightedName() {
		String source = Index.getName(mIndex);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mHilite.length; i++) {
			if (mHilite[i]) {
				sb.append("<font color='#ffff00'>" + source.charAt(i) + "</font>");
			} else {
				sb.append(source.charAt(i));
			}
		}
		return Html.fromHtml(sb.toString());
	}
}