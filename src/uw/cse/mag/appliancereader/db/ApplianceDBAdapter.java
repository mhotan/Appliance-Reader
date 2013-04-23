package uw.cse.mag.appliancereader.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * General Skeleton to create a SQLLite Open helper
 * This database provides a general interface to hold application 
 * specific information about appliances 
 * @author mhotan
 */
public class ApplianceDBAdapter  {

	private static final String TAG = ApplianceDBAdapter.class.getSimpleName();
	
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
	private DatabaseHelper mDBHelper;
	private SQLiteDatabase db;
	private final Context mContext;
	
	// Table Columns
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NICKNAME = "name";
	public static final String COLUMN_MAKE = "make";
	public static final String COLUMN_MODEL = "model";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_DIRECTORY = "directory";

	public static final String USER_TABLE = "userappliances";
	public static final String DEFAULT_TABLE = "defaultappliances"; 
	
	// One table for user 
	private static final String CREATE_TABLE_USER = "create table " + USER_TABLE + "(" 
			+ COLUMN_ID + " integer primary key autoincrement, " 
			+ COLUMN_NICKNAME + " text not null, "
		      + COLUMN_MAKE +" text, "
		      + COLUMN_MODEL +" text, "
		      + COLUMN_TYPE +" text, "
		      + COLUMN_DIRECTORY + " text not null"
			+ ");"; 
	
	// One table for default appliances
	private static final String CREATE_TABLE_DEFAULT = "create table " + DEFAULT_TABLE + "(" 
			+ COLUMN_ID + " integer primary key autoincrement, " 
			+ COLUMN_NICKNAME + " text not null, "
		      + COLUMN_MAKE +" text, "
		      + COLUMN_MODEL +" text, "
		      + COLUMN_TYPE +" text, "
		      + COLUMN_DIRECTORY + " text not null"
			+ ");"; 
	
	/**
	 * Create an SQl Lite open helper to access database tables
	 * @param context
	 */
	public ApplianceDBAdapter(Context context) {
		mContext = context;
		mDBHelper = new DatabaseHelper(mContext);
	}

//	/**
//	 * This method was made to handle subclass request to create different tables
//	 * in the same database.  
//	 * Generates the Data Base creaet sql command in String form
//	 * @return
//	 */
//	protected String getSQLDataBaseCreate(){
//		String tableName = mTableName;
//		return "create table " + tableName + " (" 
//				+ COLUMN_ID + " integer primary key autoincrement, " 
//				+ COLUMN_NICKNAME + " text not null,"
//			      + COLUMN_MAKE +" text,"
//			      + COLUMN_MODEL +" text,"
//			      + COLUMN_TYPE +" text,"
//			      + COLUMN_DIRECTORY + " text not null"
//				+ ");"; 
//	}
//	
//	@Override
//	public void onCreate(SQLiteDatabase database) {
//		Log.d(TAG, "Creating a new database");
//		database.execSQL(getSQLDataBaseCreate());
//	}

	/**
     * open the db
     * @return this
     * @throws SQLException
     * return type: DBAdapter
     */
    public ApplianceDBAdapter open() throws SQLException 
    {
        this.db = this.mDBHelper.getWritableDatabase();
        return this;
    }

    /**
     * close the db 
     * return type: void
     */
    public void close() 
    {
        this.mDBHelper.close();
    }

	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_USER.toUpperCase());
			db.execSQL(CREATE_TABLE_DEFAULT.toUpperCase());
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + DEFAULT_TABLE);
			onCreate(db);
		}
	}
	
	
	
}
