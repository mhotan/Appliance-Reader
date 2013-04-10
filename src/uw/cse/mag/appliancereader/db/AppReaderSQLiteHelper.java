package uw.cse.mag.appliancereader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
public class AppReaderSQLiteHelper extends SQLiteOpenHelper {

	// Log purposes
	private static final String TAG = AppReaderSQLiteHelper.class.getSimpleName();
	
	/**
	 * Specific Name of database
	 * And version number
	 */
	private static final String DATABASE_NAME = "userappliances.db";
	private static final int DATABASE_VERSION = 1;
	
	/**
	 * Name of table the that stores user appliances
	 * ID values that increment automatically  
	 */
	public static final String TABLE_USER_APPLIANCES = "userappliances";
	// Table Columns
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NICKNAME = "name";
	public static final String COLUMN_MAKE = "make";
	public static final String COLUMN_MODEL = "model";
	public static final String COLUMN_DIRECTORY = "directory";
	
	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table " + TABLE_USER_APPLIANCES + "(" 
	+ COLUMN_ID + " integer primary key autoincrement, " 
			+ COLUMN_NICKNAME + " text not null,"
		      + COLUMN_MAKE +" text,"
		      + COLUMN_MODEL +" text,"
		      + COLUMN_DIRECTORY + " text not null"
			+ ");";
	
	/**
	 * Constructs lowest level interface for database
	 * @param context Owning context
	 */
	public AppReaderSQLiteHelper(Context context){
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		Log.d(TAG, "Creating a new database");
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
	            + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_APPLIANCES);
		onCreate(db);
	}

}
