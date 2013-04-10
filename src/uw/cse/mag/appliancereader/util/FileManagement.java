package uw.cse.mag.appliancereader.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uw.cse.mag.appliancereader.datatype.XMLTestImageSet;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

/**
 * As of right now the directory layout of a particular appliance includes
 * <b>
 * <b>For an Appliance "A" The layout class
  	<b>
  	<b>-ApplianceReader
  	<b>--Appliances
	<b>---A
	<b>----TODO: Add meta data including the ID 
	<b>----reference image directory
	<b>----other images (optional)
	<b>----XML file directory (Thing that describes all the features)
 * <b>
 * Thread safe class for file management Read and Write access
 * <b>This class is specific for Appliance Reader
 * storage
 * @author mhotan
 *
 */
public class FileManagement {
	private static final String TAG = FileManagement.class.getSimpleName();
	
	// Directories
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ApplianceReaderSpecific/";
	public static final String APPLIANCES_PATH = DATA_PATH + "Appliances/";
	
	// Sub directories of a given appliance
	private static final String REFERENCE_IMAGES_DIR = "REF-Images/";
	private static final String OTHER_IMAGES_DIR = "OTHER-Images/";
	private static final String XML_FILE_DIR = "XML-File/";
	private static final String[] APPLIANCE_SUBDIRECTORIES = 
			new String[]{REFERENCE_IMAGES_DIR, OTHER_IMAGES_DIR, XML_FILE_DIR};
	// Potentially can add more subdirectories
	
	// Generic file names
	private static final String REFERENCE_IMG_FILE = "REF.jpg";
	private static final String FEATURES_FILE = "features.xml";
	// Pattern matchers
	private static final Pattern mNonAlphaNumeric = Pattern.compile("^([A-Za-z]|[0-9]|-|_)+$"); 
	
	private static FileManagement singleton;
	
	/**
	 * Mapping form simple name of appliance to Full path directory
	 */
	private Map<String, String> mApplianceDirectories;
	
	/**
	 * Private constructor so that there is only one instance of
	 * this class running at one time.  This means that this
	 * class will have to synchronize loading and saving
	 * <b>Has to make sure the root directories are saved
	 */
	private FileManagement(){
		String[] directories =  new String[] {DATA_PATH, APPLIANCES_PATH};
		addDirectories(directories);
		mApplianceDirectories = new HashMap<String, String>();
	}
	
	/**
	 * Returns a single instane to help manage Appliance related files
	 * @return
	 */
	public static FileManagement getInstance(){
		if (singleton == null) 
			singleton = new FileManagement();
		return singleton;
	}
	
	////////////////////////////////////////////////////////////////////
	// Simple query methods
	////////////////////////////////////////////////////////////////////
	
	/**
	 * Quick helper method for checking if a name is valid
	 * @param name Name of appliance
	 * @return 
	 */
	public synchronized boolean isValidName(String name){
		return privIsValidName(name);
	}
	
	/**
	 * Given a single name of an appliance
	 * <b> Name has to contain only all alphanumeric characters 
	 * (IE letters, numbers, "_", or "-" only)
	 * @param simpleName non absolute path name
	 * @return Whether the appliance already exist
	 * @throws NameFormatException If the name requested is not a simple name that only
	 * includes Alphanumeric and _ and - 
	 */
	public synchronized boolean hasAppliance(String simpleName) throws NameFormatException{
		if (!privIsValidName(simpleName))
			throw new NameFormatException(simpleName);
		// TODO CReate Cache and add to it
		return mApplianceDirectories.containsKey(simpleName);
	}
	
