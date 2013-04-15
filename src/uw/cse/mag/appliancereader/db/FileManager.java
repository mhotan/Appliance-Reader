package uw.cse.mag.appliancereader.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;

import uw.cse.mag.appliancereader.datatype.Appliance;
import uw.cse.mag.appliancereader.datatype.ApplianceFeature;
import uw.cse.mag.appliancereader.datatype.ApplianceFeatures;
import uw.cse.mag.appliancereader.datatype.ApplianceXMLParser;
import uw.cse.mag.appliancereader.util.ImageIO;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

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
public class FileManager {
	private static final String TAG = FileManager.class.getSimpleName();

	// Directories
	public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ApplianceReaderSpecific/";
	public static final String APPLIANCES_PATH = DATA_PATH + "Appliances/";

	// General Storage.. Can store anything in here for short term use.
	// Does not handle name space issues

	// Sub directories of a given appliance
	private static final String REFERENCE_IMAGES_DIR = "REF-Images/";
	private static final String OTHER_IMAGES_DIR = "OTHER-Images/";
	private static final String XML_FILE_DIR = "XML-File/";
	private static final String[] APPLIANCE_SUBDIRECTORIES = 
			new String[]{REFERENCE_IMAGES_DIR, OTHER_IMAGES_DIR, XML_FILE_DIR};
	// Potentially can add more subdirectories

	// Generic file names
	private static final String REFERENCE_IMG_FILE = "REF.jpg";
	private static final String XML_FEATURES_FILE = "features.xml";

	// Pattern matchers
	//	private static final Pattern mNonAlphaNumeric = Pattern.compile("^([A-Za-z]|[0-9]|-|_)+$"); 

	private static FileManager singleton;

	/**
	 * Mapping form simple name of appliance to Full path directory
	 * 
	 * Representation invariant: 
	 * For every non null key there must exist a non null file path
	 * 		This file path must lead be a path to a directory of the layout above
	 * 
	 */
	private Map<Long, String> mApplianceDirectories;

	/**
	 * Private constructor so that there is only one instance of
	 * this class running at one time.  This means that this
	 * class will have to synchronize loading and saving
	 * <b>Has to make sure the root directories are saved
	 */
	private FileManager(){
		String[] directories =  new String[] {DATA_PATH, APPLIANCES_PATH};
		addDirectories(directories);
		mApplianceDirectories = new HashMap<Long, String>();
	}

	/**
	 * Returns a single instane to help manage Appliance related files
	 * @return
	 */
	public static FileManager getInstance(){
		if (singleton == null) 
			singleton = new FileManager();
		return singleton;
	}

	////////////////////////////////////////////////////////////////////
	// Simple query methods
	////////////////////////////////////////////////////////////////////

	/**
	 * Given a single name of an appliance that knows its ID and FilePath.
	 *	If the ID is stored in the mapping and  
	 *
	 * @param simpleName non absolute path name
	 * @return Whether the appliance already exist, if appliance does not have valid ID false 
	 */
	public synchronized boolean hasAppliance(Appliance app) {
		Long id = app.getID();
		if (id == -1L){ // Failure case
			throw new IllegalArgumentException("Illegal Appliance STATE, ID cannot be -1");
		}
		return mApplianceDirectories.containsKey(id) && // Must contain the right key
				mApplianceDirectories.get(id).equals(app.getDirectoryPath()); // key has to map to latest file
	}

	////////////////////////////////////////////////////////////////////
	// Mutating methods
	////////////////////////////////////////////////////////////////////


