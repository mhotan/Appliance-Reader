package uw.cse.mag.appliancereader;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import uw.cse.mag.appliancereader.camera.ExternalApplication;
import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.db.ApplianceDataSource;
import uw.cse.mag.appliancereader.db.ApplianceDataSource.DatabaseNotInitializedException;
import uw.cse.mag.appliancereader.db.UserAppliancesSQLiteHelper;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

public class UserApplianceSelectionActivity extends ListActivity implements OnLongClickListener {

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
			switch (requestCode) {
			case REQUEST_CODE_DEFAULT_APPLIANCES : 
				
				Bundle b = data.getBundleExtra(Appliance.KEY_BUNDLE_APPLIANCE);
				Appliance app = Appliance.toAppliance(b);
				try {
					datasource.createAppliance(app);
				} catch (DatabaseNotInitializedException e) {
					log.log(Level.SEVERE, "Database was not intialized for appliance: " + app);
				}
				
				// Perculate back result
				setResult(resultCode, data);
				finish();
				break;
			case REQUESTCODE_REFERENCE_IMG: 
				// ADd more cases
			}
		}

	}

	@Override
	public boolean onLongClick(View v) {
		if (v == mGetMoreButton) {
			// Start the default selection activity to select A user appliance
			Intent i = new Intent(UserApplianceSelectionActivity.this, DefaultSelectionActivity.class);
			this.startActivityForResult(i, REQUEST_CODE_DEFAULT_APPLIANCES);
		} else if (v == mSpeakButton) {
			// TODO Implement
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
