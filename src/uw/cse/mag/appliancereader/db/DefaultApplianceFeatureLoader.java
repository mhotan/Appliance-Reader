package uw.cse.mag.appliancereader.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;

import uw.cse.mag.appliancereader.camera.ExternalApplication;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DefaultApplianceFeatureLoader {

	private static final String TAG = DefaultApplianceFeatureLoader.class.getSimpleName();
	
	/**
	 * Returns an a list of String formatted as an XML Document
	 * A non usable XML format may be attemted to read
	 * <b>It depends what is in xml resource folder
	 * @param ctx owning context to retrieve resources
	 * @return map of resource name to literal XML representation String
	 * @throws IllegalAccessException No access to xml assets 
	 * @throws IllegalArgumentException  
	 */
	public static HashMap<String, String> getDefaultAppliances(Context ctx) 
			throws IllegalArgumentException, IllegalAccessException {

		// Create an empty map of XMLs
		HashMap<String, String> xmls = new HashMap<String, String>();

		// Obtain a list of fields of everything in the xml resource folder
		Field[] xml_fields = R.xml.class.getFields();
		// Iterate through every XML Document
		for (Field f: xml_fields){
			Log.d(TAG, "XML Asset Located: " +  f.getName());
			int resID = f.getInt(f);
			// read literal XMl representation
			String xml_str = readRawTextFile(ctx, resID);
			xmls.put(f.getName(), xml_str);
		}
		return xmls;
	}

	/**
	 * Reads a resource file and returns it in its literal String representation
	 * @param ctx that has access to resources
	 * @param resId 
	 * @return
	 */
	private static String readRawTextFile(Context ctx, int resId)
	{
		InputStream inputStream = ctx.getResources().openRawResource(resId);

		InputStreamReader inputreader = new InputStreamReader(inputStream);
		BufferedReader buffreader = new BufferedReader(inputreader);
		String line;
		StringBuilder text = new StringBuilder();

		try {
			while (( line = buffreader.readLine()) != null) {
				text.append(line);
				// MH:
				// XML will be directly read so new line is not needed
				//				text.append('\n');
			}
		} catch (IOException e) {
			return null;
		}
		return text.toString();
	}
}