	/**
	 * Creates a complete directory for adding appliance images
	 * Name that does not contain any special characters. Name should
	 * only contain alpha numeric 
	 * @param simpleName Name of appliance to create
	 * @return The same appliance instance but with an updated Directory
	 */
	public synchronized Appliance addAppliance(Appliance app) {
		// The time stamped directory and 
		// ID are equal
		if (hasAppliance(app))
			return app;

		// There is already an appliance with the same ID.
		// Must check if id and path are correctly matched
		// The directory path doesn't match up
		String storedPath = mApplianceDirectories.get(app.getID());
		if (storedPath != null) { // there is directory that exist that correlates the ID
			removeAppliance(app);
		}

		// Adds a unique Directory path marked with time stamp
		String appliancePath = createNewDirectory(app.getID());

		// Add to cache / map 
		mApplianceDirectories.put(app.getID(), appliancePath);
		// Add the appliance path current Appliance
		app.setDirectoryPath(appliancePath);
		return app;
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
	public synchronized void setReferenceImage(Appliance appliance, Bitmap newRefImg) 
			throws ApplianceNotExistException {
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

	/**
	 * For a given appliance that already exist within the file system.  If 
	 * there is already features associated with this appliance the new one will be replaced
	 * 
	 * @requires appliance and image Data not to be null.  Appliance Should have a valid ID and Directory Path
	 * @param appliance Appliance to store features in
	 * @param appFeatures Appliance features
	 * @throws ApplianceNotExistException 
	 */
	public synchronized void addXMLFile(Appliance appliance, ApplianceFeatures appFeatures) 
			throws ApplianceNotExistException {
		// TODO Implement saving the XML file
		if (appliance.getDirectoryPath() == null)
			throw new IllegalArgumentException("Attempting to add a groupp of appliance features" +
					"into a directory that doesn't exist.  Call addAppliance(Appliance a) to initialize" +
					"the directory");

		// Check appliance exist 
		if (!hasAppliance(appliance))
			throw new ApplianceNotExistException(appliance);

		// Create the file path for the XML
		String xmlPath = mApplianceDirectories.get(appliance) + XML_FILE_DIR + XML_FEATURES_FILE;
		File xmlFile = new File(xmlPath);

		// Obtain the associated names
		// This is called before deleting the file to raise a null pointer 
		// and check for preconditions
		List<String> featureNames = appFeatures.getFeatures();

		// delete the XML file if it exists
		if (xmlFile.exists()){
			xmlFile.delete();
		}

		xmlFile = new File(xmlPath);
		try {
			xmlFile.createNewFile();
		} catch (IOException e) {
			Log.e(TAG, "Unable to create file to store XML describing features");
			return;
		}

		// Bind a file output stream to the xml file
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(xmlFile);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundExcetpion. Unable to create File Output stream to store XML describing features, " +
					"Should not occur because file should have been created");
		}

		XmlSerializer serializer = Xml.newSerializer();
		try {
			// Associate output stream
			serializer.setOutput(fos, "UTF-8");

			// No XML declaration with encoding
			// Standalone == true
			serializer.startDocument(null, true);

			// Set indention
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

			// Set the root as the annotation tag
			serializer.startTag(null, ApplianceXMLParser.FEATURE_ANNOTATION);

			// TODO Add other TAG here if needed
			// IE filename, source, etc
			
			if (appFeatures != null){
				for (ApplianceFeature af: appFeatures) {
					serializer.startTag(null, ApplianceXMLParser.FEATURE_TAG);
					
					// Store the name of this feature this is the object tag in Label Me
					serializer.startTag(null, ApplianceXMLParser.FEATURE_NAME_TAG);
					serializer.text(af.getName());
					serializer.endTag(null, ApplianceXMLParser.FEATURE_NAME_TAG);
					
					//Add polygon tag in Label ME
					serializer.startTag(null, ApplianceXMLParser.FEATURE_SHAPE_TAG);
					
					for (appFeatures.get)
					
					serializer.endTag(null, ApplianceXMLParser.FEATURE_SHAPE_TAG);

					serializer.endTag(null, ApplianceXMLParser.FEATURE_TAG);
				}
			}

			// End container document and end the document
			serializer.endTag(null, ApplianceXMLParser.FEATURE_ANNOTATION);
			serializer.endDocument();

			// Flush will write the xml to the file
			serializer.flush();
			fos.close();

		} catch (Exception e) {

		}
	}

