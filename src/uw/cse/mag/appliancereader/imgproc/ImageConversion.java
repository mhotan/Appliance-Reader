package uw.cse.mag.appliancereader.imgproc;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import android.graphics.Bitmap;

public class ImageConversion {

	/**
	 * Converts Mat to Bitmap
	 * @param m Mat instance to convert to Bitmap
	 * @return Bitmap conversion
	 */
	public static Bitmap matToBitmap(Mat m){
		Bitmap disp = Bitmap.createBitmap(m.cols(), m.rows(),
				Bitmap.Config.ARGB_8888); // Android uses ARGB_8888
		Utils.matToBitmap(m, disp);
		return disp;
	}
	
	/**
	 * Converts Bitmap image to Mat and returns to the client
	 * @param b bitmap image to convert
	 * @return Mat instance which is the equivalent
	 */
	public static Mat bitmapToMat(Bitmap b) {
		Mat mat = new Mat();
		Utils.bitmapToMat(b, mat);
		return mat;
	}
	
}
