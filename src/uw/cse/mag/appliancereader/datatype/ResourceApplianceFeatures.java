package uw.cse.mag.appliancereader.datatype;

/**
 * Appliance Features that are defined in a pre compiled resource file in
 * the application
 * @author mhotan
 */
class ResourceApplianceFeatures extends ApplianceFeatures {
	
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
