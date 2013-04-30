package uw.cse.mag.appliancereader.db;

import uw.cse.mag.appliancereader.db.datatype.Appliance;

public class ApplianceNotExistException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2564686040361234092L;

	public ApplianceNotExistException(Appliance appliance){
		super("Appliance:" + appliance +" does not exist on your external drive.  Be sure to addAppliance before setting" +
				"reference image");
	}
}