	////////////////////////////////////////////////////////////////////
	// Mutating methods
	////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Creates a complete directory for adding appliance images
	 * Name that does not contain any special characters. Name should
	 * only contain alpha numeric 
	 * @param simpleName Name of appliance to create
	 * @throws NameFormatException the name requested is not a simple name that only
	 * includes Alphanumeric and _ and - 
	 */
	public synchronized void addAppliance(String simpleName) throws NameFormatException {
		if (!hasAppliance(simpleName)){
			// Add main appliance directory
			String appliancePath = APPLIANCES_PATH + simpleName + "/";
			addDirectory(appliancePath);
			
			// Add all the subdirectories
			for (String subDir: APPLIANCE_SUBDIRECTORIES){
				String subDirAbsolute = appliancePath + subDir;
				addDirectory(subDirAbsolute);
			}
			
			// Add to cache / map 
			mApplianceDirectories.put(simpleName, appliancePath);
		}
	}
	
	/**
	 * If new image is null, there will be no effects
	 * 
	 * @param appliance appliance in which new image will be a reference for
	 * @param newRefImg Image to be used as a reference image
	 * @throws ApplianceNotExistException appliance does not exist
	 * @throws NameFormatException the name requested is not a simple name that only
	 * includes Alphanumeric and _ and - 
	 */
	public synchronized void setReferenceImage(String appliance, Bitmap newRefImg) 
			throws ApplianceNotExistException, NameFormatException {
		// Bitmap is null
		if (newRefImg == null)
			Log.d(TAG, "Null Image attempted to be saved as reference");
		// Check if appliance exist
		if (hasAppliance(appliance)){
			String refPath = mApplianceDirectories.get(appliance) + REFERENCE_IMAGES_DIR + REFERENCE_IMG_FILE;
			ImageIO.saveBitmapToFile(newRefImg, refPath);
		} else
			throw new ApplianceNotExistException(appliance);
	}
	
	public synchronized void addOtherImage(String appliance, Bitmap b) {
		// Check if appliance exist
		// TODO implements
	}
	
	public synchronized void addXMLFile(String appliance, XMLTestImageSet imageData) {
		// TODO Implement saving the XML file
	}
	
	////////////////////////////////////////////////////////////////////
	// Retrieval methods
	////////////////////////////////////////////////////////////////////
	
	/**
	 * 
	 * @param appliance Appliance to be found
	 * @return null if no reference image for appliance exist
	 * @throws NameFormatException Illegal name input
	 */
	public synchronized String getReferenceImage(String appliance) throws NameFormatException{
		if (hasAppliance(appliance)){
			String refPath = mApplianceDirectories.get(appliance) + REFERENCE_IMAGES_DIR + REFERENCE_IMG_FILE;
			File refFile = new File(refPath);
			if (refFile.exists())
				return refFile.getAbsolutePath();
		}
		return null;
	}
	
	public synchronized List<String> getOtherImages(String appliance){
		// TODO Implement, beware list of bitmap is very straining on android
		return null;
	}
	
	public synchronized XMLTestImageSet getFeatures(String appliance){
		// TODO Implement
		return null;
	}
	
	
	////////////////////////////////////////////////////////////////////
	// Private Helper Methods
	////////////////////////////////////////////////////////////////////
	
	private void addDirectory(String directory) {
		File dir = new File(directory);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "ERROR: Creation of directory " + directory + " on sdcard failed");
				return;
			} else {
				Log.i(TAG, "Created directory " + directory + " on sdcard");
			}
		}
	}
	
	private void addDirectories(String[] directories){
		for (String path : directories) {
			addDirectory(path);
		}
	}
	
	private boolean privIsValidName(String name){
		if (name == null) return false;
		Matcher match = mNonAlphaNumeric.matcher(name);
		return match.find();
	}
	
	public class ApplianceNotExistException extends IOException {
		public ApplianceNotExistException(String appliance){
			super("Appliance:" + appliance+" does not exist on your drive");
		}
	}
	
	public class NameFormatException extends Exception {

		/**
		 * Serial ID
		 */
		private static final long serialVersionUID = -8561650210050752923L;
		
		public NameFormatException(String name){
			super("Name inludes non alphanumeric, \"-\" or _  characters: " + name);
		}
	}
}
