package uw.cse.mag.appliancereader.db.datatype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opencv.core.Mat;
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
		for (Point p: shape){
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

	/**
	 * Rotates this feature
	 * @param center center to rotate feature around
	 * @param radians 
	 */
	public void rotate(Point center, double radians) {
		List<Point> rotatedList = new ArrayList<Point>(mPoints.size());
		for (Point p : mPoints) {
			// translate point tP
			Point tP = new Point(p.x - center.x, p.y - center.y);
			// Rotated point 		
			Point rP = new Point(tP.x * Math.cos(radians) - tP.y * Math.sin(radians),
					tP.x * Math.sin(radians) + tP.y * Math.cos(radians));
			// Translate the point back
			rotatedList.add(new Point(rP.x + center.x, rP.y + center.y));
		}

		// Clear out the old points
		mPoints.clear();
		for (Point p: rotatedList) {
			mPoints.add(p);
		}
	}

	/**
	 * Rotates all the points of this feature by the matrix
	 * @param rotMat2by3
	 */
	public void rotate(Mat rotMat2by3) {
		double m11 = rotMat2by3.get(0, 0)[0];
		double m12 = rotMat2by3.get(0, 1)[0];
		double m13 = rotMat2by3.get(0, 2)[0];
		double m21 = rotMat2by3.get(1, 0)[0];
		double m22 = rotMat2by3.get(1, 1)[0];
		double m23 = rotMat2by3.get(1, 2)[0];
		
		List<Point> rotatedList = new ArrayList<Point>(mPoints.size());
		for (Point p : mPoints) {
			rotatedList.add(new Point(m11*p.x + m12*p.y + m13, m21*p.x + m22*p.y + m23));
		}
		
		// Clear out the old points
		mPoints.clear();
		for (Point p: rotatedList) {
			mPoints.add(p);
		}
	}
}
