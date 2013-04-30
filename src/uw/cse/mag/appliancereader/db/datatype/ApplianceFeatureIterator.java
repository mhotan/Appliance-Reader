package uw.cse.mag.appliancereader.db.datatype;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class ApplianceFeatureIterator implements Iterator<ApplianceFeature> {

	private static final Logger log = Logger.getLogger(ApplianceFeatureIterator.class.getSimpleName());
	
	private final List<ApplianceFeature> mFeatures;
	private int mIndex;
	
	public ApplianceFeatureIterator(List<ApplianceFeature> feats){
		if (feats == null)
			throw new IllegalArgumentException("list of Appliance Features is null");
		mFeatures = feats;
		mIndex = 0;
	}
	
	@Override
	public boolean hasNext() {
		return mIndex != mFeatures.size();
	}

	@Override
	public ApplianceFeature next() {
		ApplianceFeature a = mFeatures.get(mIndex);
		mIndex++;
		return a;
	}

	@Override
	public void remove() {
		log.log(Level.WARNING, "Remove called but not supported");
	}

}
