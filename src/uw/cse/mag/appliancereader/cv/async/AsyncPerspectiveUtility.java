package uw.cse.mag.appliancereader.cv.async;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

import uw.cse.mag.appliancereader.cv.CVSingletons;
import uw.cse.mag.appliancereader.cv.ComputerVision;
import android.os.AsyncTask;
import android.util.Log;

/**
 * A complete end to 
 * @author mhotan
 */
public abstract class AsyncPerspectiveUtility extends AsyncTask<Mat, Object, Mat> {

	private final static String TAG = AsyncPerspectiveUtility.class.getSimpleName();
	
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
	protected final Mat mRefImg;
	
	/**
	 * Matrix of keypoints for the reference image
	 */
	protected final MatOfKeyPoint mRefKeyPts; 
	
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
	 * Boolean tracker to see if this task is currently working
	 */
	private boolean isWorking;
	
	/**
	 * TRack amount of time each evaluation takes
	 */
	private long mDuration;
	
	/**
	 * Constructor where target the homography that will map target to reference
	 * <b> Such that target * h = reference
	 * @param cv
	 * @param reference
	 */
	protected AsyncPerspectiveUtility(ComputerVision cv, ImageInformation refImg){
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
			
		isWorking = false;
	}
	
	/**
	 * @return whether this task is currentyl active or processing a homography
	 */
	public boolean isActive(){
		return isWorking;
	}
	
	/**
	 * 
	 * @return
	 */
	public long getDurationMilliSeconds(){
		return mDuration;
	}
	
	@Override
	protected void onPreExecute(){
		isWorking = true;
		mDuration = System.currentTimeMillis();
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
		
		mHomography = mCV.findHomography( tgt2f, ref2f, 
				CVSingletons.getHomographyMethod(), CVSingletons.getRansacThreshold());
	
		return mHomography;
	}
	
	@Override 
	protected void onPostExecute(Mat homography){
		isWorking = false;
		mDuration = System.currentTimeMillis() - mDuration;
	}

	
	

}
