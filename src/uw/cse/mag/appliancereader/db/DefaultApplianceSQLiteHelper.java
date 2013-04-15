package uw.cse.mag.appliancereader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * An SQLite Open Helper for local android specific data base appliance
 * <br>This particular TABLE of Appliances contain all the preformatted appliances
 * that are generally used for test purposes
 * 
 *	Database Layout
 *	Columns...
 *
 *	Appliance Nickname | Make | Model | FileDirectory (Includes Images and XML file)
 * 
 * @author Michael Hotan
 */
public class DefaultApplianceSQLiteHelper extends ApplianceSQLiteHelper {
	// Log purposes
	private static final String TAG = DefaultApplianceSQLiteHelper.class.getSimpleName();

	public DefaultApplianceSQLiteHelper(Context ctx) {
		super(ctx);
		TABLE_NAME = "defaultappliances";
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		Log.d(TAG, "Creating a new database");
		database.execSQL(getSQLDataBaseCreate());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
	            + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	@Override
	public String getTableName() {
		return TABLE_NAME;
	}
}
