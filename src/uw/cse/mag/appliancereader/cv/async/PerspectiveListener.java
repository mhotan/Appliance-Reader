package uw.cse.mag.appliancereader.cv.async;

import org.opencv.core.Mat;

public interface PerspectiveListener {

	/**
	 * Called whenever the matrix is to be updated
	 * @param m new matrix image
	 */
	public void onUpdate(AsyncPerspectiveUtility thread, Mat m);
}
