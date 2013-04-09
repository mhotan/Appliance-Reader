package uw.cse.mag.appliancereader.camera;

import java.io.File;
import java.io.FileNotFoundException;

import uw.cse.mag.appliancereader.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

/*
 * Obtains a picture using an external application.
 * This class extends BaseImageTaker. Take a look at the class description
 * to get an idea on how to use it
 */
public class ExternalApplication extends BaseImageTaker {

	private static final String TAG = ExternalApplication.class.getSimpleName();

	private String mTempFilePath;
	private String mTempFileName;

	private static int RESULT_COUNTER = 1;
	public final static int TAKE_PICTURE_RESULT = RESULT_COUNTER++;
	public final static int CHOOSE_PICTURE_RESULT = RESULT_COUNTER++;

	/**
	 * Extra placed incase client wants to skip menu
	 * Options for values:
	 * TAKE_PICTURE_RESULT
	 * CHOOSE_PICTURE_RESULT
	 */
	public static final String EXTRA_SPECIFIC_REQUEST_TYPE = "SPECIFIC_REQUEST";
	// Temporary place to store the captured image
	private File mTemporaryFile;
	private int mPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Open the content view
		setContentView(R.layout.picture_chooser_layout);

		// reference the buttons for choosing
		ImageButton pic_taker = (ImageButton) findViewById(R.id.take_picture_button);
		ImageButton pic_chooser = (ImageButton) findViewById(R.id.choose_picture_button);

		View.OnClickListener buttonListener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// if user selects take picture button
				if (v.getId() == R.id.take_picture_button) {
					takePicture();
					// If user selects 	
				} else if (v.getId() == R.id.choose_picture_button){
					choosePicturefromGallery();
				}
			}
		};

		pic_taker.setOnClickListener(buttonListener);
		pic_chooser.setOnClickListener(buttonListener);

		// Default directory to store pictures
		mTempFilePath = getExternalFilesDir(null).getAbsolutePath();
		mTempFileName = System.currentTimeMillis()+ ".bmp";
		// Time stamp the image name
		mTemporaryFile = new File(mTempFilePath, mTempFileName);
		Log.d(TAG, "Camera directory: " + mTemporaryFile.getAbsolutePath());
		// Start camera intent
		Intent extras = getIntent();
		int beSpecific = extras.getIntExtra(EXTRA_SPECIFIC_REQUEST_TYPE, -1);
		if (beSpecific == TAKE_PICTURE_RESULT)
			takePicture();
		else if (beSpecific == CHOOSE_PICTURE_RESULT)
			choosePicturefromGallery();
	}

	@Override
	public void onBackPressed(){
		// Send result code notifying stopped process
		finishFail();
	}

	/**
	 * uses external program to obtain a picture from gallery
	 * Returns result in this or super classes onActivityResult
	 */
	private void choosePicturefromGallery() {
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, CHOOSE_PICTURE_RESULT);
	}

	/*
	 * Runs an external intent to take a picture.
	 */
	private void takePicture() {
		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTemporaryFile));
		startActivityForResult(i, TAKE_PICTURE_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Picture is taken!
		if (requestCode == TAKE_PICTURE_RESULT) {
			if (resultCode == RESULT_OK) {
				try {
					if(!mTemporaryFile.exists())
						throw new FileNotFoundException();
					// Return and quit
					finishAndReturnImagePath(mTemporaryFile.getAbsolutePath());
				} catch (FileNotFoundException e) {
					Log.e(TAG, "ExternalApplication.java: temporary file not found!");
					e.printStackTrace();
				}
			} else {
				finishFail();
			}
		} else if (requestCode == CHOOSE_PICTURE_RESULT) {
			if (resultCode == RESULT_OK) {
				Uri photoUri = data.getData();

				if (photoUri != null) {
					try {
						finishAndReturnImagePath(photoUri);
					} catch (Exception e) {
						Log.e(TAG, "ExternalApplication.java: temporary file not found!");
						e.printStackTrace();
					}
				}
			} else {
				finishFail();
			}
		}
	}
}
