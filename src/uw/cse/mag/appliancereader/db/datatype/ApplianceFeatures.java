package uw.cse.mag.appliancereader.db.datatype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;


/**
 * Abstract class that describes the features of a single application
 * <br>A feature is descrtibed to have
 * <br>-- Reference name of feature
 * <br>-- List of points describing the bounding box of the feature
 * 
 * @author mhotan
 */
public abstract class ApplianceFeatures implements Iterable<ApplianceFeature> {

	 
	
	private static final Logger log = Logger.getLogger(ApplianceFeatures.class.getSimpleName());
	
	
	/**
	 * Hidden data abstraction for containing data
	 * Features are only identified by 
	 */
	private final Map<String, ApplianceFeature> mFeatures;
		
	public ApplianceFeatures(){
		mFeatures = new HashMap<String, ApplianceFeature>();
	}
	
	/**
	 * Adds a new feature to the appliance image set
	 * 
	 * @requires neither argument to be null, or points to have a size less then two
	 * @param featureName Name of feature
	 * @param points List of points that represent the feature
	 */
	protected void addFeature(String featureName,  List<Point> points){
		if (points == null)
			throw new IllegalArgumentException("Null Point list");
		if (featureName == null)
			throw new IllegalArgumentException("Null feature name");
		if (featureName.length() == 0)
			throw new IllegalArgumentException("Feature name Empty: \"" + featureName +"\"");
		if (points.size() <= 2)
			throw new IllegalArgumentException("Not enough points for this feature Number of Points: " + points.size());
		
		List<Point> hiddenPoints = new LinkedList<Point>();
		for (Point p: points){
			hiddenPoints.add(p.clone());
		}
		
		mFeatures.put(featureName, new ApplianceFeature(featureName, hiddenPoints));
	} 
	 
	/**
	 * Each image set contains a known set of features that are distinguishable on the appliance <b>
	 * To be able to reference the image shapes the all the names of the features must be known<b>
	 * This indicates that every feature name should be unique <b>
	 * 
	 * @return unmodifiable list of names of features, if no features exist list is empty
	 */
	public List<String> getFeatures(){
		return new LinkedList<String>(mFeatures.keySet());
	}
	
	/**
	 * Given a feature name found from call to getFeatures() it returns the list of corresponding points of the image
	 * <b>  The list of points returned will have more then two points 
	 * <b>  The point list will not be closed, meaning no two points will be the same
	 * 
	 * @param featureName
	 * @return null if feature does not exist, Empty list when feature is incomplete, else list of unmodifiable Points
	 */
	public List<Point> getFeature(final String featureName){
		if (!mFeatures.containsKey(featureName))
			return null;
		
		ApplianceFeature appfeature = mFeatures.get(featureName);
		return appfeature.getPoints();
	}
	
	public boolean isEmpty(){
		return mFeatures.isEmpty();
	}
	
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer();
		for (ApplianceFeature feature: getListOfApplianceFeatures()){
			buf.append(feature);
			buf.append("\n");
		}
		return buf.toString();
	}
	
	/**
	 * Scales down all the feature point by a particular value
	 * Scales down all the features
	 * @param scaleFactor int factor to scale points down by
	 */
	public void scaleFeatures(float scaleFactor) {
		Collection<ApplianceFeature> AppFeatures = mFeatures.values();
		for (ApplianceFeature a: AppFeatures)
			a.scaleFeature(scaleFactor); // Scale down
	}
	
	/**
	 * Returns a bounding box for every feature for this appliance.  Each box 
	 * has parallel vertical line and parallel horizontal lines.  
	 * @return list of boxes
	 */
	public List<Rect> getFeatureBoxes(){
		List<Rect> boxes = new ArrayList<Rect>();
		for (ApplianceFeature a: this){
			if (a == null) continue;
			boxes.add(a.getBoundingBox());
		}
		return boxes;
	}
	
	/**
	 * Returns the smallest bounding box in which all display elements
	 * are contained
	 * @param scaleFactor int factor to scale points down by
	 */
	public org.opencv.core.Rect getEncompassingBox() {
		List<Rect> rects = getFeatureBoxes();
		if (rects == null) return null;
		
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		for (Rect r: getFeatureBoxes()) {
			if (r.tl().x < minX) minX = r.tl().x;
			if (r.tl().y < minY) minY = r.tl().y;
			if (r.br().x > maxX) maxX = r.br().x;
			if (r.br().y > maxY) maxY = r.br().y;
		}
		
		Point minPt = new Point(minX, minY);
		Point maxPt = new Point(maxX, maxY);
		
		Rect r = new Rect(minPt, maxPt);
		
		log.log(Level.INFO, "Bounding box found around all the features TL:" + r.tl() + " BR:" + r.br());
		
		return r;
	}
	
	@Override
	public Iterator<ApplianceFeature> iterator(){
		return new ApplianceFeatureIterator(getListOfApplianceFeatures());
	}
	
	private List<ApplianceFeature> getListOfApplianceFeatures(){
		List<String> featureNames = new ArrayList<String>(mFeatures.keySet());
		Collections.sort(featureNames);
		List<ApplianceFeature> apps = new ArrayList<ApplianceFeature>(featureNames.size());
		for (String s: featureNames){
			apps.add(mFeatures.get(s));
		}
		return apps;
	}

	public void rotateAround(Mat rotMat2by3) {
		Collection<ApplianceFeature> AppFeatures = mFeatures.values();
		for (ApplianceFeature a: AppFeatures)
			a.rotate(rotMat2by3); // Scale down
	}
}
