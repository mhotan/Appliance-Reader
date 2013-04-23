package uw.cse.mag.appliancereader.db;

import java.util.ArrayList;
import java.util.List;

import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.datatype.ApplianceFeatures;
import uw.cse.mag.appliancereader.imgproc.Size;
import uw.cse.mag.appliancereader.util.ImageIO;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
	//	private static final Logger log = Logger.getLogger(ApplianceDataSource.class.getSimpleName());

	private SQLiteDatabase mDB;
	private final Context mCtx;
	private DatabaseHelper mDbHelper;
	private final FileManager mFileManager;

	private String mTableName;
	
	private String[] allColumns = {
			ApplianceDBAdapter.COLUMN_ID,
			ApplianceDBAdapter.COLUMN_NICKNAME,
			ApplianceDBAdapter.COLUMN_MAKE,
			ApplianceDBAdapter.COLUMN_MODEL,
			ApplianceDBAdapter.COLUMN_TYPE,
			ApplianceDBAdapter.COLUMN_DIRECTORY
	};

	/**
	 * For a table name found in 
	 * @param ctx
	 * @param tableName
	 */
	public ApplianceDataSource(Context ctx, String tableName){
		mCtx = ctx;
		mTableName = tableName;
		mFileManager = FileManager.getInstance();
	}

	/**
	 * Open this datasource allowing it to be written
	 */
	public ApplianceDataSource open() throws SQLException {
		this.mDbHelper = new DatabaseHelper(this.mCtx);
		this.mDB = this.mDbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Closes current connection
	 */
	public void close(){
		this.mDbHelper.close();
	}

	private void checkDatabase() throws DatabaseNotInitializedException{
		if (mDB == null)
			throw new DatabaseNotInitializedException();
	}

	/**
	 * Creates an appliance and adds it to this Database
	 * @param a appliance to add to the data base
	 * @return Appliance that was stored inside the database
	 * @throws DatabaseNotInitializedException When Database was not Opened before this call
	 */
	public Appliance createAppliance(Appliance a, Bitmap b) throws DatabaseNotInitializedException{
		checkDatabase();

		// Then attempt to write the 
		ContentValues values = applianceToContentValues(a);
		long insertId = mDB.insert(mTableName, null, values);
		Cursor cursor = mDB.query(mTableName, allColumns, ApplianceDBAdapter.COLUMN_ID + " = " + insertId,
				null, null, null, null);
		cursor.moveToFirst();
		Appliance newApp = cursorToAppliance(cursor);
		cursor.close();
		
		// Create a file directory for this appliance
		a = mFileManager.addAppliance(newApp);
		// Save the image as the reference image of this bitmap
		saveApplianceReferenceImage(a, b);
		
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
	 * Delete Appliance from and all knowledge of it  
	 * @param a Appliance to delete
	 * @throws DatabaseNotInitializedException When Database was not Opened before this call
	 */
	public void deleteAppliance(Appliance a) throws DatabaseNotInitializedException{
		checkDatabase();

		// Delete Appliance from the data base
		long id = a.getID();
		mDB.delete(mTableName, ApplianceDBAdapter.COLUMN_ID
				+ " = " + id, null);
	}

	/**
	 * Returns the size of the given image that is the reference
	 * for appliance app.
	 * 
	 * @param app appliance to find the size of
	 * @return Size of the reference image
	 * @throws ApplianceNotExistException
	 */
	public Size getSizeOfRefImage(Appliance app) 
			throws ApplianceNotExistException {
		// Obtain the file path for the image
		String imagePath = mFileManager.getReferenceImage(app);
		if (imagePath == null)
			throw new ApplianceNotExistException(app);	
		return ImageIO.getSizeOfImage(imagePath);
	}

	/**
	 * Returns the reference image for this appliance 
	 * If dim is non null then the appliance will be scaled to best match that 
	 * dimension.
	 * 
	 * If dim is null then the original image will be returned.
	 * 
	 * @param app appliance that is stored 
	 * @param dim Requested dimension of appliance images
	 * @return null if appliance does not exist, 
	 * @throws ApplianceNotExistException 
	 */
	public Bitmap getReferenceImage(Appliance app, Size dim) 
			throws ApplianceNotExistException {
		// Obtain the file path for the image
		String imagePath = mFileManager.getReferenceImage(app);
		if (imagePath == null)
			throw new ApplianceNotExistException(app);

		return ImageIO.loadBitmapFromFilePath(imagePath, dim);
	}

	/**
	 * For a given appliance return Configuration.ORIENTATION_LANDSCAPE
	 * for a landscape reference oriented image.  Returns Configuration.ORIENTATION_PORTRAIT
	 * for a portrait image 
	 * @param app appliance in question
	 * @return configuration of this appliance
	 * @throws ApplianceNotExistException 
	 */
	public int getRefimageOrientation(Appliance app) 
			throws ApplianceNotExistException {
		// Obtain the file path for the image
		String imagePath = mFileManager.getReferenceImage(app);
		if (imagePath == null)
			throw new ApplianceNotExistException(app);

		// Get image orientation
		return ImageIO.getOrientationOfImage(imagePath);
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
		Cursor c = mDB.query(mTableName,
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

	/**
	 * Returns whether there is any Appliances in this local data store
	 * @return true if there exists any appliance false other wise
	 * @throws DatabaseNotInitializedException 
	 */
	public boolean hasAppliances() throws DatabaseNotInitializedException {
		return !getAllAppliances().isEmpty();
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

	public class DatabaseNotInitializedException extends Exception {

		private static final long serialVersionUID = -6690165807139149242L;

		public DatabaseNotInitializedException(){
			super("Database is not initialized. Must call <Datasource instance>.open()");
		}
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
}
