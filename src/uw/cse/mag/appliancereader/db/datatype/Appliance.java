package uw.cse.mag.appliancereader.db.datatype;

import uw.cse.mag.appliancereader.db.ApplianceNotExistException;
import uw.cse.mag.appliancereader.db.FileManager;
import uw.cse.mag.appliancereader.imgproc.Size;
import uw.cse.mag.appliancereader.util.ImageIO;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

/**
 * Appliance object that defines a Electrical Appliance that has a digitial display
 * @author mhotan
 */
public class Appliance {

	private static final String TAG = Appliance.class.getSimpleName();
	
	// TODO Finalize Abstract functions and Representation Invariants
	/**
	 * This is a key for storing bundles in intents that represent appliances
	 */
	public static final String KEY_BUNDLE_APPLIANCE = "KEY_APPLIANCE_BUNDLE";
	
	/**
	 * Database ID for this appliance
	 */
	private long mId;
	
	/**
	 * Nick Name that user can choose for the appliance
	 * IE "My Microwave" 
	 */
	private String mNickName;
	
	/**
	 * Make of the Appliance
	 * IE "Samsung"
	 */
	private String mMake;
	
	/**
	 * Model of Appliance
	 */
	private String mModel;
	
	/**
	 * Type of appliance
	 */
	private String mType;
	
	/**
	 * Directory where all the files are stored
	 */
	private String mDirectory;
	
	/**
	 * Directory that stores appliance features
	 */
	private ApplianceFeatures mFeatures;
	
	private final FileManager mFileManager;
	
	public Appliance(){
		mId = -1;
		mFileManager = FileManager.getInstance();
	}
	
	/*
	 * Setters or modifiers
	 * */
	
	public void setId(long id){
		this.mId = id;
	}
	
	public void setNickName(String nickname){
		this.mNickName = nickname;
	}
	
	public void setMake(String make){
		this.mMake = make;
	}
	
	public void setModel(String model){
		this.mModel = model;
	}
	
	public void setType(String type) {
		this.mType = type;
	}
	
	public void setApplianceFeatures(ApplianceFeatures af){
		if (af == null) return;
		this.mFeatures = af;
		if (mFileManager.hasAppliance(this)) {
			try {
				mFileManager.addXMLFile(this, af);
			} catch (ApplianceNotExistException e) {
				Log.e(TAG, "Error with file manager recognizing the appliane exists");
			}
		}
	}
	
	public void setDirectoryPath(String directoryPath){
		this.mDirectory = directoryPath;
	}
	
	/**
	 * Scales down all the feature point by a particular value
	 * Scales down all the features
	 * @param scaleFactor int factor to scale points down by
	 */
	public void scaleDownFeatures(float scaleFactor) {
		if (mFeatures == null) {
			Log.w(TAG, "No features found to scale");
		} else {
			mFeatures.scaleDownFeatures(scaleFactor);
		}
	}
	
	/*
	 * Getters
	 * */
	
	public long getID(){
		return mId;
	}

	public String getNickname(){
		return mNickName;
	}
	
	public String getMake(){
		return mMake;
	}
	
	public String getModel(){
		return mModel;
	}
	
	public String getType(){
		return mType;
	}
	
	public boolean hasApplianceFeatures(){
		if (mFeatures == null)
			return false;
		return !mFeatures.isEmpty();
	}
	
	public ApplianceFeatures getApplianceFeatures(){
		return mFeatures;
	}
	
	public String getDirectoryPath(){
		return mDirectory;
	}
	
	public String toString(){
		if (mNickName != null)
			return mNickName;
		if (mMake != null && mModel != null )
			return mMake + "_" + mModel;
		if (mType != null) 
			return "Unknown " + mType;
		return null;
	}
	
	private static final String BUNDLE_ID = "APP_BUN_ID";
	private static final String BUNDLE_NICKNAME = "APP_BUN_NICKNAME";
	private static final String BUNDLE_MAKE = "APP_BUN_MAKE";
	private static final String BUNDLE_MODEL = "APP_BUN_MODEL";
	private static final String BUNDLE_TYPE = "APP_BUN_TYPE";
	private static final String BUNDLE_DIR = "APP_BUN_DIRECTORY";
	
	public Bundle toBundle(){
		Bundle b = new Bundle();
		b.putLong(BUNDLE_ID, mId);
		b.putString(BUNDLE_NICKNAME, mNickName);
		b.putString(BUNDLE_MAKE, mMake);
		b.putString(BUNDLE_MODEL, mModel);
		b.putString(BUNDLE_TYPE, mType);
		b.putString(BUNDLE_DIR, mDirectory);
		return b;
	}
	
	/**
	 * Returns null 
	 * @param b
	 * @return null if Bundle doesn't represent appliance, Apliance otherwise 
	 */
	public static Appliance toAppliance(Bundle b){
		if (b == null) return null;
		long id = b.getLong(BUNDLE_ID, -1);
		if (id == -1L) return null;
		Appliance a = new Appliance();
		a.setId(id);
		a.setNickName(b.getString(BUNDLE_NICKNAME));
		a.setMake(b.getString(BUNDLE_MAKE));
		a.setModel(b.getString(BUNDLE_MODEL));
		a.setType(b.getString(BUNDLE_TYPE));
		a.setDirectoryPath(b.getString(BUNDLE_DIR));
		
		// Attempt to get and reload the appliance features
		FileManager f = FileManager.getInstance();
		ApplianceFeatures af = f.getFeatures(a);
		a.mFeatures = af;
		return a;
	}

	/**
	 * Returns Configuration.ORIENTATION_LANDSCAPE, or Configuration.ORIENTATION_PORTRAIT
	 * @return orientation of image
	 */
	public int getRefimageOrientation() {
		String path = mFileManager.getReferenceImage(this);
		return ImageIO.getOrientationOfImage(path);
	}

	/** 
	 * @return size of reference image
	 */
	public Size getSizeOfRefImage() {
		return ImageIO.getSizeOfImage(mFileManager.getReferenceImage(this));
	}

	/**
	 * 
	 * @param actualDimension
	 * @return
	 */
	public Bitmap getReferenceImage(Size dimensions) {
		return ImageIO.loadBitmapFromFilePath(mFileManager.getReferenceImage(this), dimensions);
	}
}

