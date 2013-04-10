package uw.cse.mag.appliancereader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * An SQLite Open Helper for local android specific data base appliance
 * <br>Users need to have access to all the appliances they specifically need
 * <br>This is not the same as the default appliances available
 * 
 *	Database Layout
 *	Columns...
 *
 *	Appliance Nickname | Make | Model | FileDirectory (Includes Images and XML file)
 * 
 * @author Michael Hotan
 */
public class UserAppliancesSQLiteHelper extends ApplianceSQLiteHelper {

	// Log purposes
	private static final String TAG = UserAppliancesSQLiteHelper.class.getSimpleName();
	
//	/**
//	 * Name of table the that stores user appliances
//	 * ID values that increment automatically  
//	 */
//	public static final String TABLE_USER_APPLIANCES = "userappliances";
//	
	// Database creation sql statement

//	private final String DATABASE_CREATE = "create table " + TABLE_USER_APPLIANCES + "(" 
//	+ COLUMN_ID + " integer primary key autoincrement, " 
//			+ COLUMN_NICKNAME + " text not null,"
//		      + COLUMN_MAKE +" text,"
//		      + COLUMN_MODEL +" text,"
//		      + COLUMN_DIRECTORY + " text not null"
//			+ ");";
	
	/**
	 * Constructs lowest level interface for database
	 * @param context Owning context
	 */
	public UserAppliancesSQLiteHelper(Context context){
		super(context);
		TABLE_NAME = "userappliances";
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

}
