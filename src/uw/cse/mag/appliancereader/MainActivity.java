package uw.cse.mag.appliancereader;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import uw.cse.mag.appliancereader.cv.ComputerVision;
import uw.cse.mag.appliancereader.cv.ComputerVisionCallback;
import uw.cse.mag.appliancereader.cv.async.AsyncBoxDrawer;
import uw.cse.mag.appliancereader.cv.async.AsyncBoxDrawer.WarpedPointListener;
import uw.cse.mag.appliancereader.cv.async.AsyncFeatureDetector;
import uw.cse.mag.appliancereader.cv.async.AsyncFeatureDetector.FeatureDetectionListener;
import uw.cse.mag.appliancereader.cv.async.AsyncFeatureDrawer;
import uw.cse.mag.appliancereader.cv.async.AsyncFeatureDrawer.OnFeaturesDrawnListener;
import uw.cse.mag.appliancereader.cv.async.AsyncImageWarper;
import uw.cse.mag.appliancereader.cv.async.AsyncImageWarper.ImageWarpListener;
import uw.cse.mag.appliancereader.cv.async.ImageInformation;
import uw.cse.mag.appliancereader.db.ApplianceNotExistException;
import uw.cse.mag.appliancereader.db.datatype.Appliance;
import uw.cse.mag.appliancereader.db.datatype.ApplianceFeatureFactory;
import uw.cse.mag.appliancereader.db.datatype.ApplianceFeatures;
import uw.cse.mag.appliancereader.imgproc.ImageConversion;
import uw.cse.mag.appliancereader.imgproc.Size;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Initial activity for Appliance Reader application
 * 
 * Activity Objective: Provide user initial ability to start interpretting a s  
 * 
 * @author mhotan
 */
