package uw.cse.mag.appliancereader.db.datatype;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Downloads XML features asyncronously
 * @author mhotan
 */
public class AsyncFeatureDownloader extends AsyncTask<URL, Integer, String> {

	private static final String TAG = AsyncFeatureDownloader.class.getSimpleName();
	
	private static final int CAPACITY = 1024;

	@Override
	protected String doInBackground(URL... params) {
		String xml = null;

		try {
			URI uri = params[0].toURI();
			// defaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(uri);

			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			xml = EntityUtils.toString(httpEntity);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			Log.e(TAG, "Unable to convert URL: " + params[0] + " into a URI");
		}
		// return XML
		return xml;
	}
}
