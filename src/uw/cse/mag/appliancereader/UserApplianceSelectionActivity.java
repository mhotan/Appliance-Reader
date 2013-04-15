package uw.cse.mag.appliancereader;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import uw.cse.mag.appliancereader.camera.BaseImageTaker;
import uw.cse.mag.appliancereader.camera.ExternalApplication;
import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.db.ApplianceDataSource;
import uw.cse.mag.appliancereader.db.ApplianceDataSource.DatabaseNotInitializedException;
import uw.cse.mag.appliancereader.db.UserAppliancesSQLiteHelper;
import uw.cse.mag.appliancereader.util.ImageIO;
import uw.cse.mag.appliancereader.util.Util;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class UserApplianceSelectionActivity extends ListActivity implements OnLongClickListener {

	private static final String TAG = UserApplianceSelectionActivity.class.getSimpleName();
	private static final Logger log = Logger.getLogger(UserApplianceSelectionActivity.class.getSimpleName()); 
	private static final int REQUEST_CODE_DEFAULT_APPLIANCES = 0x1;
	private static final int REQUESTCODE_REFERENCE_IMG = REQUEST_CODE_DEFAULT_APPLIANCES + 1;
	private ApplianceDataSource datasource;

	//UI elements
	private Button mGetMoreButton;
	private ImageButton mSpeakButton;
	private ListView mListView;

	private TakePictureOption mTakePicture;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.appliance_list_layout);

		// Create the default option to take a picture
		mTakePicture = new TakePictureOption();

		mListView = (ListView)findViewById(R.id.appliance_list);

		// Create connection to database
		UserAppliancesSQLiteHelper helper = new UserAppliancesSQLiteHelper(this);
		datasource = new ApplianceDataSource(helper);
		datasource.open();

		List<Appliance> values = null;
		try {
			values = datasource.getAllAppliances();
		} catch (DatabaseNotInitializedException e) {
			log.log(Level.SEVERE, "Unable to access the data base for all appliances.  The current" +
					"implementation does not properly open the database");
		}

		ArrayAdapter<Appliance> adapter = new ArrayAdapter<Appliance>(
				this, android.R.layout.simple_expandable_list_item_1, values);
		setListAdapter(adapter);

		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(
					AdapterView<?> parentView, View childView, int position, long id) {
				// this will provide the value
				Appliance appliance = (Appliance) mListView.getItemAtPosition(position);

				if (appliance == mTakePicture) {
					// User wants to take a picture
					getImageForReference(REQUESTCODE_REFERENCE_IMG);
				} else {
					int result = RESULT_OK;
					Bundle b = null;
					if (appliance == null || appliance.getID() == -1) {
						// Fail case
						result = RESULT_CANCELED;
					} else 
						b = appliance.toBundle();

					Intent data = new Intent();
					data.putExtra(Appliance.KEY_BUNDLE_APPLIANCE, b);
					setResult(result, data);
					finish();
				}
				return false;
			}
		});

		// Add on click listener
		mGetMoreButton = (Button) findViewById(R.id.more_appliance_button);
		mSpeakButton = (ImageButton) findViewById(R.id.speak_button);

		mGetMoreButton.setOnLongClickListener(this);
		mSpeakButton.setOnLongClickListener(this);
	}

	@Override
	protected void onResume() {
		datasource.open();
		super.onResume();
	}

	@Override
	protected void onPause() {
		datasource.close();
		super.onPause();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			try {
				switch (requestCode) {
				case REQUEST_CODE_DEFAULT_APPLIANCES : 

					Bundle b = data.getBundleExtra(Appliance.KEY_BUNDLE_APPLIANCE);
					Appliance app = Appliance.toAppliance(b);

					datasource.createAppliance(app);
					break;
				case REQUESTCODE_REFERENCE_IMG: 
					// Add more cases

					// New appliance to store
					Bitmap img = processNewAppliance(data);

					// TODO Ask user for name
					String timeStamp = Util.getTimeStamp();
					String name = "Unknown_" + timeStamp;

					//TODO Ask user to draw bounding box.
					Appliance a = new Appliance();
					a.setNickName(name);
					// Save the reference image
					datasource.createAppliance(a);
					datasource.saveApplianceReferenceImage(a, img);
				}
				// Always executes
				setResult(resultCode, data);
				finish();
			} catch (DatabaseNotInitializedException e) {
				log.log(Level.SEVERE, "Database was not intialized for appliance e: " + e);
			}
		}

	}

	private Bitmap processNewAppliance(Intent data) {
		//The user is given two option upon return
		// either take an image or choose an existing images
		String filePath = data.getExtras().getString(BaseImageTaker.INTENT_RESULT_IMAGE_PATH);
		Uri uri = data.getExtras().getParcelable(BaseImageTaker.INTENT_RESULT_IMAGE_URI); 

		// Save full size image as reference, scale later
		Bitmap image;
		// Upload our image for manipulation
		// We can only choose so for now prioritize choosing the the image
		// taken through the camera
		if (filePath != null){
			//			image = ImageIO.loadBitmapFromFilePath(filePath, null);
			image = ImageIO.loadBitmapFromFilePath(filePath, null);
		} else if (uri != null) {
			//			image = ImageIO.loadBitmapFromURI(getContentResolver(), uri, null);
			image = ImageIO.loadBitmapFromURI(getContentResolver(), uri, null);
		} else {
			throw new RuntimeException("Unable to load any image from External Application");
		}

		String message = null;
		// Have to check if there was an error interpretting the image
		if ( image == null ){ // This should never happen where it cant load an image
			message = "Null image cannot display";
			Log.e(TAG, message);
			String tMessage = "Unable to upload Image at ";
			if ( filePath != null)
				tMessage += filePath;
			else if (uri != null)
				tMessage += uri.toString();
			else 
				tMessage += "Unknown Source";
			Toast t = Toast.makeText(this, tMessage, Toast.LENGTH_LONG);
			t.show();
			return null;
		} 
		// TODO Speak Error
		return image;
	}

	@Override
	public boolean onLongClick(View v) {
		if (v == mGetMoreButton) {
			// Start the default selection activity to select A user appliance
			Intent i = new Intent(UserApplianceSelectionActivity.this, DefaultSelectionActivity.class);
			this.startActivityForResult(i, REQUEST_CODE_DEFAULT_APPLIANCES);
		} else if (v == mSpeakButton) {
			// TODO Implement
			Util.getNotImplementedDialog(getApplicationContext()).show();
		}
		return true;
	}

	/**
	 * Starts activity to obtain image for further processing
	 * Img address is set to 
	 * @param id
	 */
	private void getImageForReference(int extraREquest){
		//		Log.d(TAG,"Calling camera intent"); 
		Intent i = new Intent(this, ExternalApplication.class);
		i.putExtra(ExternalApplication.EXTRA_SPECIFIC_REQUEST_TYPE, extraREquest);
		startActivityForResult(i, REQUESTCODE_REFERENCE_IMG);
	} 

	private static final String TAKE_PICTURE = "Take a picture of a new appliance?";
	public class TakePictureOption extends Appliance {

		public String toString(){
			return TAKE_PICTURE;
		}

	}

}