public class MainActivity extends Activity implements ComputerVisionCallback,
CvCameraViewListener, FeatureDetectionListener, WarpedPointListener, 
OnFeaturesDrawnListener, ImageWarpListener, OnItemSelectedListener {

	// Log Tag
	private static final String TAG = MainActivity.class.getSimpleName();

	// Angle to rotate the reference image
	private static final double ROTATE_ANGLE = 90.0;
	private static final double ROTATE_SCALE = 1.0;

	// String representations of storage
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ApplianceReaderSpecific/";
	public static final String APPLIANCES_PATH = DATA_PATH + "Appliances/";

	private static final int SELECT_APPLIANCE = 0x1;
	private static final String SAVED_APPLIANCE = MainActivity.class.getName() + "_SAVED_APPLIANCE";

	/**
	 * This the a main parameter mostly used for debugging to look at
	 * different outputs of opencv calls of the same appliance
	 */
	private DISPLAY_OPTION mCurrentOption;

	/**
	 * This spinner is including for debugging process
	 * This allows the user to see different feature 
	 * outputs of the selected appliance
	 */
	private Spinner mDisplayOptSpinner;

	/**
	 * Computer vision instance that can handle 
	 * all Computer Vision oriented task
	 */
	private ComputerVision mCV;

	/**
	 * This view shows the video source
	 */
	private CameraBridgeViewBase mOpenCvCameraView;

	/**
	 * This appliance provides the display elements to focus on
	 * and the reference image for homography
	 */
	private Appliance mCurrentAppliance;

	/**
	 * Asynchronous calculators
	 * Each one of these does some kind of asynchronous 
	 * homography calculation and returns a matrix to present 
	 * when it is complete
	 */
	private AsyncFeatureDetector mAsyncFD;
	private AsyncBoxDrawer mAsyncBoxer;
	private AsyncFeatureDrawer mAsyncFeatureDrawer;
	private AsyncImageWarper mAsyncImageWarper;

	private Mat mRgba;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Keep the screen on while this activity is running
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_main);
		mOpenCvCameraView = (JavaCameraView) findViewById(R.id.camera_java_surface_view);

		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);

		// Load our default image
		// TODO Make this more robust possible in an exterior class/interface
		// Encapsulate some how to make it portable
		mDisplayOptSpinner = (Spinner) findViewById(R.id.display_option_spinner);
		initializeDisplayOptions(mDisplayOptSpinner);
		// Default option is dont display
		mCurrentOption = getOption(mDisplayOptSpinner.getSelectedItem().toString());
		// Dont enable until ready
		mDisplayOptSpinner.setEnabled(false);

		// Create a computer vision instance to handle all the procedures
		mCV = new ComputerVision(this.getApplicationContext(), this, this);
	}

	@Override
	public void onPause()
	{
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		super.onPause();
	}

	@Override
	public void onResume()
	{
		// Check if we havent just returned from Selecting an appliance
		if (mCurrentAppliance == null) {
			// Attempt to recover last appliance used appliance
			mCurrentAppliance = new AppliancePeferenceSharer(this).getLastAppliance();
			if (mCurrentAppliance == null) {
				// Unable to find last appliance
				Intent i = new Intent(this, UserApplianceSelectionActivity.class);
				startActivityForResult(i, SELECT_APPLIANCE);
			} else {
				Log.i(TAG, "Past Appliance found!");
			}
		}

		super.onResume();

		//Attempts to initialize the service 
		// and then will signal the callback
		mCV.initializeService();
	}

	@Override
	public void onStop(){
		super.onStop();
		new AppliancePeferenceSharer(this).saveAppliance(mCurrentAppliance);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView(); 
		//		mDatabaseAdapter.close();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is
		// killed and restarted.
		if (mCurrentAppliance != null)
			savedInstanceState.putBundle(SAVED_APPLIANCE, mCurrentAppliance.toBundle());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.
		Bundle a = savedInstanceState.getBundle(SAVED_APPLIANCE);
		if (a != null){
			mCurrentAppliance = Appliance.toAppliance(a);
		} else
			Log.w(TAG, "Unable to load previously used appliance");
	}

	//////////////////////////////////////////////////////
	//// ComputerVision Callback methods
	//////////////////////////////////////////////////////


	@Override
	public void onInitServiceFinished() {
		// This signifies it is ok to enable the opencv camera view
		if (mCurrentAppliance != null)
			mOpenCvCameraView.enableView();
	}

	@Override
	public void onInitServiceFailed() {
		// TODO Notify User through TTS and/or Toast that the application 
		// will not work
		Log.e(TAG, "Notifying user Open CV failed to initialize");
	}

	//////////////////////////////////////////////////////
	//// CameraViewBase callback methods for interpreting 
	//////////////////////////////////////////////////////

	/**
	 * 
	 * @param original
	 * @param af
	 * @return
	 */
	private Mat rotate(Mat original, ApplianceFeatures af){
		// Rotate referencing website http://stackoverflow.com/questions/12852578/image-rotation-with-opencv-in-android-cuts-off-the-edges-of-an-image
		// Obtain the width and height of the rotated image
		double radians = Math.toRadians(ROTATE_ANGLE);
		double sin = Math.abs(Math.sin(radians));
		double cos = Math.abs(Math.cos(radians));

		org.opencv.core.Size initialSize = original.size();

		int newWidth = (int) (original.width() * cos + original.height() * sin);
		int newHeight = (int)(original.width() * sin + original.height() * cos);

		// Create a new size box newWidth and newHeight
		int pivotX = newWidth / 2;
		int pivotY = newHeight / 2;

		org.opencv.core.Point center = new org.opencv.core.Point(pivotX, pivotY);
		org.opencv.core.Size targetSize = new org.opencv.core.Size(newWidth, newHeight);
		Log.d(TAG, "Center found x:" + center.x + " y:"+center.y);
		// Create a new Mat with the corrext type
		Mat targetMat = new Mat(targetSize, original.type());

		// obtain the rotation matrix
		Mat rotateMatrix = Imgproc.getRotationMatrix2D(center, ROTATE_ANGLE, 1.0);

		Imgproc.warpAffine(original, targetMat, rotateMatrix, targetSize);

		if (af != null)
			af.rotateAround(rotateMatrix);

		return targetMat;
	}

	/**
	 * 
	 * @param actualDim
	 * @return
	 */
	private Mat setupv2(org.opencv.core.Size actualDim){
		// Mat constructors 

		Size original = mCurrentAppliance.getSizeOfRefImage();
		boolean needsRotation = original.width > original.height 
				&& actualDim.width <= actualDim.height || original.width <= original.height & actualDim.width > actualDim.height;
				// Creates a scale down image
				Bitmap ref;
				// If the orientations dont match
				// swap the size dimensions
				if (needsRotation) {
					ref = mCurrentAppliance.getReferenceImage(
							new org.opencv.core.Size(actualDim.height, actualDim.width));
				} else
					ref = mCurrentAppliance.getReferenceImage(actualDim);
				int width = ref.getWidth();
				int height = ref.getHeight();
				Log.d(TAG, "Reference width: " + width + " height: " + height);

				// Get the scale factor and reduce the feature point appropiately
				float scaleFactor = (float)ref.getWidth() / (float)original.width;
				mCurrentAppliance.scaleFeatures(scaleFactor);

				// Creates a new frame to output this 
				Mat refMat = ImageConversion.bitmapToMat(ref);
				logImg("Reference after scale", refMat);

				// Check if we need to rotate the image
				if (needsRotation){ // Rotate by 90
					refMat = rotate(refMat, mCurrentAppliance.getApplianceFeatures());
				}
				logImg("Reference after scale and rotation", refMat);

				return refMat;
	}

	@Deprecated
	private Mat setupv1(org.opencv.core.Size actualDim){
		Mat refImg = mCurrentAppliance.getReferenceImageMat();
		logImg("Original reference", refImg);

		// Create a place holder for this application
		// TODO check whether these dimensions are correct
		// Mat are constructed be column and rows
		mRgba = new Mat((int)actualDim.height, (int)actualDim.width, refImg.type());
		logImg("Initial camera frame", mRgba);

		// NOTE: Open CV camera bridge view does not support 
		// Portrait orientation.  I had to go into the Open CV Source
		// Code and change CameraBridgeViewBase and change it.
		// However its only the appearance on the screen that changes.
		// The image is actually always being taken in "Landscape mode"
		// As a current attempt to work around this issue.
		// We will check if our reference image is in portrait mode
		// If it is we will rotate the matrix 

		// If in portrait mode
		// Rotate the image so that the orientation matches the video input
		Mat targetMat = null;
		if (refImg.width() < refImg.height()) {
			targetMat = rotate(refImg, mCurrentAppliance.getApplianceFeatures());
			// We have to rotate the image appropiately
			Log.d(TAG, "Rotated features: " + mCurrentAppliance.getApplianceFeatures().toString());

		}
		logImg("Reference image after rotation", targetMat);

		// Now we must scale the reference image to the video output
		// Noe both orientations are the same
		// Scale down the image and the features of the image
		Mat finalImg = new Mat(actualDim, targetMat.type());
		Imgproc.resize(targetMat, finalImg, actualDim);

		// IF the original image is larger then the input frame 
		// then the scale factor will be < 1
		float scaleFactor = (float)finalImg.width() / (float) refImg.width();

		logImg("Initial image after rotation and scaling", finalImg);

		mCurrentAppliance.scaleFeatures(scaleFactor);
		return finalImg;
	}

	@Override
	public void onCameraViewStarted(int w, int h) {
		// TODO adjust the reference image with respect to the width and height that the camera is starting at
		// If camera in portrait mode the h is the actual width and the w is the actual height
		// If camera in landscape mode the w => actual width and h=> actual height
		// Upload the base image with the correct dimension as specified

		// get camera orientaion
		// This orientation check is because OPENCV for android cant handle orientation changes
		// IE think everything is in landscape
		//		int actualOrientation = getResources().getConfiguration().orientation;
		//		if (actualOrientation == Configuration.ORIENTATION_PORTRAIT) {
		//			int temp = w;
		//			w = h;
		//			h = temp;
		//		} 

		//		Reference the initial Appliance 
		//  Grab its reference image
		if (mCurrentAppliance == null) {
			Log.w(TAG, "onCameraViewStarted, Appliance should not be null");

			return;
		}

		org.opencv.core.Size actualDimension = new org.opencv.core.Size(w,h);

		Mat finalImg = setupv2(actualDimension);

		if (DEBUG){
			for (Rect r: mCurrentAppliance.getApplianceFeatures().getFeatureBoxes()){
				mCV.drawRect(r, finalImg);
			}
		}

		mRgba = new Mat(actualDimension, finalImg.type());
		Imgproc.resize(finalImg, mRgba, actualDimension);
		logImg("Final reference Image", mRgba);

		Mat copy = mRgba.clone();
		// Initialize the feature extraction for the reference Image
		mAsyncFD = new AsyncFeatureDetector(mCV);
		mAsyncFD.setFeatureDetectionListener(this);
		// DEBUG Dont execute intial
		mAsyncFD.execute(copy);

		//		b.recycle(); // Free up the memory as soon as possible
		Log.d(TAG, "OnCameraStartComplete");
	}

	private static void logImg(String prefix, Mat img){
		if (img == null) return;

		if (img.type() == CvType.CV_8UC4)
			Log.d(TAG, prefix + " w:" + img.width() + " h:" + img.height() + "Image type 8UC4");
		else if (img.type() == CvType.CV_8UC3)
			Log.d(TAG, prefix + " w:" + img.width() + " h:" + img.height() + "Image type 8UC3");
		else if (img.type() == CvType.CV_8UC1)
			Log.d(TAG, prefix + " w:" + img.width() + " h:" + img.height() + "Image type 8UC1");
		else
			Log.d(TAG, prefix + " w:" + img.width() + " h:" + img.height() + "Image type unknown");
	}

	/**
	 * Holds the reference image information
	 * 
	 */
	private ImageInformation mRefImgInfo = null; 

	private static final boolean DEBUG = false;

	@Override
	public Mat onCameraFrame(Mat inputFrame) {
		logImg("Input frame", inputFrame);
		if (DEBUG)
			return mRgba;

		if (mCurrentAppliance == null || mRgba == null) {
			//			return inputFrame;
			return mRgba;
		}

		// Set this image as new Other image
		Mat workImage = new Mat();
		inputFrame.copyTo(workImage);
		logImg("Input working copy", workImage);

		if (mRefImgInfo != null){ // The reference image features are finished being computed
			switch (mCurrentOption){
			case BOX:
				if (mAsyncBoxer == null && mCurrentAppliance.hasApplianceFeatures()){
					ApplianceFeatures feats = mCurrentAppliance.getApplianceFeatures();
					mAsyncBoxer = new AsyncBoxDrawer(mCV, mRefImgInfo, feats);
					mAsyncBoxer.addListener(this);
					mAsyncBoxer.execute(workImage);
				}
				break;
			case FEATURES:
				if (mAsyncFeatureDrawer == null){
					mAsyncFeatureDrawer = new AsyncFeatureDrawer(mCV, mRefImgInfo);
					mAsyncFeatureDrawer.setFeatureDrawnListener(this);
					mAsyncFeatureDrawer.execute(workImage);
				}
				break;
			case WARP_IMG:
				if (mAsyncImageWarper == null){
					mAsyncImageWarper = new AsyncImageWarper(mCV, mRefImgInfo);
					mAsyncImageWarper.setImageWarpedListener(this);
					mAsyncImageWarper.execute(workImage);
				}
				break;
			case DONT_DISPLAY:
			default:
				return mRgba;
			}
		}
		if (mResult != null) {
			logImg("Input result", mResult);
			return mResult;
		} else 
			return mRgba;
	}

	//////////////////////////////////////////////////////
	//// CameraViewBase callback methods for interpreting 
	//////////////////////////////////////////////////////

	@Override
	public void onFailedToExtractFeatures() {
		// TODO Notify user that the application was unable to extract any features 
		// about the display

		// Set all other asyncronous tasks to null signifying that features
		// have not been found on reference image
		mAsyncBoxer = null;
		mAsyncFeatureDrawer = null;
		mAsyncImageWarper = null;
	}

	/**
	 * This will initialize all the nescesary 
	 */
	@Override
	public void onExtractedFeatures(ImageInformation info) {
		// Initialize the Async Boxer if there is an image set available
		mRefImgInfo = info;
		mDisplayOptSpinner.setEnabled(true);
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Either find out why it stopped or close the program 
		// due to intention stop
		Log.i(TAG, "OpenCV Camerabridge has stopped");
	}

	private Mat mResult;

	@Override
	public void onImageWarped(Mat warpedImage) {
		if (warpedImage != null)
			mResult = warpedImage;
		mAsyncImageWarper = null; // Reset the task
	}

	@Override
	public void onFeaturesDrawn(Mat imgWFeat) {
		if (imgWFeat != null)
			mResult = imgWFeat;
		mAsyncFeatureDrawer = null;// Reset the task
	}

	@Override
	public void onBoxDrawn(Mat image) {
		if (image != null)
			mResult = image;
		mAsyncBoxer = null;// Reset the task
	}

	//////////////////////////////////////////////////////
	//// Unused inherited methods
	//////////////////////////////////////////////////////

	@Override
	public void cvLogd(String msg) {}

	@Override
	public void cvLogd(String tag, String msg) {}

	@Override
	public void cvLoge(String msg) {}

	@Override
	public void cvLoge(String tag, String msg) {}

	// Called when a started intent return
	// In our case it for when External Application returns 
	// an image
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			Toast t = Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT);
			t.show();
			return;
		}

		Bundle b = data.getBundleExtra(Appliance.KEY_BUNDLE_APPLIANCE);
		Appliance a = Appliance.toAppliance(b);

		if (a == null) {
			Log.e(TAG, "Null Appliance on returned View is not re enabled");
			return;
		}

		// Unable to extract any features
		if (a.getApplianceFeatures() == null){
			Log.w(TAG, "Appliance: " + a + " did not come with any features.");
			a.setApplianceFeatures(ApplianceFeatureFactory.getEmptyApplianceFeatures());
		}

		// We have a valid appliance.
		// Might have no features 
		mCurrentAppliance = a;
	}

	////////////////////////////////////////////////////////////////////
	///// Private Helper class that will help encapsulate Tranformation Builder Procedure
	////////////////////////////////////////////////////////////////////

	@Override
	public void onItemSelected(AdapterView<?> spinner, View arg1, int pos,
			long arg3) {
		if (spinner == mDisplayOptSpinner){
			mCurrentOption = mDisplayOptions[pos];
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> spinner) {}

	/**
	 * Initializes spinners for display options
	 * @param s 
	 */
	private void initializeDisplayOptions(Spinner s) {
		ArrayAdapter<DISPLAY_OPTION> dataAdapter = 
				new ArrayAdapter<DISPLAY_OPTION>(this, android.R.layout.simple_spinner_item, mDisplayOptions);
				dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				s.setAdapter(dataAdapter);

				// Set the default feature to be the book
				DISPLAY_OPTION defualtOpt = mDisplayOptions[2];
				int num = s.getCount();
				for (int i = 0; i < num; ++i){
					if (s.getItemAtPosition(i) == defualtOpt) {
						s.setSelection(i);
						break;
					}
				}
				s.setOnItemSelectedListener(this);
	}

	/*
	 * DEBUG: The following 
	 */
	private static final DISPLAY_OPTION[] mDisplayOptions = {DISPLAY_OPTION.DONT_DISPLAY, 
		DISPLAY_OPTION.FEATURES, DISPLAY_OPTION.BOX, DISPLAY_OPTION.WARP_IMG };

	private enum DISPLAY_OPTION {
		FEATURES("Display Features"), 
		WARP_IMG("Display Warp"), 
		BOX("Display Box"), 
		DONT_DISPLAY("Don't Display");

		private final String mtext;

		private DISPLAY_OPTION(String text){
			mtext = text;
		}

		@Override
		public String toString(){
			return mtext;
		}
	}

	/**
	 * Wrapper helper method that gets the Display Option that correlates to the specific String
	 * @param s
	 * @return
	 */
	private static DISPLAY_OPTION getOption(String s){
		for (DISPLAY_OPTION d: mDisplayOptions){
			if (d.toString().equals(s))
				return d;
		}
		return DISPLAY_OPTION.DONT_DISPLAY;
	} 
}
