package uw.cse.mag.appliancereader.datatype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Point;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

/**
 * XML Parser for appliance features as created by label me
 * @author Michael Hotan
 */
public class ApplianceXMLParser {
	private static final String TAG = ApplianceXMLParser.class.getSimpleName();

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
			parse(features, xpp);
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
			parse(features, xpp);
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
			parse(features, xpp);
		} catch (XmlPullParserException e) {
			Log.w(TAG, "Illegal XML format");
			return null;
		}
		return features;
	}

	// Conforms to label me tag
	public static final String FEATURE_ANNOTATION = "annotation";
	public static final String FEATURE_TAG = "object";
	public static final String FEATURE_NAME_TAG = "name";
	public static final String FEATURE_PT_TAG = "pt";
	public static final String FEATURE_PT_X_TAG = "x";
	public static final String FEATURE_PT_Y_TAG = "y";
	public static final String FEATURE_SHAPE_TAG = "polygon";

	/**
	 * Given an XML Pull Parser be able to parse any 
	 * @param features
	 * @param xpp
	 * @throws XmlPullParserException
	 */
	private static void parse(ApplianceFeatures features, XmlPullParser xpp) 
			throws XmlPullParserException {
		assert(xpp != null);
		try {
			xpp.next();

			int eventType = xpp.getEventType();
			StringBuffer stringBuffer = new StringBuffer();
			while (eventType != XmlPullParser.END_DOCUMENT)
			{
				if(eventType == XmlPullParser.START_DOCUMENT)
				{
					stringBuffer.append("--- Start XML ---");
				}
				else if(eventType == XmlPullParser.START_TAG)
				{
					String object = xpp.getName();

					// Iterate through all the tags 
					// find all the objects
					if (object.equals(FEATURE_TAG)){
						int objectType = xpp.next();
						String tagName = xpp.getName(); 

						//Variables to store object variables
						String objectName = null;
						List<Point> fPoints = new LinkedList<Point>();

						// WHile have not reached end tag for object
						while (!(objectType == XmlPullParser.END_TAG && 
								object.equals(tagName))){

							stringBuffer.append("\nObject Start: " + xpp.getName());

							// If the current tag is the NAME object
							if (objectType == XmlPullParser.START_TAG &&
									tagName.equals(FEATURE_NAME_TAG)){
								if (xpp.next() == XmlPullParser.TEXT){
									objectName = xpp.getText().trim();
								}
								else objectName = null;
							}
							stringBuffer.append("\nObject Name: " + xpp.getName());

							// TODO Add additional Fields here... 



							// For every point within the object add to list
							if (objectType == XmlPullParser.START_TAG &&
									tagName != null && tagName.equals(FEATURE_PT_TAG)){

								int ptType = xpp.next();
								String ptTagName = xpp.getName(); 

								// Iterate through the point and look for x and y
								Point p  = new Point();
								p.x = -1;
								p.y = -1;
								while (!(ptType == XmlPullParser.END_TAG &&
										ptTagName != null && ptTagName.equals(FEATURE_PT_TAG))){

									// If is X coordinate
									if (ptType == XmlPullParser.START_TAG &&
											ptTagName != null && ptTagName.equals(FEATURE_PT_X_TAG)){
										if (xpp.next() == XmlPullParser.TEXT){
											String text = xpp.getText();
											text = text.replace("\n", "");
											p.x = Double.parseDouble(text);
										}
									}

									// If is Y coordinate
									if (ptType == XmlPullParser.START_TAG &&
											ptTagName != null && ptTagName.equals(FEATURE_PT_Y_TAG)){
										if (xpp.next() == XmlPullParser.TEXT){
											String text = xpp.getText();
											text = text.replace("\n", "");
											p.y = Double.parseDouble(text);
										}
									}

									//Check if the point is complete

									ptType = xpp.next();
									ptTagName = xpp.getName(); 

									//If end of point has been reached 
									// check if point is valid
									// Create a new one
									if (ptType == XmlPullParser.END_TAG &&
											ptTagName != null && ptTagName.equals(FEATURE_PT_TAG)){

										if (p.x != -1 && p.y != -1){
											// valid point so add to list
											stringBuffer.append("\nObject Feature Point: " + p);
											fPoints.add(p);
										}

										p  = new Point();
										p.x = -1;
										p.y = -1;
									}
								}
							}

							// Update the tag name and tag id number
							objectType = xpp.next();
							tagName = xpp.getName(); 
						}

						stringBuffer.append("\nObject END: " + xpp.getName());

						// Check for name 
						if (objectName == null){
							Log.e(TAG, "Object had no name");
						}
						else if (fPoints.size() <= 2){
							Log.e(TAG, "Size of Feature points to small: " + fPoints.size());
						} else {
							//Finally add the feature to points
							features.addFeature(objectName, fPoints);
						}
					}
				}
				eventType = xpp.next();
			}
			stringBuffer.append("\n--- End XML ---");
			Log.i(TAG, "event name: " + stringBuffer.toString());
		} catch (IOException e) {
			// TODO Create some kind of special exception for android
			Log.e(TAG, "IOException occured loading XML File");
			e.printStackTrace();
		}
	}

	/*
	 * TODO Add Special Exception refurring to XML Ill formatted Documents 
	 */
}
