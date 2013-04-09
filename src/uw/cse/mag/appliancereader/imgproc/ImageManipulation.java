package uw.cse.mag.appliancereader.imgproc;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;

/**
 * General Class for conducting standard general image processing techniques
 * using open cv library.
 * @author mhotan
 */
public class ImageManipulation {

	public static final Scalar RED = new Scalar(200, 0, 0, 255);
	public static final Scalar GREEN = new Scalar(0, 200, 0, 255);
	public static final Scalar BLUE = new Scalar(0, 0, 200, 255);
	
	/**
	 * Conducts standard erode procedure on the given image
	 * @param in Bitmap to erode
	 * @param kernel kernel to use for erosion
	 * @return Bitmap of eroded image
	 */
	public static Bitmap erode(Bitmap in, Mat kernel){
		Mat input = ImageConversion.bitmapToMat(in);
		Mat output = new Mat();
		Imgproc.erode(input, output, kernel);
		Bitmap outB = ImageConversion.matToBitmap(output);
		return outB;
	}
	
	/**
	 * Dilates the inputted image
	 * @param in
	 * @param kernel
	 * @return
	 */
	public static Bitmap dilate(Bitmap in, Mat kernel){
		Mat input = ImageConversion.bitmapToMat(in);
		Mat output = new Mat();
		Imgproc.dilate(input, output, kernel);
		Bitmap outB = ImageConversion.matToBitmap(output);
		return outB;
	}
	
	/**
	 * Create grey scale image
	 * @param src Source to be greyscaled
	 * @return grey scaled image
	 */
	public static Bitmap createGrayscale(Bitmap src) {
		int width = src.getWidth();
		int height = src.getHeight();
		Bitmap bmOut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bmOut);
		ColorMatrix ma = new ColorMatrix();
		ma.setSaturation(0);
		Paint paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(ma));
		canvas.drawBitmap(src, 0, 0, paint);
		return bmOut;
	}
	
	/**
	 * Compute the Mode grey value in the image
	 * Grey for pixel p[RGB] is calculated from (0.2989 * R + 0.5870 * G + 0.1140 * B)
	 * @param src image in question
	 * @return return Median grey value
	 */
	public static int computeModeGrey(Bitmap src) {
		
		int[] histogram = createGreyHistogram(src);
		int mode = 0;
		int currentMax = -1;
		
		// Find the grey value that occured the most
		for (int i = 0; i < histogram.length; i++){
			if (histogram[i] > currentMax){
				mode = i;
				currentMax = histogram[i];
			}
		}
		
		return mode;
	}

	/**
	 * Compute the median grey value in the image
	 * Grey for pixel p[RGB] is calculated from (0.2989 * R + 0.5870 * G + 0.1140 * B)
	 * @param src image in question
	 * @return return Median grey value
	 */
	public static int computeMedianGrey(Bitmap src) {
		int width = src.getWidth();
		int height = src.getHeight();

		int[] histogram = createGreyHistogram(src);
		int median = 0;
		
		int halfNumPix = width * height / 2;
		while (halfNumPix != 0 && median < 256) {
			while (halfNumPix != 0 && histogram[median] != 0) {
				histogram[median]--;
				halfNumPix--;
			}
			if (halfNumPix != 0) // Complete otherwise
				median++;
		}
		
		return median;
	}
	
	/**
	 * Create mapping where Histogram h has quality where h[i] = number of occurences in the image
	 * <br> where the value i, is between 0 - 255 and is calculated by (0.2989 * R + 0.5870 * G + 0.1140 * B) of every pixel
	 * @param src
	 * @return
	 */
	private static int[] createGreyHistogram(Bitmap src){
		int[] histogram = new int[256];
		
		int width = src.getWidth();
		int height = src.getHeight();
		
		int R, G, B;
		int pixel;
		int cGrey = 0;
		// scan through all pixels
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// get pixel color
				pixel = src.getPixel(x, y);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);
				cGrey = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);
				histogram[cGrey]++;
			}
		}
		return histogram;
	}
	
	/**
	 * Compute the average grey value in the image
	 * @param src
	 * @return
	 */
	public static int computeMeanGrey(Bitmap src) {
		int width = src.getWidth();
		int height = src.getHeight();

		int R, G, B;
		int pixel;
		long totalGrey = 0;
		// scan through all pixels
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// get pixel color
				pixel = src.getPixel(x, y);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);
				totalGrey += (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);
			}
		}
		return (int) (totalGrey / (width*height));
	}

	/**
	 * Creates a black and white image from src
	 * Pixel is determined to be black or white depending on the threshold 
	 * For pixel P with values RGB if (0.2989 * R + 0.5870 * G + 0.1140 * B) > threshold pixel is black, and white otherwise
	 * @param src Source to make copy from
	 * @param threshold threshold to compare for white and black
	 * @return Black and white image
	 */
	public static Bitmap createBlackAndWhite(Bitmap src, int threshold) {
		int width = src.getWidth();
		int height = src.getHeight();

		// Ensure threshold is between 0-255
		threshold = Math.max(Math.min(threshold, 255),0);

		// create output bitmap
		Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
		// color information
		int A, R, G, B;
		int pixel;

		// scan through all pixels
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// get pixel color
				pixel = src.getPixel(x, y);
				A = Color.alpha(pixel);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);
				int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);

				// use 128 as threshold, above -> white, below -> black
				if (gray > threshold) 
					gray = 255;
				else
					gray = 0;
				// set new pixel color to output bitmap
				bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
			}
		}
		return bmOut;
	}

	/**
	 * 
	 * @param text Text to be displayed in Image
	 * @param width 
	 * @param height
	 * @return
	 */
	public static Bitmap getTextImage(String text, int width, int height) {
		final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		final Paint paint = new Paint();
		final Canvas canvas = new Canvas(bmp);

		canvas.drawColor(Color.WHITE);

		paint.setColor(Color.BLACK);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);
		paint.setTextSize(24.0f);
		canvas.drawText(text, width / 2, height / 2, paint);

		return bmp;
	}
	
}
