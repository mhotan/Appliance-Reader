package uw.cse.mag.appliancereader;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.datatype.ApplianceFeatureFactory;
import uw.cse.mag.appliancereader.datatype.ApplianceFeatures;
import uw.cse.mag.appliancereader.db.ApplianceDBAdapter;
import uw.cse.mag.appliancereader.db.ApplianceDataSource;
import uw.cse.mag.appliancereader.db.ApplianceDataSource.DatabaseNotInitializedException;
import uw.cse.mag.appliancereader.db.DefaultApplianceFeatureLoader;
import uw.cse.mag.appliancereader.imgproc.ImageConversion;
import uw.cse.mag.appliancereader.util.ImageIO;
import uw.cse.mag.appliancereader.util.Util;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

public class DefaultSelectionActivity extends Activity implements OnLongClickListener {

	private static final String TAG = DefaultSelectionActivity.class.getSimpleName();
	private static final Logger log = Logger.getLogger(DefaultSelectionActivity.class.getSimpleName()); 

	private ApplianceDataSource datasource;

	// UI elements
	private Button mDownloadMore;
	private ImageButton mSpeakButton;
	private ListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.appliance_list_layout);

		mListView = (ListView)findViewById(R.id.appliance_list);

		// Create connection to database
//		DefaultApplianceSQLiteHelper helper = new DefaultApplianceSQLiteHelper(this);
		datasource = new ApplianceDataSource(this, ApplianceDBAdapter.DEFAULT_TABLE);
		datasource = datasource.open();

		ArrayAdapter<?> adapter = null;

		try {
			if (!datasource.hasAppliances()) {
				log.log(Level.INFO, "No default appliances were found in database");

				// TODO Fix hard code and download remote host
				// Test image
				String appName = "Linear Algebra Book";
				String xmlString = DefaultApplianceFeatureLoader.readRawTextFile(
						this.getApplicationContext(), R.xml.book);
				ApplianceFeatures features = 
						ApplianceFeatureFactory.getApplianceFeaturesFromString(xmlString);

				// Find the current screen size and 
				Display display = getWindowManager().getDefaultDisplay();
				int reqWidth = display.getWidth();
				int reqHeight = display.getHeight();
				
				Bitmap b = ImageIO.decodeSampledBitmapFromResource(getResources(),
						 R.raw.book, reqWidth, reqHeight);
				
				Appliance a = new Appliance();
				a.setNickName(appName);
				try {
					a = datasource.createAppliance(a, b);
					//				datasource.saveApplianceReferenceImage(a, b);
					datasource.saveApplianceFeatures(a, features);
				} catch (DatabaseNotInitializedException e) {
					Log.e(TAG, "database closed accidently");
				}

				// Add more default appliance
				// TODO Fix this process of adding resource XML file documents
				//			
				//			HashMap<String, String> defaultAppliances = null;
				//			try {
				//				defaultAppliances = DefaultApplianceFeatureLoader.getDefaultAppliances(this);
				//			} catch (Exception e) {
				//				log.log(Level.SEVERE, "Unable to obtain XML files that store XML representation of appliance features");
				//			}
				//
				//			for (String appName: defaultAppliances.keySet()){
				//				ApplianceFeatures features = ApplianceXMLParser.getApplianceFeaturesFromString(
				//						defaultAppliances.get(appName));
				//				// Appliance 
				//				Appliance a = new Appliance();
				//				a.setNickName(appName);
				//
				//				try {
				//					a = datasource.createAppliance(a);
				//					if (!datasource.saveApplianceFeatures(a, features)){
				//						log.log(Level.SEVERE, "Appliance was not in database right after atomic insertion");
				//					}
				//				} catch (DatabaseNotInitializedException e) {
				//					log.log(Level.SEVERE, "Database was closed inadvertantly");
				//				}
				//			}
			}

			// Now there should be appliances in there
			if (!datasource.hasAppliances()){
				log.log(Level.SEVERE, "Failed to initialize the database of default appliance");

				String s = "Unable to recover xml files";
				List<String> list = new ArrayList<String>();
				list.add(s);
				adapter = new ArrayAdapter<String>(
						this, android.R.layout.simple_expandable_list_item_1, 
						list);
				mListView.setOnItemLongClickListener(new FailLongClickListener());
			} else {
				// Success case			
				List<Appliance> values = null;
				try {
					values = datasource.getAllAppliances();
				} catch (DatabaseNotInitializedException e) {
					log.log(Level.SEVERE, "Unable to access the data base for all appliances.  The current" +
							"implementation does not properly open the database");
				}
				adapter = new ArrayAdapter<Appliance>(
						this, android.R.layout.simple_expandable_list_item_1, values);

				mListView.setOnItemLongClickListener(new OKLongClickListener());
			}
		} catch (DatabaseNotInitializedException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Unable to initialize database appropiately");
			throw new RuntimeException(e.getMessage());
		}

		if (adapter != null)
			mListView.setAdapter(adapter);

		mDownloadMore.setOnLongClickListener(this);
		mDownloadMore.setText(R.string.str_download_appliances);
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

	private class FailLongClickListener implements OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			// SHow dialog saying its not implemented
			Util.getNotImplementedDialog(getApplicationContext()).show();
			return false;
		}

	}

	private class OKLongClickListener implements OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(
				AdapterView<?> parentView, View childView, int position, long id) {
			// this will provide the value
			Appliance appliance = (Appliance)mListView.getItemAtPosition(position);

			int result = RESULT_OK;
			Bundle b = null;
			if (appliance == null || appliance.getID() == -1) {
				// Fail case
				result = RESULT_CANCELED;
			} else 
				b = appliance.toBundle();

			setResult(result);
			Intent data = new Intent();
			data.putExtra(Appliance.KEY_BUNDLE_APPLIANCE, b);
			finish();
			return false;
		}

	}

	@Override
	public boolean onLongClick(View v) {
		if (v == mDownloadMore) {
			// Todo Implement
			Util.getNotImplementedDialog(getApplicationContext()).show();
		} else if (v == mSpeakButton) {
			// Todo IMplement
			Util.getNotImplementedDialog(getApplicationContext()).show();
		}
		return false;
	}
}
