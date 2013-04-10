package uw.cse.mag.appliancereader.datatype;

/**
 * Appliance Features that are defined in a file saved in an external storage device
 * 
 * @author mhotan
 */
class ExtFileApplianceFeatures extends ApplianceFeatures {

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
