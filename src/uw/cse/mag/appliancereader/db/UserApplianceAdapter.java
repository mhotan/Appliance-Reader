package uw.cse.mag.appliancereader.db;

import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.datatype.ApplianceFeatures;
import uw.cse.mag.appliancereader.db.ApplianceDataSource.DatabaseNotInitializedException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;

public class UserApplianceAdapter {

	private static final String TAG = UserApplianceAdapter.class.getSimpleName();
	
	private static final String TABLE_NAME = ApplianceDBAdapter.USER_TABLE;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private final Context mCtx;
	private final FileManager mFileManager;
	
	private String[] allColumns = {
			ApplianceDBAdapter.COLUMN_ID,
			ApplianceDBAdapter.COLUMN_NICKNAME,
			ApplianceDBAdapter.COLUMN_MAKE,
			ApplianceDBAdapter.COLUMN_MODEL,
			ApplianceDBAdapter.COLUMN_TYPE,
			ApplianceDBAdapter.COLUMN_DIRECTORY
	};
	
	 /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx
     *            the Context within which to work
     */
    public UserApplianceAdapter(Context ctx) {
        this.mCtx = ctx;
        mFileManager = FileManager.getInstance();
    }

    /**
     * Open the cars database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException
     *             if the database could be neither opened or created
     */
    public UserApplianceAdapter open() throws SQLException {
        this.mDbHelper = new DatabaseHelper(this.mCtx);
        this.mDb = this.mDbHelper.getWritableDatabase();
        return this;
    }

    /**
     * close return type: void
     */
    public void close() {
        this.mDbHelper.close();
    }
	
    /**
	 * Creates an appliance and adds it to this Database
	 * @param a appliance to add to the data base
	 * @return Appliance that was stored inside the database
	 * @throws DatabaseNotInitializedException When Database was not Opened before this call
	 */
	public Appliance createAppliance(Appliance a, Bitmap b) throws DatabaseNotInitializedException{
//		checkDatabase();

		// Create a file directory for this appliance
		a = mFileManager.addAppliance(a);
		// Save the image as the reference image of this bitmap
		saveApplianceReferenceImage(a, b);

		// Then attempt to write the 
		ContentValues values = applianceToContentValues(a);
		long insertId = mDb.insert(TABLE_NAME, null, values);
		Cursor cursor = mDb.query(TABLE_NAME, 
				allColumns, ApplianceDBAdapter.COLUMN_ID + " = " + insertId,
				null, null, null, null);
		cursor.moveToFirst();
		Appliance newApp = cursorToAppliance(cursor);
		cursor.close();
		return newApp;
	}
	
	/**
	 * Save appliance features to this appliace
	 * @param a Appliance to be saved to
	 * @param features features to save
	 * @return true if appliance exist and a file
	 */
	public boolean saveApplianceFeatures(Appliance a, ApplianceFeatures features){
		try {
			a.setApplianceFeatures(features);
			mFileManager.addXMLFile(a, features);
		} catch (ApplianceNotExistException e) {
			Log.w(TAG, "Unable to load appliance features Exception: " + e);
			return false;
		}
		return true;
	}

	/**
	 * Save the appliance for later reference
	 * 
	 * @param a Appliance to save reference image to
	 * @param b bitmap to save as reference image for appliance a
	 * @return true if appliance was saved or false if appliance did not exist
	 */
	private boolean saveApplianceReferenceImage(Appliance a, Bitmap b){
		try {
			mFileManager.setReferenceImage(a, b);
		} catch (ApplianceNotExistException e) {
			Log.w(TAG, "Unable to load appliance features");
			return false;
		}
		return true;
	}
    
    
    /**
     * Helper class for accessing data
     * @author mhotan
     *
     */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		DatabaseHelper(Context context) {
			super(context, ApplianceDBAdapter.DATABASE_NAME, null, ApplianceDBAdapter.DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	/**
	 * 
	 * @param a Appliance to make into ContentValues
	 * @return ContentValues with key value associations
	 */
	private ContentValues applianceToContentValues(Appliance a){
		ContentValues values = new ContentValues();
		values.put(ApplianceDBAdapter.COLUMN_NICKNAME, a.getNickname());
		values.put(ApplianceDBAdapter.COLUMN_MAKE, a.getMake());
		values.put(ApplianceDBAdapter.COLUMN_MODEL, a.getModel());
		values.put(ApplianceDBAdapter.COLUMN_TYPE, a.getType());
		values.put(ApplianceDBAdapter.COLUMN_DIRECTORY, a.getDirectoryPath());
		return values;
	}

	/**
	 * 
	 * @param c
	 * @return
	 */
	private Appliance cursorToAppliance(Cursor c){
		Appliance a = new Appliance();
		a.setId(c.getLong(0));
		a.setNickName(c.getString(1));
		a.setMake(c.getString(2));
		a.setModel(c.getString(3));
		a.setDirectoryPath(c.getString(4));
		return a;
	}
	
}
