package com.toraleap.collimator.util;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 代表一个软引用缓存器。派生类应重写 request 方法获取关键字对应的结果。
 * @author		uestc.Mobius <mobius@toraleap.com>
 * @version	2010.1104
 *
 * @param <K>	关键字的类型
 * @param <V>	结果的类型
 */
public abstract class SoftCache<K, V> {

	private static final int MESSAGE_FIRST = 200;
	public static final int MESSAGE_CACHE_GOT = MESSAGE_FIRST + 1;
	public static final int MESSAGE_QUEUE_FINISHED = MESSAGE_FIRST + 2;
	
    private final ConcurrentHashMap<K, SoftReference<V>> cache = new ConcurrentHashMap<K, SoftReference<V>>();
    private final LinkedBlockingQueue<K> queue = new LinkedBlockingQueue<K>();
    private final SoftReference<V> loadingHolder = new SoftReference<V>(null);
    private final Handler callback;
    private final Thread thread = new Thread() {
		public void run() {
			K key;
			while (true) {
				try {
					key = queue.take();
			    	requestAndCache(key);
			    	if (!isInterrupted()) {
				    	sendHandlerMessage(MESSAGE_CACHE_GOT, 0, 0, key);
				    	if (queue.size() == 0) sendHandlerMessage(MESSAGE_QUEUE_FINISHED, 0, 0, null);
			    	} else {
			    		clearQueue();
			    	}
				} catch (InterruptedException e) {
					e.printStackTrace();
					clearQueue();
				}
			}
		}
	};

	/**
	 * 构造一个新的软引用缓存器。
	 * @param callback	异步请求返回时的回调 Handler
	 */
    public SoftCache(Handler callback) {
    	this.callback = callback;
    	thread.setDaemon(true);
    	thread.start();
    }
    
    /**
     * 向缓存请求一个关键字对应的结果。若结果不在缓存中或已不可用，返回 默认值 并将结果置入请求队列；否则从缓存中查找结果并返回。每个缓存请求完成后都会向注册的 Handler 发送 MESSAGE_CACHE_GOT 消息，整个请求队列完成后将会向注册的 Handler 发送 MESSAGE_QUEUE_FINISHED 消息。返回的默认值可通过重写 getDefault 方法改变。
     * @param key	请求的关键字
     * @return 关键字对应的结果或默认值
     */
    public V get(K key) {
    	SoftReference<V> ref = cache.get(key);
    	// 不在缓存中，这是一次新访问
    	if (ref == null) {
    		offerRequest(key);
    		return getDefault();
    	}
    	// 正在读取队列中
    	if (ref == loadingHolder) {
    		return getDefault();
    	}
    	V value = ref.get();
    	// 曾经缓存，但是已经被回收
    	if (value == null) {
    		offerRequest(key);
    		return getDefault();
    	}
    	// 目标存在于缓存中
    	return value;
    }
    
    /**
     * 尝试中断队列中未完成的请求。当前执行中的请求完成后不会向 Handler 产生 MESSAGE_CACHE_GOT 消息，同时也不会产生 MESSAGE_QUEUE_FINISHED 消息。
     */
    public void interrupt() {
    	thread.interrupt();
    }
    
    /**
     * 从缓存中获取一个关键字对应的结果。若关键字在缓存中不存在或已被回收，返回 null。此函数应在子线程中调用。
     * @param key	要请求的关键字
     * @return	关键字对应的结果，或 null
     */
    V getCache(K key) {
    	SoftReference<V> ref = cache.get(key);
    	if (ref == null) return null;
    	return ref.get();
    }

    /**
     * 将给出的键值对缓存起来。此函数应在子线程中调用。
     * @param key	关键字
     * @param value		关键字对应的结果
     * @return 输入的结果参数
     */
    V putCache(K key, V value) {
    	if (value == null) return null;
    	cache.put(key, new SoftReference<V>(value));
    	return value;
    }
    
    /**
     * 请求指定的关键字，并将结果缓存起来。若关键字已在缓存中，直接返回结果。此函数应在子线程中调用。
     * @param key	要请求的关键字
     * @return 请求结果
     */
    V requestAndCache(K key) {
    	V value = getCache(key);
    	// 结果不存在或已经被回收
    	if (value == null) {
        	return putCache(key, request(key));
    	}
    	// 目标存在于缓存中
    	return value;    	
    }
    
    /**
     * 将给定关键字加入请求队列。
     * @param key	要请求的关键字
     */
    private void offerRequest(K key) {
    	cache.put(key, loadingHolder);
    	queue.offer(key);
    	if (getMaxQueueLength() > 0 && queue.size() > getMaxQueueLength()) {
    		cache.remove(queue.remove());
    	}
    }
    
    /**
     * 还原队列中对象的请求状态，然后清空请求队列。
     */
    private void clearQueue() {
    	while (true) {
    		K key = queue.poll();
    		if (key == null) break;
    		cache.remove(key);
    	}
    }
    
	/**
	 * 向消息处理器发送一条消息。
	 * @param what	消息类型
	 * @param arg1	消息参数1 (依消息类型而定)
	 * @param arg2	消息参数2 (依消息类型而定)
	 * @param obj	消息附加对象 (依消息类型而定)
	 */
	private void sendHandlerMessage(int what, int arg1, int arg2, Object obj) {
		if (null != callback) {
			Message msg = Message.obtain();
			msg.what = what;
			msg.arg1 = arg1;
			msg.arg2 = arg2;
			msg.obj = obj;
			callback.sendMessage(msg);
		}
	}
	
	/**
	 * 重写此函数以返回自定义的默认值，而不是默认的 null。
	 * @return 替代 null 的默认值
	 */
	V getDefault() {
		return null;
	}
	
	/**
	 * 重写此函数以决定请求队列的最大长度。若请求队列超出长度，位于队首的会首先被抛弃。默认队列长度为无限(-1)。
	 * @return
	 */
	int getMaxQueueLength() {
		return -1;
	}
		
    /**
     * 子线程主体，子类必须重写此函数，完成获取给出关键字的值并返回。
     * @param key	请求的关键字
     * @return 关键字对应的值
     */
    abstract V request(K key);
}