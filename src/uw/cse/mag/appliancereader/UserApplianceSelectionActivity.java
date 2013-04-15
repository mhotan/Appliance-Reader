package uw.cse.mag.appliancereader;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.db.ApplianceDataSource;
import uw.cse.mag.appliancereader.db.ApplianceDataSource.DatabaseNotInitializedException;
import uw.cse.mag.appliancereader.db.UserAppliancesSQLiteHelper;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class UserApplianceSelectionActivity extends ListActivity {

	private static final Logger log = Logger.getLogger(UserApplianceSelectionActivity.class.getSimpleName()); 

	private ApplianceDataSource datasource;

	private ListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.appliance_list_layout);

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
			public boolean onItemLongClick(AdapterView parentView, View childView, int position, long id) {
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

		});
	}

}
