package uw.cse.mag.appliancereader.cv;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;

import uw.cse.mag.appliancereader.cv.TransformationLibrary.MATCH_PRUNING_METHOD;
import uw.cse.mag.appliancereader.cv.TransformationLibrary.PruningMethodParameters;
import uw.cse.mag.appliancereader.imgproc.ImageConversion;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

/**
 * Class that is able to build a homography transformation between two images
 * 
 * @author mhotan
 */
public class TransformationBuilder {
	private static final String TAG = "HomographyBuilder";

	// Corresponding references to images for homography
	private enum IMAGE {REFERENCE_IMAGE , VARIABLE_IMAGE};

	// Threshold values for finding homography
	public static final int RANSAC_THRESHHOLD_MAX = 10;
	private static final Pair<Integer, Integer> RANSAC_RANGE = 
			new Pair<Integer, Integer>(1, RANSAC_THRESHHOLD_MAX);

	// Computer vision object to
	private ComputerVision mCV;

	// Asyncronous homography producer 
	private AsyncHomographyProcessor homographyProcesser;
	private AsyncFeatureDetector mRefFeatureDetector;
	private AsyncFeatureDetector mOtherFeatureDetector;

	//Listeners that listens for state changes 
	private TransformationStateListener mlistener;

	// Storage of Tranformation information
	TransformInfo storage;

	///////////////////////////////////////////////////////////////////
	// Vairables for build process

	private Mat mReferenceImage, mOtherImage;

	// Feature and Homography Tools
	private int mHomographyType = -1;
	private int mRansacThreshhold; // If threshold 
	private String mFeatDetectorName;
	private String mDescExtractorName;

	// Pruning Methods
	private MATCH_PRUNING_METHOD mPruningMethod;
	private PruningMethodParameters mPruningParameters;

	// Parameters to set if the client wants to
	// Convert images to grey scale
	private boolean mUseGreyScale;
	// Use Histogram Equalization
	private boolean mUseHistogramEqualization;

	///////////////////////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////////////////////

	/**
	 * Sets all the initial values of Homography types 
	 * Ransac threshold is defaulted to ten
	 * @param cv fully initialized Computer Vision instance
	 */
	public TransformationBuilder(ComputerVision cv){
		if (cv == null)
			throw new IllegalArgumentException("ComputerVision not instantiated");
		// Instantiated vision
		mCV = cv;

		// Storage of all types of variables
		storage = new TransformInfo();

		// Initialize features 
		setHomograhyMethod(TransformationLibrary.RANSAC);
		setRansacThreshhold(3);
		setFeatureDetector(TransformationLibrary.ORB);
		setDescriptorExtractor(TransformationLibrary.BRIEF);
		setMatchPruningMethod(MATCH_PRUNING_METHOD.NONE);
		setMatchPruningParamters(new PruningMethodParameters());
		setGreyScale(true); // Use grey scale images as default
		setHistogramEqualize(true); // Use histogram equalization 
		Log.i(TAG, "Transformation builder is set");
		checkRep();
	}

	private void checkRep(){
		if (mFeatDetectorName == null)
			throw new RuntimeException("Feature Detector Name is not valid");
		if (mDescExtractorName == null)
			throw new RuntimeException("Feature descriptor extractor Name is not valid");
		if (mHomographyType == -1)
			throw new RuntimeException("Homography type is not valid");
	}

	///////////////////////////////////////////////////////////////////
	// Accessors
	///////////////////////////////////////////////////////////////////

