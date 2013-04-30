package uw.cse.mag.appliancereader;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import uw.cse.mag.appliancereader.db.DefaultApplianceDataSource;
import uw.cse.mag.appliancereader.db.DefaultApplianceFeatureLoader;
import uw.cse.mag.appliancereader.db.datatype.Appliance;
import uw.cse.mag.appliancereader.db.datatype.ApplianceFeatureFactory;
import uw.cse.mag.appliancereader.db.datatype.ApplianceFeatures;
import uw.cse.mag.appliancereader.util.ImageIO;
import uw.cse.mag.appliancereader.util.Util;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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

	private DefaultApplianceDataSource datasource;

	// UI elements
	private Button mDownloadMore;
	private ImageButton mSpeakButton;
	private ListView mListView;

	private Appliance ApplianceToReturn;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.appliance_list_layout);

		mListView = (ListView)findViewById(R.id.appliance_list);

		// Create connection to database
		//		DefaultApplianceSQLiteHelper helper = new DefaultApplianceSQLiteHelper(this);
		datasource = new DefaultApplianceDataSource(this);
		datasource.open();

		ArrayAdapter<?> adapter = null;

		if (!datasource.hasAppliances()) {
			log.log(Level.INFO, "No default appliances were found in database");

			// TODO Fix hard code and download remote host
			// Test image
			String nickName = "Linear Algebra Book";
			String type = "book";

			// TODO fix hardcode
			String xmlString = "<annotation><filename>book.jpg</filename><folder>users/mhotan//book" +
					"</folder><source><submittedBy>Michael Hotan</submittedBy></source><imagesize><nrows>2592</nrows>" +
					"<ncols>1944</ncols></imagesize><object><name>book</name><deleted>0</deleted><verified>0</verified>" +
					"<date>04-Mar-2013 19:18:51</date><id>0</id><polygon><username/><pt><x>289</x><y>325</y></pt><pt>" +
					"<x>1672</x><y>338</y></pt><pt><x>1701</x><y>2068</y></pt><pt><x>276</x><y>2085</y></pt></polygon>" +
					"</object></annotation>";
			ApplianceFeatures features = 
					ApplianceFeatureFactory.getApplianceFeaturesFromString(xmlString);

			// Find the current screen size and 
			Display display = getWindowManager().getDefaultDisplay();
			int reqWidth = display.getWidth();
			int reqHeight = display.getHeight();

			Bitmap b = ImageIO.decodeSampledBitmapFromResource(getResources(),
					R.raw.book, reqWidth, reqHeight);

			Appliance a = datasource.createAppliance(nickName, null, null, type, b);
			a.setApplianceFeatures(features);

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
			//				ApplianceFeatures "<annotation><filename>book.jpg</filename><folder>users/mhotan//book" +
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
			List<Appliance> values = datasource.getAllAppliances();

			adapter = new ArrayAdapter<Appliance>(
					this, android.R.layout.simple_expandable_list_item_1, values);

			mListView.setOnItemLongClickListener(new OKLongClickListener());
		}

		if (adapter != null)
			mListView.setAdapter(adapter);

		mDownloadMore = (Button) findViewById(R.id.more_appliance_button);
		mSpeakButton = (ImageButton) findViewById(R.id.speak_button);
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
			ApplianceToReturn = (Appliance)mListView.getItemAtPosition(position);
			finish();
			return false;
		}

	}
	
	@Override
	public void finish() {
	  // Prepare data intent 
	  Intent data = new Intent();
	  if (ApplianceToReturn != null)
		  data.putExtra(Appliance.KEY_BUNDLE_APPLIANCE, ApplianceToReturn.toBundle());
	  // Activity finished ok, return the data
	  setResult(RESULT_OK, data);
	  super.finish();
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