	/**
	 * Deletes this appliance from the entire file system
	 * @param appliance Appliance to delete
	 */
	public synchronized void removeAppliance(Appliance appliance) {
		String pathToDel = mApplianceDirectories.get(appliance.getID());
		if (!deleteDirectory(pathToDel)){
			Log.w(TAG, "Path: " + pathToDel + " does not exist but attempted to be deleted");
		} 
		// Remove from mapping
		mApplianceDirectories.remove(appliance.getID());
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
	public synchronized String getReferenceImage(Appliance appliance) {
		if (hasAppliance(appliance)){
			String refPath = mApplianceDirectories.get(appliance) + XML_FILE_DIR + ;
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

		XmlSerializer serializer = 
				// TODO Implement
				return null;
	}


	////////////////////////////////////////////////////////////////////
	// Private Helper Methods
	////////////////////////////////////////////////////////////////////

	/**
	 * Returns a date format for saving files
	 * 
	 *  Format returned: MM/DD/YR_HR:MI_(AM or PM)
	 * 
	 * @return A Short time stamp with out white spaces
	 */
	private String getTimeStamp(){
		Date date = new Date();
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US);
		String timeStamp = df.format(date);	
		timeStamp = timeStamp.trim();
		timeStamp = timeStamp.replaceAll(" ", "_");
		return 	timeStamp;
	}

	/**
	 * Creates a standerd format directory that
	 * correlates with id and current timestamp
	 * @return path to root of directory
	 */
	private String createNewDirectory(long id){
		String path = APPLIANCES_PATH + id + "_" + getTimeStamp() + "/";
		addDirectory(path);

		// Add all the subdirectories
		for (String subDir: APPLIANCE_SUBDIRECTORIES){
			String subDirAbsolute = path + subDir;
			addDirectory(subDirAbsolute);
		}
		return path;
	}

	/**
	 * Creates a directory to store images and xml file of Appliances
	 * If failure 
	 * @param File path to create a directory
	 * @return
	 */
	private boolean addDirectory(String directory) {
		File dir = new File(directory);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "ERROR: Creation of directory " + directory + " on sdcard failed");
				return false;
			} else {
				Log.i(TAG, "Created directory " + directory + " on sdcard");
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds a an array of directories to the file path determined
	 * @param directories
	 */
	private void addDirectories(String[] directories){
		for (String path : directories) {
			addDirectory(path);
		}
	}

	/**
	 * For a string representation of a file delete the file that exist there
	 * @param dirPath Directory path
	 * @return if directory was deleted
	 */
	private boolean deleteDirectory(String dirPath){
		File f = new File(dirPath);
		if (!f.exists()) return false;
		deleteDirectory(f);
		return true;
	}

	/**
	 * Delete directory recursively
	 * @param dir directory to delete
	 */
	private void deleteDirectory(File dir){
		// Must check if this file is directory first
		if ( dir.isDirectory() )
		{	// Iterate through every child
			for ( File child: dir.listFiles() )
			{	// Recursively delete
				if(child.isDirectory()){
					deleteDirectory( child );
					child.delete();
				}else{
					child.delete();
				}
			}
			dir.delete();
		}
	}

	//	private boolean privIsValidName(String name){
	//		if (name == null) return false;
	//		Matcher match = mNonAlphaNumeric.matcher(name);
	//		return match.find();
	//	}

	public class ApplianceNotExistException extends IOException {
		public ApplianceNotExistException(Appliance appliance){
			super("Appliance:" + appliance+" does not exist on your external drive.  Be sure to addAppliance before setting" +
					"reference image");
		}
	}
	//
	//	public class NameFormatException extends Exception {
	//
	//		/**
	//		 * Serial ID
	//		 */
	//		private static final long serialVersionUID = -8561650210050752923L;
	//
	//		public NameFormatException(String name){
	//			super("Name inludes non alphanumeric, \"-\" or _  characters: " + name);
	//		}
	//	}
}