	/**
	 * Returns complete copy of current Parameters setting. <b> If any values of these
	 * parameters are altered it will NOT effect the current bulding procedure.
	 * To place the change use setMAtchPruningParameters();
	 * <b>NOTE: To restart the build process run setMatchPruningMethod
	 */
	public PruningMethodParameters getMatchPruningParameters(){
		try {
			return (PruningMethodParameters) mPruningParameters.clone();
		} catch (CloneNotSupportedException e) {
			Log.e(TAG, "PruningMethodParameters Clone not implemented properly");
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Returns the current pruning method of this Tranformation builder
	 * <b> this determines the default  
	 * @return current pruning method
	 */
	public MATCH_PRUNING_METHOD getCurrentMatchingPruningMethod(){
		return mPruningMethod;
	}

	/**
	 * Returns the name of current Feature Detector
	 * @return non null name of 
	 */
	public String getCurrentFeatureDetector(){
		return mFeatDetectorName;
	}

	/**
	 * Returns the name of the current Feature Descriptor Extractor
	 * @return non null name
	 */
	public String getCurrentDescriptorExtractor(){
		return mDescExtractorName;
	}

	/**
	 * Returns the name of the current Homography name
	 * @return name (not null)
	 */
	public String getCurrentHomographyType(){
		for (String name : TransformationLibrary.getSupportedHomographyMethods()) {
			if (mHomographyType == TransformationLibrary.getHomographyIdentifier(name))
				return name;
		}
		throw new RuntimeException("Unable to find the current name of the homography");
	}

	/**
	 * Creates a new Feature Detector
	 * @return the current Feature Detector that is assigned by set feature Detector
	 */
	public FeatureDetector getFeatureDetector() {
		return TransformationLibrary.getFeatureDetector(mFeatDetectorName);
	}

	/**
	 * Creates a new Descriptor Extractor
	 * @return the current Descriptor Extracter that is assign by setDescripterExtractor
	 */
	public DescriptorExtractor getDescriptorExtractor(){
		return TransformationLibrary.getDescriptorExtractor(mDescExtractorName);
	}

	///////////////////////////////////////////////////////////////////
	// Mutator setting tools, homography types, and listeners
	///////////////////////////////////////////////////////////////////

	/**
	 * Use the grey scale images instead of full color images
	 * @param useGreyScale true if grey scale wants to used, or false otherwise
	 */
	public void setGreyScale(boolean useGreyScale) {
		mUseGreyScale = useGreyScale;
		attemptToRefindFeature();
	}

	/**
	 * Use histogram equalization to normalize pictures of different intensities
	 * <b> REFERENCE: http://docs.opencv.org/doc/tutorials/imgproc/histograms/histogram_equalization/histogram_equalization.html
	 * @param histEqualize true if histogram equalization is desired, false no equalization
	 * occurs
	 */
	public void setHistogramEqualize(boolean histEqualize) {
		mUseGreyScale = true;
		mUseHistogramEqualization = histEqualize;
		attemptToRefindFeature();
	}

	/**
	 * Does nto attempt to rebuild after completion
	 * @param params parameters to be set to prune out methods
	 */
	public void setMatchPruningParamters(PruningMethodParameters params) {
		if (params == null || mPruningParameters == params) return;
		mPruningParameters = params;
	}

	/**
	 * Sets the pruning method of this tranformation builder
	 */
	public void setMatchPruningMethod(MATCH_PRUNING_METHOD method){
		if (method == null || method == mPruningMethod) return;
		mPruningMethod = method;
		attemptToFindHomography();
	}

	/**
	 * Sets the homography method to use
	 * @requires method != null and method is one specified in TranformationLibrary
	 * @param method name of method
	 */
	public void setHomograhyMethod(String method){
		if (method == null) return;
		mHomographyType = TransformationLibrary.getHomographyIdentifier(method);
		attemptToFindHomography(); 
	}

	/**
	 * Sets the threshold to input value if the value falls in between 
	 * 1 < max threshold value		
	 * @param threshhold requested threshold for ransac
	 */
	public void setRansacThreshhold(int threshhold){
		mRansacThreshhold = Math.max(RANSAC_RANGE.first, //It is at least min value
				Math.min(RANSAC_RANGE.second, threshhold)); // atmost max value
		Log.i(TAG, "Ransac threshhold set: " + mRansacThreshhold);
		attemptToFindHomography();
	}

	/**
	 * Sets the Feature Detector to use for feature detecting
	 * @requires detectorName != null and medetectorNamethod is one specified in TranformationLibrary
	 * @param detectorName detector to use
	 */
	public void setFeatureDetector(String detectorName) {
		if (detectorName == null) return;
		mFeatDetectorName = detectorName; 
		attemptToRefindFeature();
	}

	/**
	 * Sets the Descriptor Extractor to use for feature detecting 
	 * @requires ExtratorName != null and ExtratorName is one specified in TranformationLibrary
	 * @param ExtratorName name of extractor to use
	 */
	public void setDescriptorExtractor(String ExtratorName) {
		if (ExtratorName == null) return;
		mDescExtractorName = ExtratorName;
		attemptToRefindFeature();
	}

	/**
	 * Set the listener to be notified when Homography is complete
	 * @param listener
	 */
	public void setTransformationStateListener(TransformationStateListener listener){
		mlistener = listener;
		updateListeners(storage);
	}

	///////////////////////////////////////////////////////////////////
	// Image Selection

	/**
	 * sets and preprocesses reference images finding key points
	 * @param image Bitmap image to be reference
	 */
	public void setReferenceImage(Mat image){
		setImagePrivate(image, IMAGE.REFERENCE_IMAGE);
	}

	/**
	 * sets and preprocesses other image finding key points
	 * @param image Bitmap image to be other
	 */
	public void setOtherImage(Mat image){
		setImagePrivate(image, IMAGE.VARIABLE_IMAGE);
	}

	/**
	 * Note that Large BitMap will cause out of memory errors
	 * This will not do anything on a null Image
	 * @param image 
	 * @param which
	 */
	private void setImagePrivate(Mat image, IMAGE which){
		if (image == null) {
			Log.d(TAG, "Null parameter passed in to setImagePrivate");
			return;
		}

		// cancel any asynchronous process before we starrt a new one
		switch (which){
		case REFERENCE_IMAGE:
			// Clear out old value of the reference image
			storage.setReferenceImage(null, null, null);
			mReferenceImage = image;
			// Cancel any feature finding thread
			if (mRefFeatureDetector != null){
				mRefFeatureDetector.cancel(true);
				mRefFeatureDetector = null;
			} 
			// Start new feature detector
			mRefFeatureDetector = new AsyncFeatureDetector(mReferenceImage, IMAGE.REFERENCE_IMAGE);
			mRefFeatureDetector.execute();
			break;
		case VARIABLE_IMAGE:
			storage.setOtherImage(null, null, null);
			mOtherImage = image;
			// Cancel any feature finding thread
			if (mOtherFeatureDetector != null){
				mOtherFeatureDetector.cancel(true);
				mOtherFeatureDetector = null;
			} 
			// Start new feature detector
			mOtherFeatureDetector = new AsyncFeatureDetector(mOtherImage, IMAGE.VARIABLE_IMAGE);
			mOtherFeatureDetector.execute();
			break;			
		}
	}

	/**
	 * Should be called after a change to a feature detection 
	 * parameter.  Restarts teh feature finding process
	 */
	private void attemptToRefindFeature(){
		// This will update the images 
		setImagePrivate(mReferenceImage, IMAGE.REFERENCE_IMAGE);
		setImagePrivate(mOtherImage, IMAGE.VARIABLE_IMAGE);
	}

	/**
	 * calls asynchronous method to process Images
	 */
	private void attemptToFindHomography(){
		if (mHomographyType == -1 || !storage.hasBothImages()){
			if (mlistener != null)
				mlistener.OnNoHomographyFound();
			return; // Cant build with thos values
		} else {
			//Cancel any current running processes
			if (homographyProcesser != null){
				homographyProcesser.cancel(false);
				homographyProcesser = null;
			}
			homographyProcesser = new AsyncHomographyProcessor(storage);
			homographyProcesser.execute();
		}
	}

	/**
	 * Warps input based on stored homography
	 * @param input
	 * @return
	 */
	public Mat getWarpedImage(Mat input) {
		if (!storage.isComplete()) return null;
		return mCV.getWarpedImage(input, storage.getHomographyMatrix(),new Size(input.width(), input.height()), false);
	}

	/**
	 * Attempts to build transformation and returns in paor
//	 * pair.first = regular inversion
//	 * pair.secod = inverse ivnersion 
//	 * @return null if couldnt build or Data other wise
//	 */
//	public Bitmap getWarpedImages(){
//		if (!storage.isComplete()) return null;
//
//		// Check if storage has a complete homography
//		Mat homography = storage.getHomographyMatrix() ;
//		// Do a transformation with non inverted map
//		Mat src = storage.getOtherMatrix();
//		Mat result = mCV.getWarpedImage(src, homography, false);
//		return ImageConversion.matToBitmap(result);
//	}
	
	public Mat getLastWarpedImage(){
		if (!storage.isComplete()) return null;
		Size s = new Size(storage.getOtherMatrix().width(), storage.getOtherMatrix().height());
		Mat result = mCV.getWarpedImage(storage.getOtherMatrix(), storage.getHomographyMatrix(),s, false);
		return result;
	}

	///////////////////////////////////////////////////////////////////
	// Image Processing

	/**
	 * Runs feature detection in the background for specific image
	 * Pair<MatOfKeyPoint, Mat> first is keypoints found for this image
	 * second are the image descriptors
	 * @author mhotan
	 */
	private class AsyncFeatureDetector extends AsyncTask<Void, Void, Pair<MatOfKeyPoint, Mat>>{

		private IMAGE mWhichImg;
		private Mat mImg;
		private final boolean mUseGrey;
		private final boolean mEqualizeHistogram;
		private final FeatureDetector mFeatDetector;
		private final DescriptorExtractor mDescriptorExtractor;

		public AsyncFeatureDetector(Mat img, IMAGE whichImg){
			// Create new instances of thesse object to be run in background thread
			mImg = img.clone();
			mWhichImg = whichImg;
			this.mUseGrey = mUseGreyScale;
			this.mEqualizeHistogram = mUseHistogramEqualization;
			this.mFeatDetector = getFeatureDetector();
			this.mDescriptorExtractor = getDescriptorExtractor();
		}

		/**
		 * Finds Key Points in new image
		 * @return MatofKeyPoints found and Mat of descriptors found
		 */
		@Override
		protected Pair<MatOfKeyPoint, Mat> doInBackground(Void... params) {
			// Calling the following Computer Vision methods 
			// from the helper class locks on the helper class itself
			// There is no following synchronization required

			Mat source = mImg;
			if (this.mUseGrey) { // Convert to greyscale image
				source = mCV.RGBToGrey(source);
			}
			if (this.mEqualizeHistogram) { // Use Histogram equalization
				// this normalizes light intensity to create a new image
				source = mCV.toEqualizedHistogram(source);
			}
			
			// Get the general feature keypoints
			MatOfKeyPoint matKeyPoints = mCV.findFeatures(this.mFeatDetector, source);
			Log.d(TAG, "Features found for image: " + mWhichImg + 
					" key Points: " + matKeyPoints.toArray());

			// Compute the descriptions for the feature key points
			Mat descriptors = mCV.computeDescriptors(this.mDescriptorExtractor,
					source, matKeyPoints);
			Log.d(TAG, "Feature descriptions found for image: " + mWhichImg
					+ " descriptors: " + descriptors.toString());

			if (matKeyPoints.empty()){
				Log.w(TAG, "No Keypoints found");
				return null;
			}
			if (descriptors.empty()){
				Log.w(TAG, "No Descriptors found");
				return null;
			}
			
			Pair<MatOfKeyPoint, Mat> p = new Pair<MatOfKeyPoint, Mat>(matKeyPoints, descriptors); 
			return p;
		}

		//Runs on main thread
		@Override
		protected void onPostExecute(Pair<MatOfKeyPoint, Mat> result){
			//Somethine
			
			if (result != null && mWhichImg == IMAGE.REFERENCE_IMAGE){
				// Save all the progress that has been made for reference image
				storage.setReferenceImage(mImg, result.first, result.second);

				// Notify listener completed feature detectiona and description 
				if (mlistener != null){
					mlistener.OnKeypointsFoundForReference(
							mCV.getMatWithKeyPointsDrawn(storage.getReferenceMatrix(),
									storage.getReferenceKeyPoints()));
				}
			} else if (result != null && mWhichImg == IMAGE.VARIABLE_IMAGE) {
				// Save all the progress that has been made for variable image
				storage.setOtherImage(mImg, result.first, result.second);
				if (mlistener != null)
					mlistener.OnKeypointsFoundForOther(
							mCV.getMatWithKeyPointsDrawn(storage.getOtherMatrix(),
									storage.getOtherKeyPoints()));
			}

			// because image changed must attempt to build again
			attemptToFindHomography();
		}

	}


	/**
	 * Helper class to calculate find the homography between the reference and secondary image
	 * stored in temp storage
	 * 
	 * @author mhotan
	 */
	private class AsyncHomographyProcessor extends AsyncTask<Void, Void,Boolean>{

		private final TransformInfo tempStorage;
		private final int tranformMethod, threshhold;
		

		/**
		 * Creates a new task to run
		 * @param info stores all the pertinent information needed to find homography
		 */
		public AsyncHomographyProcessor(TransformInfo info){
			if (!info.hasBothImages())
				throw new IllegalStateException("Tranform info does not information " +
						"on both images");

			//Create copies or use immutable objects
			tempStorage = info.clone();
			tranformMethod = mHomographyType;
			threshhold = mRansacThreshhold;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
//			publishProgress();

			// Assertion is that storage has all necessary data
			
			// Let Matrix s be a matrix that represents the source image
			// Let Matrix d be a matrix that represents the destination image
			// Let Matrix h be a matrix that represents the Homography matrix
			
			// We will attempt to find an h such that
			// for a si, s(xi, yi, 1) * h = d(xi, yi, 1)
			
			// Assign the non reference image as the src matrix
			// This way we will find an homography that could transform to look like
			// the reference
			MatOfKeyPoint srcKeyPtMat = tempStorage.getOtherKeyPoints();
			Mat sourceDescriptor = tempStorage.getOtherDescriptors();
			
			// Use the reference image as the destination because it is our target
			MatOfKeyPoint destKeyPtMat = tempStorage.getReferenceKeyPoints();
			Mat destDescriptor = tempStorage.getReferenceDescriptors();
			
			// Get potential matches in both directions for 
			// cross checking (matching validation) purposes
			MatOfDMatch matches1to2 = mCV.getMatchingCorrespondences(
					sourceDescriptor, destDescriptor);
			MatOfDMatch matches2to1 = mCV.getMatchingCorrespondences(
					destDescriptor, sourceDescriptor);

			// Create Arrays for ease of use
			KeyPoint[] srcKeyPts = srcKeyPtMat.toArray();
			KeyPoint[] destKeyPts = destKeyPtMat.toArray();

			int imgHeight = tempStorage.getReferenceMatrix().rows();
			int imgWidth = tempStorage.getReferenceMatrix().cols();

			MatOfDMatch good_matches;
			// Cross matches are used by multiple images
			MatOfDMatch crossMatches = mCV.getCrossMatches(matches1to2, matches2to1);
			// Prune out the good matches
			switch (mPruningMethod) {
			case CROSS_MATCH:
				good_matches = crossMatches;
				break;
			case KNNMATCH:
				List<MatOfDMatch> knnMatches = mCV.getKnnMatchList(destDescriptor, 
						sourceDescriptor, mPruningParameters.getKValue());
				good_matches = mCV.getDistanceMatches(knnMatches, srcKeyPtMat, 
						destKeyPtMat, mPruningParameters.getKValue(), 
						mPruningParameters.getDistanceThreshhold());
				break;
			case LOCAL_MATCH:
				good_matches = mCV.getLocalMatches(crossMatches, srcKeyPtMat, destKeyPtMat, 
						mPruningParameters.getNumZones(), imgHeight, imgWidth);
				break;
			case KNN_AND_CROSSCHECK: 
				good_matches = mCV.getKnnWithCrossCheckingMatches(sourceDescriptor,
						destDescriptor, mPruningParameters.getKValue());
				break;
//			case STANDARD_DISTANCE:
//				good_matches = mCV.getStandardDistanceCheck(matches1to2);
//				break;
			default:
				// Default is to use every value
				good_matches = matches1to2;
			}
			// Store the good matches
			tempStorage.setPutativeMatches(good_matches);

			// Find Correspondences
			DMatch[] goodMatchArray = good_matches.toArray();
			List<Point> srcPts = new ArrayList<Point>(goodMatchArray.length);
			List<Point> dstPts = new ArrayList<Point>(goodMatchArray.length);
			for (int i = 0; i < goodMatchArray.length; ++i) {
				srcPts.add(srcKeyPts[goodMatchArray[i].queryIdx].pt);
				dstPts.add(destKeyPts[goodMatchArray[i].trainIdx].pt);
			}
			
			MatOfPoint2f src = new MatOfPoint2f();
			MatOfPoint2f dest = new MatOfPoint2f();
			
			src.fromList(srcPts);
			dest.fromList(dstPts);

			// Find the homography mapping that maps the source to destination
			Mat H = mCV.findHomography(src, dest, tranformMethod, threshhold);
			if (H == null)
				return Boolean.FALSE;

			// Store Homography
			tempStorage.setHomographyMatrix(H);
			return Boolean.TRUE;
		}

		protected void onProgressUpdate(Void... progress) {
			// Notifies listener homography is still processing 
			updateListeners(null);
		}

		@Override 
		protected void onPostExecute(Boolean result){
			// On successful completion 
			// Store the new tranformation data into storage
			// update the listener
			if (result.booleanValue()){
				storage = tempStorage;
			} 
			updateListeners(storage);
		}

	}

	/**
	 * Update the listener when the Homography is ready to be used
	 * @param storage
	 */
	private void updateListeners(TransformInfo storage){
		if (mlistener != null){
			if (storage != null && storage.isComplete())
				mlistener.OnHomographyStored(storage);
			else
				mlistener.OnNoHomographyFound();
		}
	}

	/**
	 * General Interface to handle Callback for transformation
	 * @author mhotan
	 */
	public interface TransformationStateListener {
		public void OnHomographyStored(TransformInfo storage);
		public void OnNoHomographyFound();
		public void OnKeypointsFoundForReference(Mat image);
		public void OnKeypointsFoundForOther(Mat image);
	}

}
