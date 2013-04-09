package uw.cse.mag.appliancereader.cv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import uw.cse.mag.appliancereader.cv.TransformationBuilder.TransformationStateListener;
import uw.cse.mag.appliancereader.cv.TransformationLibrary.MATCH_PRUNING_METHOD;
import android.util.Log;

public class TransformationBuilderWrapper implements TransformationStateListener {
	private final String TAG = TransformationBuilderWrapper.class.getSimpleName();
	TransformationBuilder builder;

	private boolean active;
	private Mat currentHomography;

	public TransformationBuilderWrapper(ComputerVision initiatedInstance) {
		builder = new TransformationBuilder(initiatedInstance);
		builder.setFeatureDetector(TransformationLibrary.ORB);
		builder.setDescriptorExtractor(TransformationLibrary.BRIEF);
		builder.setHomograhyMethod(TransformationLibrary.RANSAC);
		builder.setRansacThreshhold(6);
		builder.setGreyScale(true);
		builder.setHistogramEqualize(true);
		builder.setMatchPruningMethod(MATCH_PRUNING_METHOD.CROSS_MATCH);
		active= false;
		currentHomography = null;
		builder.setTransformationStateListener(this);
	}

	public void setReferenceImage(Mat image, Rect bounds) {
		Mat otherImg;
		if (bounds != null && bounds.x >= 0 && bounds.y > 0 
				&& bounds.width + bounds.x <= image.cols() && 
				bounds.height+bounds.y <= image.rows())
			otherImg = image.submat(bounds);
		else 
			otherImg = image;

		builder.setOtherImage(otherImg);
	}

	/**
	 * Meant to be called continuosly and if the wrapper is not busy 
	 * it will reset the image sparking another homography
	 * @param image
	 */
	public void setOtherImage(Mat image){
		if (!active){
			builder.setReferenceImage(image);
			active = true;
		}
	}

	/**
	 * @return current homogrpahy or null if one does not exist
	 */
	public Mat getCurrentHomography(){
		return currentHomography;}

	@Override
	public void OnHomographyStored(TransformInfo storage) {
		Log.d(TAG,"Able to find Homography!");
		currentHomography = storage.getHomographyMatrix();
		active = false;
	}

	@Override
	public void OnNoHomographyFound() {
		Log.w(TAG,"Unable to find Homography");
		active = false;
	}

	@Override
	public void OnKeypointsFoundForReference(Mat image) {
		// Nothing to do
	}

	@Override
	public void OnKeypointsFoundForOther(Mat image) {
	}
} 
