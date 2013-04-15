package uw.cse.mag.appliancereader.datatype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
public abstract class ApplianceFeatures {

	/**
	 * Hidden data abstraction for containing data
	 * Features are only identified by 
	 */
	private final Map<String, List<Point>> mFeatures = new HashMap<String, List<Point>>();
	
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
		mFeatures.put(featureName, hiddenPoints);
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
	 * @return null if feature does not exist, Empty list when feature is incomplete, else list of Points
	 */
	public List<Point> getShapePoints(final String featureName){
		if (!mFeatures.containsKey(featureName))
			return null;
		List<Point> points = new LinkedList<Point>(mFeatures.get(featureName));
		if (points.size() <= 2){
			points.clear();
			return points;
		}
		//Create copy of mutable list of points
		List<Point> hiddenPoints = new LinkedList<Point>();
		for (Point p: points){
			hiddenPoints.add(p.clone());
		}
		return hiddenPoints;
	}
	
	@Override
	public String toString(){
		return mFeatures.toString();
	}
	
	/**
	 * Scales down all the feature point by a particular value
	 * Scales down all the features
	 * @param scaleFactor int factor to scale points down by
	 */
	public void scaleDownFeatures(int scaleFactor) {
		for (String feature: getFeatures()) {
			List<Point> nList = new ArrayList<Point>();
			for (Point p: getShapePoints(feature)){
				Point nPoint = new Point(p.x/scaleFactor, p.y/scaleFactor);
				nList.add(nPoint);
			}
			addFeature(feature, nList);
		}
	}
	
	/**
	 * Returns the smallest bounding box in which all display elements
	 * are contained
	 * @param scaleFactor int factor to scale points down by
	 */
	public org.opencv.core.Rect getBoundingBox() {
		if (getFeatures().size() ==0) return null;
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		for (String feature: getFeatures()) { // For every Feature
			for (Point p: getShapePoints(feature)){ // For every point that describes that feature
				if (p.x < minX) minX = p.x;
				if (p.x > maxX) maxX = p.x;
				if (p.y < minY) minY = p.y;
				if (p.y > maxY) maxY = p.y;
			}		
		}
		Point minPt = new Point(minX, minY);
		Point maxPt = new Point(maxX, maxY);
		return new Rect(minPt, maxPt);
	}
	
	
}
