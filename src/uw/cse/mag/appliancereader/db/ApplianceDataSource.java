package uw.cse.mag.appliancereader.db;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.datatype.ApplianceFeatures;
import uw.cse.mag.appliancereader.db.FileManager.ApplianceNotExistException;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * Data model that allows access to Appliance Databases
 * Provides direct and simple interface for saving to the database
 * 
 *  Its responsibilities include
 *  	maintain database connection
 *  	supports ading new appliance and fetching
 * @author mhotan
 */
public class ApplianceDataSource {

	private static final String TAG = ApplianceDataSource.class.getSimpleName();
	private static final Logger log = Logger.getLogger(ApplianceDataSource.class.getSimpleName());
	
	private SQLiteDatabase mDB;
	private ApplianceSQLiteHelper mSQLHelper;

	private final FileManager mFileManager;

	private String[] allColumns = {
			ApplianceSQLiteHelper.COLUMN_ID,
			ApplianceSQLiteHelper.COLUMN_NICKNAME,
			ApplianceSQLiteHelper.COLUMN_MAKE,
			ApplianceSQLiteHelper.COLUMN_MODEL,
			ApplianceSQLiteHelper.COLUMN_TYPE,
			ApplianceSQLiteHelper.COLUMN_DIRECTORY
	};

	public ApplianceDataSource(ApplianceSQLiteHelper helper){
		if (helper == null)
			throw new IllegalArgumentException("[ApplianceDataSource] Passed in Appliance SQLite Helper " +
					"cannot be null");
		mSQLHelper = helper;
		mFileManager = FileManager.getInstance();
	}

	/**
	 * Open this datasource allowing it to be written
	 */
	public void open() throws SQLException {
		try {
			mDB = mSQLHelper.getWritableDatabase();
		} catch (SQLException e){
			mDB = null;
			throw e;
		}
	}

	/**
	 * Closes current connection
	 */
	public void close(){
		// Nothing to close
		if (mDB == null) return;
		mDB.close();
		mDB = null; // Safety check
	}

	private void checkDatabase() throws DatabaseNotInitializedException{
		if (mDB == null)
			throw new DatabaseNotInitializedException();
	}

	/**
	 * 
	 * @param a appliance to add to the data base
	 * @return Appliance that was stored inside the database
	 * @throws DatabaseNotInitializedException When Database was not Opened before this call
	 */
	public Appliance createAppliance(Appliance a) throws DatabaseNotInitializedException{
		checkDatabase();

		// Create a file directory for this appliance
		a = mFileManager.addAppliance(a);

		// Then attempt to write the 
		ContentValues values = applianceToContentValues(a);
		long insertId = mDB.insert(
				mSQLHelper.getTableName(), null, values);
		Cursor cursor = mDB.query(mSQLHelper.getTableName(), 
				allColumns, ApplianceSQLiteHelper.COLUMN_ID + " = " + insertId,
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
			mFileManager.addXMLFile(a, features);
		} catch (ApplianceNotExistException e) {
			Log.w(TAG, "Unable to load appliance features Exception: " + e);
			return false;
		}
		return true;
	}


	public boolean saveApplianceReferenceImage(Appliance a, Bitmap b){
		try {
			mFileManager.setReferenceImage(a, b);
		} catch (ApplianceNotExistException e) {
			Log.w(TAG, "Unable to load appliance features");
			return false;
		}
		return true;
	}

	/**
	 * Delete Appliance from and all knowledge of it  
	 * @param a Appliance to delete
	 * @throws DatabaseNotInitializedException When Database was not Opened before this call
	 */
	public void deleteAppliance(Appliance a) throws DatabaseNotInitializedException{
		checkDatabase();

		// Delete Appliance from the data base
		long id = a.getID();
		mDB.delete(mSQLHelper.getTableName(), ApplianceSQLiteHelper.COLUMN_ID
				+ " = " + id, null);
	}

	/**
	 * Return all the appliance of this table
	 * @return list of appliances
	 * @throws DatabaseNotInitializedException When Database was not Opened before this call
	 */
	public List<Appliance> getAllAppliances() throws DatabaseNotInitializedException{
		checkDatabase();

		List<Appliance> appliances = new ArrayList<Appliance>();

		// Obtain the cursor to navigate the table
		Cursor c = mDB.query(mSQLHelper.getTableName(),
				allColumns, null, null, null, null, null);

		c.moveToFirst(); // Initialize the cursor
		while (!c.isAfterLast()){
			Appliance a = cursorToAppliance(c);
			appliances.add(a);
			c.moveToNext();
		}

		c.close();
		return appliances;
	}

	public boolean hasAppliances() {
		Cursor mCursor = mDB.rawQuery("SELECT * FROM " + mSQLHelper.getTableName(), null);
		if (mCursor.moveToFirst())
			return true;
		return false;
	}

	/**
	 * 
	 * @param a Appliance to make into ContentValues
	 * @return ContentValues with key value associations
	 */
	private ContentValues applianceToContentValues(Appliance a){
		ContentValues values = new ContentValues();
		values.put(ApplianceSQLiteHelper.COLUMN_NICKNAME, a.getNickname());
		values.put(ApplianceSQLiteHelper.COLUMN_MAKE, a.getMake());
		values.put(ApplianceSQLiteHelper.COLUMN_MODEL, a.getModel());
		values.put(ApplianceSQLiteHelper.COLUMN_TYPE, a.getType());
		values.put(ApplianceSQLiteHelper.COLUMN_DIRECTORY, a.getDirectoryPath());
		return values;
	}

	private Appliance cursorToAppliance(Cursor c){
		Appliance a = new Appliance();
		a.setId(c.getLong(0));
		a.setNickName(c.getString(1));
		a.setMake(c.getString(2));
		a.setModel(c.getString(3));
		a.setDirectoryPath(c.getString(4));
		return a;
	}

	public class DatabaseNotInitializedException extends Exception {

		private static final long serialVersionUID = -6690165807139149242L;

		public DatabaseNotInitializedException(){
			super("Database is not initialized. Must call <Datasource instance>.open()");
		}
	}
}
