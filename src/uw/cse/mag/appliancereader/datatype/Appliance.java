package uw.cse.mag.appliancereader.datatype;

/**
 * Appliance object that defines a Electrical Appliance that has a digitial display
 * @author mhotan
 */
public class Appliance {

	// TODO Finalize Abstract functions and Representation Invariants
	
	/**
	 * Database ID for this appliance
	 */
	private long mId;
	
	/**
	 * Nick Name that user can choose for the appliance
	 * IE "My Microwave" 
	 */
	private String mNickName;
	
	/**
	 * Make of the Appliance
	 * IE "Samsung"
	 */
	private String mMake;
	
	/**
	 * Model of Appliance
	 */
	private String mModel;
	
	/**
	 * Directory where all the files are stored
	 */
	private String mDirectory;
	
	/*
	 * Setters
	 * */
	
	public void setId(long id){
		this.mId = id;
	}
	
	public void setNickName(String nickname){
		this.mNickName = nickname;
	}
	
	public void setMake(String make){
		this.mMake = make;
	}
	
	public void setModel(String model){
		this.mModel = model;
	}
	
	public void setDirectoryPath(String directoryPath){
		this.mDirectory = directoryPath;
	}
	
	/*
	 * Getters
	 * */
	
	
}

