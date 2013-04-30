package uw.cse.mag.appliancereader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserApplianceHelper extends SQLiteOpenHelper {

	public static final String TABLE_APPLIANCE = "userappliances";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NICKNAME = "name";
	public static final String COLUMN_MAKE = "make";
	public static final String COLUMN_MODEL = "model";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_DIRECTORY = "directory";
	
	private static final String DATABASE_NAME = "appliances.db";
	private static final int DATABASE_VERSION = 1;
	
	static final String DATABASE_CREATE = "CREATE TABLE "
			+ TABLE_APPLIANCE + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
			+ COLUMN_NICKNAME + " TEXT," 
			+ COLUMN_DIRECTORY + " TEXT NOT NULL,"
			+ COLUMN_MAKE + " TEXT,"
			+ COLUMN_MODEL + " TEXT,"
			+ COLUMN_TYPE + " TEXT"
			+ ");"; 
	
	/**
	 * Creates a class that handles the SQL queries
	 * @param ctx
	 */
	public UserApplianceHelper(Context ctx) {
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		database.execSQL(DefaultApplianceHelper.DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(UserApplianceHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPLIANCE);
		onCreate(db);
	}
}
