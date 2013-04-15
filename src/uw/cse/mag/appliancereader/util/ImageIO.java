package uw.cse.mag.appliancereader.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import uw.cse.mag.appliancereader.imgproc.Size;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

public class ImageIO {
	private static final String TAG = ImageIO.class.getSimpleName();

	public static void downloadImgs(URL url) throws ClientProtocolException, IOException{
		HttpGet httpRequest = null;

		try {
			httpRequest = new HttpGet(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		HttpClient httpclient = new DefaultHttpClient();

		HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);

		HttpEntity entity = response.getEntity();

		BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);

		InputStream instream = bufHttpEntity.getContent();

		Bitmap bmp = BitmapFactory.decodeStream(instream);
	}

	/**
	 * Saves a Bitmap image to the string path
	 * @param bmp Bitmap image to save
	 * @param path relative or distinct path to 
	 * @return
	 */
	public static Uri saveBitmapToFile(Bitmap bmp, String path) {
		Uri u = null;
		try {
			u = null;
			File f = new File(path);
			u = Uri.fromFile(f);
			FileOutputStream out = new FileOutputStream(f);
			bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Unable to save Bitmap image");
			e.printStackTrace();
		}
		return u;
	}

	/**
	 * Save an image as a JPG to the relative or distinct path
	 * client must have writable access to path
	 * @param img Image to save at path
	 * @param path Path to save image at
	 */
	public static void savePhotoJPEG(Bitmap img, String path) {
		File file = new File(path);
		try {
			file.createNewFile();
			FileOutputStream os = new FileOutputStream(file);
			img.compress(Bitmap.CompressFormat.JPEG, 100, os);
			os.flush();
			os.close();
		} catch (Exception e) {
			Log.e(TAG, "Unable to save image as a Bitmap: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static Size getSizeOfImage(ContentResolver resolver, Uri uri){
		// Open an initial input stream to for either loading the image or the sizes
		// depending on the open 
		try {
			InputStream is = resolver.openInputStream(uri);
			BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
			bmpOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is,null, bmpOptions);
			Size toReturn = new Size(bmpOptions.outWidth, bmpOptions.outHeight);
			is.close();
			return toReturn;
		} catch (IOException e) {
			Log.e(TAG, "decodeSize:" + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public static Size getSizeOfImage(String filePath){
		BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
		bmpOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, bmpOptions);
		return new Size(bmpOptions.outWidth, bmpOptions.outHeight);
	}

	public static int getOrientationOfImage(ContentResolver resolver, Uri uri){
		Size current = getSizeOfImage(resolver, uri);
		if (current.width > current.height)
			return Configuration.ORIENTATION_LANDSCAPE;
		return Configuration.ORIENTATION_PORTRAIT;
	}

	public static int getOrientationOfImage(String filePath){
		Size current = getSizeOfImage(filePath);
		if (current.width > current.height)
			return Configuration.ORIENTATION_LANDSCAPE;
		return Configuration.ORIENTATION_PORTRAIT;
	}

	/**
	 * Uses resolver to load the Bitmap image stored at the path distinguished at uri.
	 * If a size is specified then the image will attempted to be loaded at that size
	 * @param resolver Content resolver that can extract the image
	 * @param uri URI that points to the image
	 * @param size Desired size of the image
	 * @return Bitmap image at desired size, or full size if size argument is invalid or null
	 */
	public static Bitmap loadBitmapFromURI(ContentResolver resolver, Uri uri, Size size){
		if (resolver == null)
			throw new IllegalArgumentException("Null resolver at load of URI: " + uri);
		if (uri == null)
			throw new IllegalArgumentException("Null uri attempted to load");

		Bitmap image = null;
		try {
			// Open an initial input stream to for either loading the image or the sizes
			// depending on the open 
			InputStream is = resolver.openInputStream(uri);
			if (size != null && size.width > 0 && size.height > 0){
				BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
				bmpOptions.inJustDecodeBounds = true;

				BitmapFactory.decodeStream(is,null, bmpOptions);
				int currHeight = bmpOptions.outHeight;
				int currWidth = bmpOptions.outWidth;

				is.close();
				InputStream is2 = resolver.openInputStream(uri);

				int sampleSize = 1;
				{
					//use either width or height
					if (currWidth>currHeight)
						sampleSize = Math.round((float)currHeight/(float)size.height);
					else
						sampleSize = Math.round((float)currWidth/(float)size.width);
				}
				bmpOptions.inSampleSize = sampleSize;
				bmpOptions.inJustDecodeBounds = false;

				//decode the file with restricted sizee
				image = BitmapFactory.decodeStream(is2, null, bmpOptions);
				is2.close();
			} else {
				image = BitmapFactory.decodeStream(is);
				is.close();
			}
			return image;
		} catch (IOException e) {
			Log.e(TAG, "Exception when reading: "+ e);
			return image;
		}
	}

	/**
	 * Load an image from the designated relative or complete path.
	 * Relative path should be determined by the context of the caller
	 * generally it is safe to use absolute paths
	 * @param filePath Path to the image
	 * @param size Desired size of the image to be returned
	 * @return Image at desired size or full size
	 */
	public static Bitmap loadBitmapFromFilePath(String filePath, Size size){
		if (filePath == null)
			throw new IllegalArgumentException("Null Filepath for loading");

		if (size != null && size.width > 0 && size.height > 0) {
			// 
			BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
			bmpOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filePath, bmpOptions);
			int currHeight = bmpOptions.outHeight;
			int currWidth = bmpOptions.outWidth;

			// Find the correct sample size
			int sampleSize = 1;
			{
				//use either width or height
				if ((currWidth>currHeight))
					sampleSize = Math.round((float)currHeight/(float)size.height);
				else
					sampleSize = Math.round((float)currWidth/(float)size.width);
			}

			bmpOptions.inSampleSize = sampleSize;
			bmpOptions.inJustDecodeBounds = false;
			//decode the file with restricted sizee
			return BitmapFactory.decodeFile(filePath, bmpOptions);
		} else { // return full size image
			return BitmapFactory.decodeFile(filePath);
		}
	}
}
