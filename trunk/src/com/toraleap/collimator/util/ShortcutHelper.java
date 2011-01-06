package com.toraleap.collimator.util;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;

/**
 * 包含生成桌面快捷方式的相关工具
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 */
public final class ShortcutHelper {
	
	private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
	private static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";
	
	private Context mContext;
	private String mName;
	private Intent mIntent;
	private ShortcutIconResource mIconResource;
	
	/**
	 * 将给定的 Intent 包装为一个快捷方式辅助类。
	 * @param context	上下文对象
	 * @param intent	目标快捷方式 Intent 对象
	 */
	public ShortcutHelper(Context context, Intent intent) {
		mContext = context;
		mIntent = intent;
	}
	
	/**
	 * 设置快捷方式显示的名称。
	 * @param name	显示名称字符串
	 * @return 当前包装对象
	 */
	public ShortcutHelper setName(String name) {
		mName = name;
		return this;
	}
	
	/**
	 * 设置快捷方式显示的图标资源。
	 * @param iconResource	要显示的图标
	 * @return	当前包装对象
	 */
	public ShortcutHelper setIconResource(ShortcutIconResource iconResource) {
		mIconResource = iconResource;
		return this;
	}
	
	/**
	 * 生成快捷方式到桌面上。
	 * @param duplicate		是否可以有多个快捷方式的副本
	 */
	public void install(boolean duplicate) {
		Intent installIntent = new Intent(ACTION_INSTALL_SHORTCUT);    
	    installIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mName);    
	    installIntent.putExtra(EXTRA_SHORTCUT_DUPLICATE, duplicate);
	    installIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, mIntent);    
	    installIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, mIconResource);
	    mContext.sendBroadcast(installIntent);  	
	}
}
