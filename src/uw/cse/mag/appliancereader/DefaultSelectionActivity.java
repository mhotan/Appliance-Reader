package uw.cse.mag.appliancereader;

import java.util.logging.Logger;

import uw.cse.mag.appliancereader.db.ApplianceDataSource;
import android.app.ListActivity;
import android.widget.ListView;

public class DefaultSelectionActivity extends ListActivity {

	private static final Logger log = Logger.getLogger(DefaultSelectionActivity.class.getSimpleName()); 

	private ApplianceDataSource datasource;

	private ListView mListView;

	
}
