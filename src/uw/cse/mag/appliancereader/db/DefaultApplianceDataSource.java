package uw.cse.mag.appliancereader.db;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uw.cse.mag.appliancereader.db.datatype.Appliance;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;

public class DefaultApplianceDataSource {
	private static final String TAG = DefaultApplianceDataSource.class.getSimpleName();

	private SQLiteDatabase database;
	private DefaultApplianceHelper dbHelper;
	private FileManager filemanager;
	private String[] allColumns = { // These are maintained in the same order 
			DefaultApplianceHelper.COLUMN_ID,
			DefaultApplianceHelper.COLUMN_NICKNAME,
			DefaultApplianceHelper.COLUMN_DIRECTORY,
			DefaultApplianceHelper.COLUMN_MAKE,
			DefaultApplianceHelper.COLUMN_MODEL,
			DefaultApplianceHelper.COLUMN_TYPE
	};

	/**
	 * Creates a new data source to load new Appliance into this 
	 * applications database
	 * @param context
	 */
	public DefaultApplianceDataSource(Context context) {
		dbHelper = new DefaultApplianceHelper(context);
		filemanager = FileManager.getInstance();
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

		ContentValues values = new ContentValues();
		values.put(DefaultApplianceHelper.COLUMN_NICKNAME, nickName);
		values.put(DefaultApplianceHelper.COLUMN_DIRECTORY, dir);
		values.put(DefaultApplianceHelper.COLUMN_MAKE, make);
		values.put(DefaultApplianceHelper.COLUMN_MODEL, model);
		values.put(DefaultApplianceHelper.COLUMN_TYPE, type);

		long insertId = database.insert(DefaultApplianceHelper.TABLE_APPLIANCE,
				null, values);
		Cursor cursor = database.query(DefaultApplianceHelper.TABLE_APPLIANCE,
				allColumns, DefaultApplianceHelper.COLUMN_ID + " = " + insertId, null,
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

		if (b == null)
			throw new IllegalArgumentException("Bitmap argument cannot be null");

		Appliance a = new Appliance();
		a.setNickName(nickName);
		a.setMake(make);
		a.setModel(model);
		a.setType(type);

		// FileManager save appliance with Bitmap and Appliance Name
		// DEBUG 
		String directoryPath;
		try {
			directoryPath = filemanager.addAppliance(a);
			a.setDirectoryPath(directoryPath);
			filemanager.setReferenceImage(a, b);
		} catch (IOException e) {
			Log.e(TAG, "Unable to create directory for appliance named: " + a);
			return null;
		} catch (ApplianceNotExistException e) {
			Log.e(TAG, "Unable correctly store appliance: " + a);
			return null;
		}

		return createAppliance(a.getNickname(), a.getMake(), a.getModel(), a.getType(), directoryPath);
	}

	public void deleteAppliance(Appliance appliance) {
		long id = appliance.getID();
		Log.i(TAG, "Appliance deleted with id: " + id);
		database.delete(DefaultApplianceHelper.TABLE_APPLIANCE,
				DefaultApplianceHelper.COLUMN_ID + " = " + id, null);
	}

	public boolean hasAppliances() {
		return getAllAppliances().size() > 0;
	}

	public List<Appliance> getAllAppliances() {
		List<Appliance> comments = new ArrayList<Appliance>();

		Cursor cursor = database.query(DefaultApplianceHelper.TABLE_APPLIANCE,
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
