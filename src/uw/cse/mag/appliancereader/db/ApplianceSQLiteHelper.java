package uw.cse.mag.appliancereader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * General Skeleton to create a SQLLite Open helper
 * This database provides a general interface to hold application 
 * specific information about appliances 
 * @author mhotan
 */
abstract class ApplianceSQLiteHelper extends SQLiteOpenHelper {

	private static final String TAG = ApplianceSQLiteHelper.class.getSimpleName();
	
	/**
	 * Specific Name of database
	 * And version number
	 */
	protected static final String DATABASE_NAME = "appliances.db";
	protected static final int DATABASE_VERSION = 1;
	
	/**
	 * Name of table that 
	 * ID values that increment automatically  
	 */
	protected String TABLE_NAME;
	
	// Table Columns
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NICKNAME = "name";
	public static final String COLUMN_MAKE = "make";
	public static final String COLUMN_MODEL = "model";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_DIRECTORY = "directory";
	
	/**
	 * Create an SQl Lite open helper to access database tables
	 * @param context
	 */
	public ApplianceSQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * This method was made to handle subclass request to create different tables
	 * in the same database.  
	 * Generates the Data Base creaet sql command in String form
	 * @return
	 */
	protected String getSQLDataBaseCreate(){
		return "create table " + TABLE_NAME + "(" 
				+ COLUMN_ID + " integer primary key autoincrement, " 
				+ COLUMN_NICKNAME + " text not null,"
			      + COLUMN_MAKE +" text,"
			      + COLUMN_MODEL +" text,"
			      + COLUMN_TYPE +" text,"
			      + COLUMN_DIRECTORY + " text not null"
				+ ");"; 
	}
	
	public abstract String getTableName();
	
	@Override
	public void onCreate(SQLiteDatabase arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
	}

}
