package uw.cse.mag.appliancereader.db;

import java.util.ArrayList;
import java.util.List;

import uw.cse.mag.appliancereader.datatype.Appliance;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.widget.SearchViewCompatIcs.MySearchView;

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
	
	private SQLiteDatabase mDB;
	private ApplianceSQLiteHelper mUserHelper;
	
	private String[] allColumns = {
			ApplianceSQLiteHelper.COLUMN_ID,
			ApplianceSQLiteHelper.COLUMN_NICKNAME,
			ApplianceSQLiteHelper.COLUMN_MAKE,
			ApplianceSQLiteHelper.COLUMN_MODEL,
			ApplianceSQLiteHelper.COLUMN_DIRECTORY
	};
	
	public ApplianceDataSource(ApplianceSQLiteHelper helper){
		if (helper == null)
			throw new IllegalArgumentException("[ApplianceDataSource] Passed in Appliance SQLite Helper " +
					"cannot be null");
		mUserHelper = helper;
	}
	
	/**
	 * Open this datasource allowing it to be written
	 */
	public void open() throws SQLException {
		try {
			mDB = mUserHelper.getWritableDatabase();
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
	
	/**
	 * 
	 * @param a appliance to add to the data base
 	 * @return Appliance that was stored inside the database
	 */
	public Appliance createAppliance(Appliance a){
		
		// Store images in the file system
		
		// Then attempt to write the 
//		ContentValues values = applianceToContentValues(a);
		values.put(ApplianceSQLiteHelper.COLUMN_DIRECTORY, a.getDirectoryPath());
		
		long insertId = mDB.insert()
		
		// Check if error occured
		if (a.getID() == -1L){
			return null;
		}
		return null;
		
		
	}
	
	/**
	 * Delete Appliance from and all knowledge of it  
	 * @param a Appliance 
	 */
	public void deleteAppliance(Appliance a){
		// Delete Appliance from the data base
		long id = a.getID();
		mDB.delete(mUserHelper.getTableName(), ApplianceSQLiteHelper.COLUMN_ID
		        + " = " + id, null);
		
		// Then obtain the directory
		String dir = 	
		// Delete the directory from external storage
		
	}

	/**
	 * Return all the appliance of this table
	 * @return list of appliances
	 */
	public List<Appliance> getAllAppliances(){
		List<Appliance> appliances = new ArrayList<Appliance>();
		
		// Obtain the cursor to navigate the table
		Cursor c = mDB.query(mUserHelper.getTableName(),
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
	
	private ContentValues applianceToContentValues(Appliance a){
		ContentValues values = new ContentValues();
		values.put(ApplianceSQLiteHelper.COLUMN_NICKNAME, a.getNickname());
		values.put(ApplianceSQLiteHelper.COLUMN_MAKE, a.getMake());
		values.put(ApplianceSQLiteHelper.COLUMN_MODEL, a.getModel());
		
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
}
