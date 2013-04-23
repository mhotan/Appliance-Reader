package uw.cse.mag.appliancereader;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

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
import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.datatype.ApplianceFeatureFactory;
import uw.cse.mag.appliancereader.datatype.ApplianceFeatures;
import uw.cse.mag.appliancereader.db.ApplianceDBAdapter;
import uw.cse.mag.appliancereader.db.ApplianceDataSource;
import uw.cse.mag.appliancereader.db.ApplianceNotExistException;
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

	// String representations of storage
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ApplianceReaderSpecific/";
	public static final String APPLIANCES_PATH = DATA_PATH + "Appliances/";

	private static final int SELECT_APPLIANCE = 0x1;

	/**
	 * This the a main parameter mostly used for debugging to look at
	 * different outputs of opencv calls of the same appliance
	 */
	private DISPLAY_OPTION mCurrentOption;

	/**
	 * 
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
	 * Data source to retrieve 
	 */
	private ApplianceDataSource mUsersDataSource;
	
	/**
	 * This is only to initialize or update database tables
	 * 
	 */
	private ApplianceDBAdapter mDatabaseAdapter;

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

	private static final String DEFAULT_APPLIANCE = "_UNKNOWN_";

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

		
		mDatabaseAdapter = new ApplianceDBAdapter(this);
		mDatabaseAdapter.open();
		mDatabaseAdapter.close();
		// Initialize the data source to retrieve the appliance in question
		mUsersDataSource = new ApplianceDataSource(this, ApplianceDBAdapter.USER_TABLE);
	}

	@Override
	public void onItemSelected(AdapterView<?> spinner, View arg1, int pos,
			long arg3) {
		if (spinner == mDisplayOptSpinner){
			mCurrentOption = mDisplayOptions[pos];
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> spinner) {
	}


	@Override
	public void onPause()
	{
		mUsersDataSource.close();

		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		super.onPause();
	}

	@Override
	public void onResume()
	{
		mCurrentAppliance = new AppliancePeferenceSharer(this).getLastAppliance();
		if (mCurrentAppliance == null) {
			// Unable to find last appliance
			Intent i = new Intent(this, UserApplianceSelectionActivity.class);
			startActivityForResult(i, SELECT_APPLIANCE);
		} else {
			Log.i(TAG, "Past Appliance found!");
		}
		
		// Open the data base for use
		mUsersDataSource.open();

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
		mDatabaseAdapter.close();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
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



	@Override
	public void onCameraViewStarted(int w, int h) {
		// TODO adjust the reference image with respect to the width and height that the camera is starting at
		// If camera in portrait mode the h is the actual width and the w is the actual height
		// If camera in landscape mode the w => actual width and h=> actual height
		// Upload the base image with the correct dimension as specified

		// get camera orientaion
		// This orientation check is because OPENCV for android cant handle orientation changes
		// IE think everything is in landscape
		int actualOrientation = getResources().getConfiguration().orientation;
		if (actualOrientation == Configuration.ORIENTATION_PORTRAIT) {
			int temp = w;
			w = h;
			h = temp;
		} 

		Size actualDimension = new Size(w,h);
		// TODO check whether these dimensions are correct
		mRgba = new Mat(actualDimension.height, actualDimension.width, CvType.CV_8UC4);

		//	Reference the initial Appliance 
		//  Grab its reference image
		if (mCurrentAppliance == null) {
			Log.w(TAG, "onCameraViewStarted, Appliance should not be null");

			// Change button of the appliance to show that no appliance is chosen
			// TODO

			return;
		}
		// Assert we have a current appliance

		// Get image orientation
		int refOrientation;
		try {
			refOrientation = mUsersDataSource.getRefimageOrientation(mCurrentAppliance);
			if (refOrientation == actualOrientation){
				// Obtain the original size of the image
				Size originaSize = mUsersDataSource.getSizeOfRefImage(mCurrentAppliance);
				// Obtain the image but scaled to the factor that matches the screen
				Bitmap b = mUsersDataSource.getReferenceImage(mCurrentAppliance, actualDimension);
				// Scale down the features of this appliance if it exists
				float scaleFactor = originaSize.width / b.getWidth();
				mCurrentAppliance.scaleDownFeatures(scaleFactor);

				// Convert our scaled image to a Mat
				Mat mRefImg = ImageConversion.bitmapToMat(b);

				// Initialize the feature extraction for the reference Image
				mAsyncFD = new AsyncFeatureDetector(mCV);
				mAsyncFD.setFeatureDetectionListener(this);
				mAsyncFD.execute(mRefImg);

				b.recycle(); // Free up the memory as soon as possible
			}
		} catch (ApplianceNotExistException e) {
			Log.e(TAG, "Failed to retireve Appliance image and features when camera started");
		}

		Log.i(TAG, "OpenCV Camerabridge has started with width: " + w + " and height: " + h );
	}

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
	 * Holds the reference image information
	 */
	private ImageInformation mRefImgInfo = null; 

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

	@Override
	public Mat onCameraFrame(Mat inputFrame) {
		// Set this image as new Other image
		Log.i(TAG, "Mat input width: " + inputFrame.cols() + " height:" + inputFrame.rows());

		// Transfer the data to mRgba for general display
		inputFrame.copyTo(mRgba);


		Mat workImage = new Mat();
		mRgba.copyTo(workImage);

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
			}
		}
		if (mResult != null)
			return mResult;
		else 
			return mRgba;
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

		String message = null;
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

	private static boolean DEBUG = false;

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
