package uw.cse.mag.appliancereader.cv.async;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

import uw.cse.mag.appliancereader.cv.CVSingletons;
import uw.cse.mag.appliancereader.cv.ComputerVision;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Draws boxes of where the applicaitons thinks the target dsiplays of the refrence imag
 * @author mhotan
 *
 */
public final class AsyncBoxDrawer extends AsyncTask<Mat, Void, Mat> {

	private static final String TAG = AsyncBoxDrawer.class.getSimpleName();

	private final List<List<Point>> mfeat2Draw;
	private final List<WarpedPointListener> mSecListeners;

	/**
	 * Computer Vision instance to do all the dirty work
	 */
	protected final ComputerVision mCV;


	////////////////////////////////////////////////////////////////////////
	//// Reference Image values
	////////////////////////////////////////////////////////////////////////

	/**
	 * Reference image and target image
	 */
	private final Mat mRefImg;

	/**
	 * Matrix of keypoints for the reference image
	 */
	final MatOfKeyPoint mRefKeyPts; 

	/**
	 * Descriptor for the reference image
	 */
	protected final Mat mRefDescriptors;

	////////////////////////////////////////////////////////////////////////
	//// Target values
	////////////////////////////////////////////////////////////////////////

	/**
	 * Matrix representation for the target image 
	 */
	protected Mat mTgtImg;

	/**
	 * Matrix of key points 
	 */
	protected MatOfKeyPoint mTgtKeyPts;

	/**
	 * Matrix of target descriptor feature extraction
	 */
	protected Mat mTgtDescriptors;

	/**
	 * Mat of D Matches of this
	 */
	protected MatOfDMatch mMatDMatches;

	/**
	 * Homography that is found between two images
	 */
	protected Mat mHomography;

	/**
	 * Feature Detector to for both reference and target image
	 */
	private final FeatureDetector mFeatureDetector;

	/**
	 * A Feature descriptor used for each set of feature detected
	 */
	private final DescriptorExtractor mDescriptorExtractor;

	/**
	 * 
	 * @param cv
	 * @param incomingFrame
	 * @param savedFrame 
	 * @param features list of features to be drawn
	 */
	public AsyncBoxDrawer(ComputerVision cv, ImageInformation refImg, List<List<Point>> displayFeatures) {
		if (cv == null || !cv.isInitialized())
			throw new RuntimeException("Illegal Computer Vision: " + cv);
		if (refImg == null)
			throw new RuntimeException("");
		mCV = cv;
		mFeatureDetector = CVSingletons.getFeatureDetector();
		mDescriptorExtractor = CVSingletons.getDescriptorExtractor();

		// Establish the reference image
		mRefImg = refImg.mImage;
		mRefKeyPts = refImg.mFeatureKeyPts;
		mRefDescriptors = refImg.mFeatureDescriptors;
	
		mfeat2Draw = new ArrayList<List<Point>>();
		for (List<Point> feature: displayFeatures){
			List<Point> nlst = new ArrayList<Point>();
			for (Point p: feature){
				nlst.add(p.clone());
			}
			mfeat2Draw.add(nlst);
		}
		
		mSecListeners = new ArrayList<AsyncBoxDrawer.WarpedPointListener>();
	}

	/**
	 * Add listener to list for complete image
	 * @param l listener to add
	 */
	public void addListener(WarpedPointListener l){
		if (l != null)
			mSecListeners.add(l);
	}
	
	/**
	 * Removes listener if it exists
	 * @param l
	 */
	public void removeListener(WarpedPointListener l){
		mSecListeners.remove(l);
	}

	@Override
	protected Mat doInBackground(Mat... params) {
		// Given a new target image
		mTgtImg = params[0];

		// Compute target key points
		mTgtKeyPts = mCV.findFeatures(mFeatureDetector, mTgtImg);

		// Compute target descriptors
		mTgtDescriptors = mCV.computeDescriptors(mDescriptorExtractor, mTgtImg, mTgtKeyPts);

		// Have to check if we did not get black image
		if (mTgtKeyPts.empty()) {
			Log.d(TAG, TAG+ ": No features");
			return null;
		}

		// Get putative matches
		mMatDMatches = mCV.getMatchingCorrespondences(mTgtDescriptors, mRefDescriptors);

		// Get points for homography calculation
		MatOfPoint2f[] pts = mCV.getCorrespondences(mMatDMatches, mRefKeyPts, mTgtKeyPts);

		MatOfPoint2f tgt2f = pts[1];
		MatOfPoint2f ref2f = pts[0];

		// Find the transformation form reference to tgt
		mHomography = mCV.findHomography( ref2f, tgt2f, 
				CVSingletons.getHomographyMethod(), CVSingletons.getRansacThreshold());

		List<List<Point>> warpedFeatures = new ArrayList<List<Point>>(mfeat2Draw.size());
		for (List<Point> l: mfeat2Draw)
			warpedFeatures.add(mCV.getWarpedPoints(l, mHomography));
	
		Mat toDraw = mTgtImg.clone();
		for (List<Point> l: warpedFeatures){
			for (int i = 0; i < l.size()-1; ++i)
				mCV.drawLine(l.get(i), l.get(i+1), toDraw);
			mCV.drawLine(l.get(l.size()-1), l.get(0), toDraw);
		}

		return toDraw;
	}
	
	@Override
	protected void onPostExecute(Mat completeImage){
		if (completeImage != null)
		for (WarpedPointListener l: mSecListeners)
			l.onBoxDrawn(completeImage);
	}

	public interface WarpedPointListener {
		public void onBoxDrawn(Mat image);
	}

}
