package uw.cse.mag.appliancereader.cv.async;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import uw.cse.mag.appliancereader.cv.ComputerVision;

public class AsyncImageWarper extends AsyncPerspectiveUtility {

	private ImageWarpListener mListener;
	
	public AsyncImageWarper(ComputerVision cv, ImageInformation refinfo) {
		super(cv, refinfo);
	}
	
	public void setImageWarpedListener(ImageWarpListener listener){
		mListener = listener;
	}

	@Override
	protected Mat doInBackground(Mat... params) {
		Mat H = super.doInBackground(params);
		if (H == null) return null;
		
		Size s = new Size(mTgtImg.width(), mTgtImg.height());
		Mat warped = mCV.getWarpedImage(mTgtImg, H, s, false);
		return warped;
	}
	
	@Override
	protected void onPostExecute(Mat warpedImg){
		if (warpedImg != null && mListener != null)
			mListener.onImageWarped(warpedImg);
	}

	public interface ImageWarpListener {
		public void onImageWarped(Mat warpedImage);
	}
	
}
