package uw.cse.mag.appliancereader.cv.async;

import org.opencv.core.Mat;

import uw.cse.mag.appliancereader.cv.ComputerVision;

public class AsyncFeatureDrawer extends AsyncPerspectiveUtility {

	private OnFeaturesDrawnListener mListener;
	
	public AsyncFeatureDrawer(ComputerVision cv, ImageInformation refImg) {
		super(cv, refImg);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Sets listener to list for when features are drawn
	 * @param list
	 */
	public void setFeatureDrawnListener(OnFeaturesDrawnListener list){
		mListener = list;
	}

	@Override
	protected Mat doInBackground(Mat... params) {
		super.doInBackground(params);

		// Target mat with keypoints drawn on it
		Mat target_with_keypoints = new Mat();

		// Draw the keypoints and output the new mat
		mCV.drawKeypoints_RGBA(mTgtImg, target_with_keypoints, mTgtKeyPts);

		return target_with_keypoints;
	} 
	
	@Override 
	protected void onPostExecute(Mat image){
		// Send the processed image with key points 
		if (image != null && mListener != null)
			mListener.onFeaturesDrawn(image);
	}

	
	public interface OnFeaturesDrawnListener {
		public void onFeaturesDrawn(Mat imgWFeat);
	}
}
