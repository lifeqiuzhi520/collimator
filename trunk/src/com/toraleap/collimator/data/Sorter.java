package com.toraleap.collimator.data;

import java.util.Collections;
import java.util.Comparator;

import com.toraleap.collimator.util.FileInfo;

/***
 * 包含针对匹配结果集的静态排序方法。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1105
 */
public final class Sorter {
	
	public static final int SORT_NAME = 0;
	public static final int SORT_DATE = 1;
	public static final int SORT_MIMETYPE = 2;
	public static final int SORT_SIZE = 3;
	public static final int SORT_PARENT = 4;
	public static final int SORT_EXTENSION = 5;
	public static final int SORT_NAME_ALPHA = 6;
	public static final int SORT_PARENT_ALPHA = 7;
	public static final int SORT_NONE = 8;

	private Sorter() { }
	
	/**
	 * 对匹配结果集按指定的排序方式进行排序。
	 * @param list		要排序的匹配结果集
	 * @param sortMode	进行排序的依据
	 * @param reverse	若为 false，进行升序排序，反之则为降序排序
	 */
	public static Comparator<Match> getSorter(int sortMode, boolean reverse) {
		Comparator<Match> comparator = null;
		switch (sortMode) {
		case SORT_NONE:
			break;
		case SORT_DATE:
			comparator = new Comparator<Match>() {
				public int compare(Match match1, Match match2) {
					if (match1.time() > match2.time()) return 1;
					if (match1.time() < match2.time()) return -1;
					return 0;
				}};
			break;
		case SORT_NAME:
			comparator = new Comparator<Match>() {
				public int compare(Match match1, Match match2) {
					return match1.name().compareTo(match2.name());
				}};
			break;
		case SORT_NAME_ALPHA:
			comparator = new Comparator<Match>() {
				public int compare(Match match1, Match match2) {
//					return Unicode2Alpha.toPureAlpha(match1.name()).compareTo(Unicode2Alpha.toPureAlpha(match2.name()));
					return match1.nameAlpha().compareTo(match2.nameAlpha());
				}};
			break;
		case SORT_EXTENSION:
			comparator = new Comparator<Match>() {
				public int compare(Match match1, Match match2) {
					return FileInfo.extension(match1.name()).compareTo(FileInfo.extension(match2.name()));
				}};
			break;
		case SORT_MIMETYPE:
			comparator = new Comparator<Match>() {
				public int compare(Match match1, Match match2) {
					return FileInfo.mimeType(match1.name()).compareTo(FileInfo.mimeType(match2.name()));
				}};
			break;
		case SORT_PARENT:
			comparator = new Comparator<Match>() {
				public int compare(Match match1, Match match2) {
					int res = match1.path().compareTo(match2.path());
					if (res == 0) res = match1.name().compareTo(match2.name());
					return res;
				}};
			break;
		case SORT_PARENT_ALPHA:
			comparator = new Comparator<Match>() {
				public int compare(Match match1, Match match2) {
//					int res = Unicode2Alpha.toPureAlpha(match1.path()).compareTo(Unicode2Alpha.toPureAlpha(match2.path()));
//					if (res == 0) res = Unicode2Alpha.toPureAlpha(match1.path()).compareTo(Unicode2Alpha.toPureAlpha(match2.path()));
					int res = match1.pathAlpha().compareTo(match2.pathAlpha());
					if (res == 0) res = match1.nameAlpha().compareTo(match2.nameAlpha());
					return res;
				}};
			break;
		case SORT_SIZE:
			comparator = new Comparator<Match>() {
				public int compare(Match match1, Match match2) {
					if (match1.size() > match2.size()) return 1;
					if (match1.size() < match2.size()) return -1;
					return 0;
				}};
			break;
		}
		if (comparator != null) {
			if (reverse) {
				comparator = Collections.reverseOrder(comparator);
			}
		}
		return comparator;
	}
}
