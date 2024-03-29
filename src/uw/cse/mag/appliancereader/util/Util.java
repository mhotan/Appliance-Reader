package uw.cse.mag.appliancereader.util;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public class Util {

	/**
	 * 
	 * REquires path is not null
	 * @param path
	 * @return
	 */
	public static String stripPathAndExtension(String path) {
		// Strip away .../something.../<name>.xml
		// to <name>
		String[] split = path.split("/");
		String s = split[split.length-1];
		split = s.split("\\");
		s = split[split.length-1];
		split = s.split(".");
		return split[0];
	}

	public static final String NOT_IMPLEMENTED_APOLOGY = "I'm, sorry this is not implemented just yet";
	
	/**
	 * Create an alert dialog saying this is not yet implement
	 * @param ctx
	 * @return
	 */
	public static AlertDialog getNotImplementedDialog(Context ctx){
		AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
		alertDialog.setTitle("Not Implemented");
		alertDialog.setMessage(NOT_IMPLEMENTED_APOLOGY);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//show what happen when OK button is clicked
				dialog.dismiss();
			}
		});
		return alertDialog;
	}
	
	/**
	 * Returns a date format for saving files
	 * 
	 *  Format returned: MM/DD/YR_HR:MI_(AM or PM)
	 * 
	 * @return A Short time stamp with out white spaces
	 */
	public static String getTimeStamp(){
		Date date = new Date();
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US);
		String timeStamp = df.format(date);	
		timeStamp = timeStamp.trim();
		timeStamp = timeStamp.replaceAll(" ", "_");
		return 	timeStamp;
	}

}
