package uw.cse.mag.appliancereader.datatype;

public class StringApplianceFeatures extends ApplianceFeatures {

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
