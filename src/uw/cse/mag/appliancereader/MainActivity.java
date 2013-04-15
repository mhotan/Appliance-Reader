package uw.cse.mag.appliancereader;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import uw.cse.mag.appliancereader.camera.BaseImageTaker;
import uw.cse.mag.appliancereader.camera.ExternalApplication;
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
import uw.cse.mag.appliancereader.db.FileManager.ApplianceNotExistException;
import uw.cse.mag.appliancereader.imgproc.ImageConversion;
import uw.cse.mag.appliancereader.imgproc.Size;
import uw.cse.mag.appliancereader.util.ImageIO;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
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
	 * Computer vision instance that can handle 
	 * all Computer Vision oriented task
	 */
	private ComputerVision mCV;

	private CameraBridgeViewBase mOpenCvCameraView;

//	private ApplianceFeatures mRefImageSet;

//	private FileManager fileManager;
//
	private Appliance mCurrentAppliance;

	private Spinner mDisplayOptSpinner;
	
	// Asyncronous calculators
	private AsyncFeatureDetector mAsyncFD;
	private AsyncBoxDrawer mAsyncBoxer;
	private AsyncFeatureDrawer mAsyncFeatureDrawer;
	private AsyncImageWarper mAsyncImageWarper;

	//	private TransformationBuilderWrapper mTransformBuilder;

	private Mat mRgba;
	private ImageInformation mRefImgInfo;
	
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
		mCurrentOption = getOption((String)mDisplayOptSpinner.getSelectedItem());
		// Dont enable until ready
		mDisplayOptSpinner.setEnabled(false);

		// Create a computer vision instance to handle all the procedures
		mCV = new ComputerVision(this.getApplicationContext(), this, this);
	
		mCurrentAppliance = new AppliancePeferenceSharer(this).getLastAppliance();
		if (mCurrentAppliance == null) {
			// Unable to find last appliance
			Intent i = new Intent(this, UserApplianceSelectionActivity.class);
			startActivityForResult(i, SELECT_APPLIANCE);
		} else {
			Log.i(TAG, "Past Appliance found!");
		}
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
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		super.onPause();
	}

	@Override
	public void onResume()
	{
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

	private Mat mRefImg;

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
		mRgba = new Mat(actualDimension.height, actualDimension.width, CvType.CV_8UC4);

		// get the layout of the reference image 
		String currRefImgPath = null;
		try {
			currRefImgPath = fileManager.getReferenceImage(mCurrentAppliance);
		} catch (NameFormatException e) {
			e.printStackTrace();
		}
		if (currRefImgPath == null){
			Log.e(TAG, "onCameraViewStarted, unable to load reference image");
			Toast t = Toast.makeText(this, "unable to load reference image", Toast.LENGTH_LONG);
			t.show();
			return;
		}

		// Get image orientation
		int refOrientation = ImageIO.getOrientationOfImage(currRefImgPath);

		if (refOrientation == actualOrientation){
			// Load the image in at the size of the actual dimensions of the camera
			Size imageDim = ImageIO.getSizeOfImage(currRefImgPath);
			Bitmap ref = ImageIO.loadBitmapFromFilePath(currRefImgPath, actualDimension);
			int nWidth = ref.getWidth();
			int scaleDownFactor = imageDim.width / nWidth;
			Log.i(TAG, "New height and width scale factors " +scaleDownFactor);
			// Set as other image, because this has the object
			// This way we can map points to new image
			//			Rect bounds = null;
			if (mRefImageSet != null) {
				mRefImageSet.scaleDownFeatures(scaleDownFactor);
				//				bounds = mRefImageSet.getBoundingBox();
			}
			// Set this as other image because the homography maps
			// TODO change this to reference after DEMO FIXME

			mRefImg = ImageConversion.bitmapToMat(ref);
			//			mTransformBuilder.setReferenceImage(ImageConversion.bitmapToMat(ref), bounds);

			// Initialize the feature extraction for the reference Image
			mAsyncFD = new AsyncFeatureDetector(mCV);
			mAsyncFD.setFeatureDetectionListener(this);
			mAsyncFD.execute(mRefImg);

			ref.recycle(); // Free up the memory as soon as possible

		} else {
			// Rotate the dimensions of the image to match the camera frame
			//TODO Implement handling orientation differences.
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
		inputFrame.copyTo(mRgba);
		// Update the transformation builder with this new builder
		//		
		//		
		//		mTransformBuilder.setOtherImage(mRgba);
		//
		//		//		// Get the Homo
		//		//		Mat lastWarp = mTransformBuilder.getLatestWarp();
		//		//		if (lastWarp != null){
		//		//			lastWarp.copyTo(mRgba);
		//		//		}
		//
		//		if (mTransformBuilder.getCurrentHomography() != null && mRefImageSet != null) {
		//			// Draw a bound over where we think the display is
		//			List<String> features = mRefImageSet.getFeatures();
		//			for (String f: features) {
		//				//trasnform points on
		//				List<Point> transformed = mCV.getWarpedPoints(mRefImageSet.getShapePoints(f), mTransformBuilder.getCurrentHomography());
		//				// Draw lines
		//				for (int i = 0; i < transformed.size()-1; ++i){
		//					mCV.drawLine(transformed.get(i), transformed.get(i+1), mRgba);
		//				}
		//				mCV.drawLine(transformed.get(transformed.size()-1), transformed.get(0), mRgba);
		//			}
		//		}
		Mat workImage = new Mat();
		mRgba.copyTo(workImage);
		if (mRefImgInfo != null){
			switch (mCurrentOption){
			case BOX:
				if (mAsyncBoxer == null && mRefImageSet != null){
					List<List<Point>> displays = new ArrayList<List<Point>>();
					for (String feature: mRefImageSet.getFeatures()){
						displays.add(mRefImageSet.getShapePoints(feature));
					}
					mAsyncBoxer = new AsyncBoxDrawer(mCV, mRefImgInfo, displays);
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
		
		if (a == null)
			Log.e(TAG, "Null Appliance on return");
		
		// If reference image
		if (requestCode == REQUESTCODE_REFERENCE_IMG){
			// Save new image as reference
			try {
				fileManager.setReferenceImage(mCurrentAppliance, image);
			} catch (ApplianceNotExistException e) {
				Log.e(TAG, "Appliance does not exist, should not have occured");
				e.printStackTrace();
			} catch (NameFormatException e) {
				Log.e(TAG, "Appliance illegal name, should not have occured");
				e.printStackTrace();
			}
		}
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

		//Set the default value
		String defualtOpt = mDisplayOptions[0].toString();
		int num = s.getCount();
		for (int i = 0; i < num; ++i){
			if (s.getItemAtPosition(i).equals(defualtOpt)) {
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

//mRefImageSet = null;
//
//if (DEBUG){
//	// Test
//	try {
//		mRefImageSet = new XMLTestImageSet(this, R.xml.book);
//	} catch (IOException e) {
//		Log.e(TAG, "IOException occured when trying to load Reference image: " + e.getMessage());
//	} catch (XmlPullParserException e) {
//		Log.e(TAG, "XML Parser Exception occured when trying to load Reference image: " + e.getMessage());
//	}
//	if (mRefImageSet == null)
//		Log.e(TAG, "Unable to load default data");
//
//	// Attempt to load 
//	String refImgPath = null; 
//	setCurrentApplianceName(book);
//	try {
//		createNewApplianceDirectory(mCurrentAppliance);
//		refImgPath = fileManager.getReferenceImage(mCurrentAppliance);
//		if (refImgPath == null) {
//			// Lets choose one from gallery
//			getImageForReference(ExternalApplication.CHOOSE_PICTURE_RESULT);
//		} 
//	} catch (NameFormatException e) {
//		Log.e(TAG, "Could not obtain book: " + e.getMessage());
//		e.printStackTrace();
//	}
//} else {
//	String name = createDefaultName();
//	if (!createNewApplianceDirectory(name))
//		Log.e(TAG, "Unable to create name"); // Should never do
//	setCurrentApplianceName(name);
//	getImageForReference();
//}
