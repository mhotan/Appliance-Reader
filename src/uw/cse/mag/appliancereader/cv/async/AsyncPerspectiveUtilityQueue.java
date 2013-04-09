package uw.cse.mag.appliancereader.cv.async;

import java.util.HashSet;
import java.util.Set;

import org.opencv.core.Mat;

import android.content.Context;

public class AsyncPerspectiveUtilityQueue implements PerspectiveListener {

	public static final int MAX_SIZE = 5;
	private static final Object mLock = new Object();
	
	/**
	 * Q of all running processes
	 */
	private final Set<AsyncPerspectiveUtility> mQ;
	private final Context mCtx;
	
	public AsyncPerspectiveUtilityQueue(Context ctx){
		mCtx = ctx;
		mQ = new HashSet<AsyncPerspectiveUtility>();
	}
	
	public void enqueue(AsyncPerspectiveUtility task){
		if (task == null) return;
		if (mQ.size() == MAX_SIZE) return;
		mQ.add(task);
//		task.addListener(this);
		task.execute();
	}

	public void shutDown(){
		for (AsyncPerspectiveUtility task: mQ){
			task.cancel(true);
		}
	}

	@Override
	public void onUpdate(AsyncPerspectiveUtility task, Mat m) {
		mQ.remove(task);
	}
}
