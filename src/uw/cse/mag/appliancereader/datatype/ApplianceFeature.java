package uw.cse.mag.appliancereader.datatype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opencv.core.Point;
import org.opencv.core.Rect;

/**
 * <p>Contains the base contents for a distinguishable feature
 * inside an image. </p> 
 * 
 * The image is associated with values that correspond to different attributes and labels.  
 * it is up to any client class that implements subclasses of this to decide the value of the attributes.
 * 
 * @author mhotan
 */
public class ApplianceFeature {

	private static final Logger log = Logger.getLogger(ApplianceFeature.class.getSimpleName(), null);

	/**
	 * Name of the feature 
	 */
	protected final String mName;
	/**
	 * List of points of this feature that describe its overall shape.
	 */
	protected final List<Point> mPoints;

	/**
	 * Create an Appliance Feature that 
	 * @param name Name of feature
	 * @param shape Points that correspond to Shape of this feature
	 */
	public ApplianceFeature(String name, List<Point> shape){
		if (name == null){
			log.log(Level.SEVERE, "Appliance Feature Attempted to be made with null name");
			throw new IllegalArgumentException("Appliance name cannot be null");
		}
		if (shape.size() <= 2) {
			log.log(Level.SEVERE, "Feature: " + name + " has to little points, size: " + shape.size());
			throw new IllegalArgumentException("Number of shapes must be greater then 2, not " + shape.size());
		}

		mName = name;
		mPoints = new ArrayList<Point>(shape.size());
		for (Point p: mPoints){
			mPoints.add(p.clone());
		}
	}

	/**
	 * @return name of this feature
	 */
	public String getName(){
		return mName;
	}

	/**
	 * @return Copy of the list of  
	 */
	public List<Point> getPoints(){
		assert mPoints != null: "list of points null";
		return Collections.unmodifiableList(mPoints);
	}

	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append(mName + ":");
		for (Point P: mPoints) {
			buf.append(" (");
			buf.append(P.x);
			buf.append(", ");
			buf.append(P.y);
			buf.append(")");
		}
		return buf.toString();
	}

	/**
	 * Scales the feature down depending on the value passed in
	 * scale factor must be positive
	 * if scalefactor < 1 the points decrease in value
	 * if scalefactor > 1 the points are increases in value
	 * @param scaleFactor scale factor to adjust points
	 */
	public void scaleFeature(double scaleFactor){
		if (scaleFactor <= 0) {
			throw new IllegalArgumentException("Scalefactor cannot be 0 or negative. Argument: " + scaleFactor);
		}

		List<Point> scaledList = new ArrayList<Point>(mPoints.size());

		for (Point p : mPoints) {
			scaledList.add(new Point(p.x * scaleFactor, p.y * scaleFactor));
		}

		// Clear out the old points
		mPoints.clear();
		for (Point p: scaledList) {
			mPoints.add(p);
		}
	}

	/**
	 * Returns a bounding box for this appliance 
	 * @return bounding box surrounding all the points
	 */
	public Rect getBoundingBox(){
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (Point p: mPoints){
			if (p.x < minX) minX = p.x;
			if (p.x > maxX) maxX = p.x;
			if (p.y < minY) minY = p.y;
			if (p.y > maxY) maxY = p.y;
		}
		Point minPt = new Point(minX, minY);
		Point maxPt = new Point(maxX, maxY);
		Rect r = new Rect(minPt, maxPt);
		
		log.log(Level.INFO, "Bounding box found around feature " + mName + " TL:" + r.tl() + " BR:" + r.br());
		
		return r;
	}
}
