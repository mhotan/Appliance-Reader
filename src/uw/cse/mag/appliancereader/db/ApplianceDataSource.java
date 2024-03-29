package uw.cse.appliance.database.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;

public class ApplianceDataSource {

	private static final String TAG = ApplianceDataSource.class.getSimpleName();

	private SQLiteDatabase database;
	private ApplianceSQlliteHelper dbHelper;
	private String[] allColumns = { // These are maintained in the same order 
			ApplianceSQlliteHelper.COLUMN_ID,
			ApplianceSQlliteHelper.COLUMN_NICKNAME,
			ApplianceSQlliteHelper.COLUMN_DIRECTORY,
			ApplianceSQlliteHelper.COLUMN_MAKE,
			ApplianceSQlliteHelper.COLUMN_MODEL,
			ApplianceSQlliteHelper.COLUMN_TYPE
	};

	/**
	 * Creates a new data source to load new Appliance into this 
	 * applications database
	 * @param context
	 */
	public ApplianceDataSource(Context context) {
		dbHelper = new ApplianceSQlliteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public Appliance createAppliance(String nickName, String make, 
			String model, String type, String dir){
		if (dir == null)
			throw new IllegalArgumentException("Directory path cannot be null");
		File f = new File(dir);
		if (!f.isDirectory()) 
			throw new IllegalArgumentException("Path leads to a non directory");
		
		ContentValues values = new ContentValues();
		values.put(ApplianceSQlliteHelper.COLUMN_NICKNAME, nickName);
		values.put(ApplianceSQlliteHelper.COLUMN_DIRECTORY, dir);
		values.put(ApplianceSQlliteHelper.COLUMN_MAKE, make);
		values.put(ApplianceSQlliteHelper.COLUMN_MODEL, model);
		values.put(ApplianceSQlliteHelper.COLUMN_TYPE, type);

		long insertId = database.insert(ApplianceSQlliteHelper.TABLE_APPLIANCE,
				null, values);
		Cursor cursor = database.query(ApplianceSQlliteHelper.TABLE_APPLIANCE,
				allColumns, ApplianceSQlliteHelper.COLUMN_ID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		Appliance newappliance = cursorToAppliance(cursor);
		cursor.close();
		return newappliance;
	}

	/**
	 * Creates an Appliance where the nickname cannot be null or both
	 * make and model can't be null
	 * @param nickName Nickname of appliance
	 * @param make make of appliance
	 * @param model model number or name of appliance
	 * @param type Type of appliance used
	 * @param directory
	 * @return
	 * @throws IllegalArgumentException 
	 */
	public Appliance createAppliance(String nickName, String make, 
			String model, String type, Bitmap b) {
		// Assert that the name either the name or make and model are not null
		// and Bitmap image is not null
		String directoryName;
		if (nickName != null) 
			directoryName = nickName;
		else if (make != null && model != null)
			directoryName = make + "_" + model;
		else 
			throw new IllegalArgumentException("Illegal name state Nickname: " + nickName + 
					" Make: " + make + " Model: " + model);

		// TODO Undo Bitmap
		//		if (b == null)
		//			throw new IllegalArgumentException("Bitmap argument cannot be null");

		// TODO save the image and get the path where the home directory is
		// FileManager save appliance with Bitmap and Appliance Name
		// DEBUG  
		String directoryPath = "ddddddddddddddddddddddddddddddddddddddddd" +
				"dddddddddddddddddddsddddddddddddddddddddd" +
				"dddddddddddddddddddsddddddddddddddddddddd" +
				"dddddddddddddddddddsddddddddddddddddddddd" +
				"dddddddddddddddddddsddddddddddddddddddddd" +
				"dddddddddddddddddddsddddddddddddddddddddd" +
				"dddddddddddddddddddsddddddddddddddddddddd" +
				"dddddddddddddddddddsddddddddddddddddddddd" +
				"dddddddddddddddddddsddddddddddddddddddddd";

		ContentValues values = new ContentValues();
		values.put(ApplianceSQlliteHelper.COLUMN_NICKNAME, nickName);
		values.put(ApplianceSQlliteHelper.COLUMN_DIRECTORY, directoryPath);
		values.put(ApplianceSQlliteHelper.COLUMN_MAKE, make);
		values.put(ApplianceSQlliteHelper.COLUMN_MODEL, model);
		values.put(ApplianceSQlliteHelper.COLUMN_TYPE, type);

		long insertId = database.insert(ApplianceSQlliteHelper.TABLE_APPLIANCE,
				null, values);
		Cursor cursor = database.query(ApplianceSQlliteHelper.TABLE_APPLIANCE,
				allColumns, ApplianceSQlliteHelper.COLUMN_ID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		Appliance newappliance = cursorToAppliance(cursor);
		cursor.close();
		return newappliance;
	}

	public void deleteAppliance(Appliance appliance) {
		long id = appliance.getID();
		Log.i(TAG, "Appliance deleted with id: " + id);
		database.delete(ApplianceSQlliteHelper.TABLE_APPLIANCE,
				ApplianceSQlliteHelper.COLUMN_ID + " = " + id, null);
	}

	public List<Appliance> getAllAppliances() {
		List<Appliance> comments = new ArrayList<Appliance>();

		Cursor cursor = database.query(ApplianceSQlliteHelper.TABLE_APPLIANCE,
				allColumns, null, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Appliance appliance = cursorToAppliance(cursor);
			comments.add(appliance);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return comments;
	}

	private Appliance cursorToAppliance(Cursor cursor) {
		Appliance appliance = new Appliance();
		appliance.setId(cursor.getLong(0));
		appliance.setNickName(cursor.getString(1));
		appliance.setDirectoryPath(cursor.getString(2));
		appliance.setMake(cursor.getString(3));
		appliance.setModel(cursor.getString(4));
		appliance.setType(cursor.getString(5));
		return appliance;
	}
}
