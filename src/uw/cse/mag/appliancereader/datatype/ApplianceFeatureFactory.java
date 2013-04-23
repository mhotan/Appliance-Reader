package uw.cse.mag.appliancereader.datatype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

/**
 * Factory that pumps out Appliance features description
 * @author mhotan
 */
public class ApplianceFeatureFactory {
	
	private static final String TAG = ApplianceFeatureFactory.class.getSimpleName();
	
	/**
	 * Can't instantiate this factory
	 */
	private ApplianceFeatureFactory(){}
	
	/**
	 * @return an empty set of appliance features
	 */
	public static ApplianceFeatures getEmptyApplianceFeatures(){
		return new EmptyApplianceFeatures();
	}
	
	/**
	 * Returns an ApplianceFeatures for XML Resource file 
	 * 
	 * @requires activity not null
	 * @param activity activity that has access to resources for xml
	 * @param id Resource ID of xml document
	 * @return ApplianceFeatures instance, null if XML was unable to load
	 */
	public static ApplianceFeatures getApplianceFeatures(Activity activity, int id){
		Resources res = activity.getResources();
		XmlResourceParser xpp = res.getXml(id);
		ApplianceFeatures features = new ResourceApplianceFeatures(id);
		// Parser features
		try {
			ApplianceXMLParser.parse(features, xpp);
		} catch (XmlPullParserException e) {
			Log.w(TAG, "Illegal XML format");
			return null;
		}
		return features;
	}

	/**
	 * 
	 * @param xmlString
	 * @return
	 */
	public static ApplianceFeatures getApplianceFeaturesFromString(String xmlString) {
		try {
			XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
			xpp.setInput(new StringReader(xmlString));
			ApplianceFeatures features = new StringApplianceFeatures(xmlString);
			ApplianceXMLParser.parse(features, xpp);
			return features;
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Unable to XML parser input string " + xmlString + "!" );
			return null;
		}
	}

	/**
	 * Returns an ApplianceFeatures for a file path 
	 * @param filePath Absolute file path for XML document
	 * @return ApplianceFeatures instance, or null if filePath
	 * @throws IOException if file path is incorrect
	 */
	public static ApplianceFeatures getApplianceFeatures(String filePath) throws IOException{
		XmlPullParser xpp = null;
		try {
			// Establish an input stream connected to the file path of the XML document
			FileInputStream fis = new FileInputStream(new File(filePath));
			// Create a character reader from the file
			InputStreamReader isr = new InputStreamReader(fis);

			// Retrieve an XML Pull instance
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			xpp = factory.newPullParser();

			// Set the input to our parser as our reader
			xpp.setInput(isr);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Unable to XML parser input file at " + filePath + "!" );
			return null;
		}

		ApplianceFeatures features = new ExtFileApplianceFeatures(filePath);
		// Parser features
		try {
			ApplianceXMLParser.parse(features, xpp);
		} catch (XmlPullParserException e) {
			Log.w(TAG, "Illegal XML format");
			return null;
		}
		return features;
	}
	
	
	
	private static class EmptyApplianceFeatures extends ApplianceFeatures {
	
	}
	
	/**
	 * Appliance Features that are defined in a pre compiled resource file in
	 * the application
	 * @author mhotan
	 */
	private static class ResourceApplianceFeatures extends ApplianceFeatures {
		
		/**
		 * Resource ID points to XML
		 */
		private final int mResourceId;
		
		/**
		 * Creates a Appliance Features from resource file
		 * @param id XML Resource ID for appliance features
		 */
		public ResourceApplianceFeatures(int id){
			mResourceId = id;
		}
		
		public int getResourceId(){
			return mResourceId;
		}
	}
	
	/**
	 * Appliance features that are in String representation
	 * @author mhotan
	 */
	private static class StringApplianceFeatures extends ApplianceFeatures {

		private final String mStringRep;
		
		public StringApplianceFeatures(String stringRep){
			if (stringRep == null) {
				throw new IllegalArgumentException("StringApplianceFeatures, String argument is null");
			}
			mStringRep = stringRep;
		}
		
		public String toString(){
			return mStringRep.trim();
		}
		
	}
	
	/**
	 * Appliance Features that are defined in a file saved in an external storage device
	 * 
	 * @author mhotan
	 */
	private static class ExtFileApplianceFeatures extends ApplianceFeatures {

		/**
		 * Resource ID points to XML
		 */
		private final String mFilePath;
		
		/**
		 * Creates a Appliance Features from resource file
		 * @param filepath absolute file path for xml file
		 */
		public ExtFileApplianceFeatures(String filepath){
			mFilePath = filepath;
		}
		
		public String getFilePath(){
			return mFilePath;
		}
		
	}
}
