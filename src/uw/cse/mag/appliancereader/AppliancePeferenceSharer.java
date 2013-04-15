package uw.cse.mag.appliancereader;

import uw.cse.mag.appliancereader.datatype.Appliance;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Wrapper class to help store shared peferences for this application
 * Especiall nice to save Appliances
 * @author mhotan
 */
public class AppliancePeferenceSharer {

	private static final String APP_SHARED_PREFS = "MainActivity_ApplianceReader_SharedPreferences"; 
    private static final String KEY_ID = "ID";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_MAKE = "make";
    private static final String KEY_MODEL = "model";
    private static final String KEY_TYPE = "type";
    private static final String KEY_DIRECTORY = "directory";
	
	private final SharedPreferences appSharedPrefs;
    
    public AppliancePeferenceSharer(Context ctx){
    	appSharedPrefs = ctx.getSharedPreferences(APP_SHARED_PREFS, 0);
    }
    
    public void saveAppliance(Appliance app) {
    	Editor e = appSharedPrefs.edit();
    	if (app == null) {// Fail to save any appliance
    		e.putLong(KEY_ID, -1);
    		return;
    	}
    	else
    		e.putLong(KEY_ID, app.getID());
    	e.putString(KEY_NICKNAME, app.getNickname());
    	e.putString(KEY_MAKE, app.getMake());
    	e.putString(KEY_MODEL, app.getModel());
    	e.putString(KEY_TYPE, app.getType());
    	e.putString(KEY_DIRECTORY, app.getDirectoryPath());
    	e.commit();
    }
    
    public Appliance getLastAppliance(){
    	Long id = appSharedPrefs.getLong(KEY_ID, -1);
    	if (id == -1) 
    		return null;
    	Appliance a = new Appliance();
    	a.setId(id);
    	a.setNickName(appSharedPrefs.getString(KEY_NICKNAME, null));
    	a.setMake(appSharedPrefs.getString(KEY_MAKE, null));
    	a.setModel(appSharedPrefs.getString(KEY_MODEL, null));
    	a.setType(appSharedPrefs.getString(KEY_TYPE, null));
    	a.setDirectoryPath(appSharedPrefs.getString(KEY_DIRECTORY, null));
    	return a;
    }
    
    
}
