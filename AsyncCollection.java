package uk.co.mattgrundy.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

import org.apache.commons.codec.digest.UnixCrypt;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import uk.co.createanet.Functions;
import uk.co.createanet.UserManager;
import uk.co.createanet.struts.AsyncObject;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/*
 *
 * Params
 * 
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("email", email_address);
 * 
 * Multiple data result
 * 
		(new Async<AsyncObjects>(ForgottenPasswordActivity.this, new Async.CollectionResult<AsyncObjects>() {
		
			public void start() {
				dialog = ProgressDialog.show(ForgottenPasswordActivity.this, "", "Sending password reminder...", true);
			}
		
			public void getResult(AsyncObjects result) {
				// we can just ignore this
				if(result != null && result.response.compareToIgnoreCase("Success") == 0){
					ForgottenPasswordActivity.this.finish();
				}
			}
		
			public void end() {
				dialog.dismiss();
			}
		
		}, AsyncObjects.class, "test_android.php", params, true)).execute();
		
 * 
 * Single data result
 * 
    	(new Async<AsyncObject>(ForgottenPasswordActivity.this, new Async.CollectionResult<AsyncObject>() {

			public void start() {
				dialog = ProgressDialog.show(ForgottenPasswordActivity.this, "", "Sending password reminder...", true);
			}

			public void getResult(AsyncObject result) {
				// we can just ignore this
				if(result != null && result.response.compareToIgnoreCase("Success") == 0){
					ForgottenPasswordActivity.this.finish();
				}
			}

			public void end() {
				dialog.dismiss();
			}

		}, AsyncObject.class, "test_android.php", params, true)).execute();
		    	
 * 
 */

public class AsyncCollection <V extends AsyncObject> extends AsyncTask<Void, Void, V> { 

    protected CollectionResult<V> collectionResult;
	
	public V product;
    public String url;
    
    public HashMap<String, String> params;
    public HashMap<String, String> files;
    
    public boolean authenticate;
    
    public Class<V> clazz;
    public Context c;
    
	// maximum image dimension (x / y)
	private int maxD = 500;

	public AsyncCollection(Context cIn, CollectionResult<V> collectionResultIn, Class<V> clazzIn, String urlIn) {
		this(cIn, collectionResultIn, clazzIn, urlIn, new HashMap<String, String>());
	}
	
	public AsyncCollection(Context cIn, CollectionResult<V> collectionResultIn, Class<V> clazzIn, String urlIn, HashMap<String, String>paramsIn) {
		this(cIn, collectionResultIn, clazzIn, urlIn, new HashMap<String, String>(), false);
	}
	
	public AsyncCollection(Context cIn, CollectionResult<V> collectionResultIn, Class<V> clazzIn, String urlIn, HashMap<String, String>paramsIn, boolean authenticateIn) {
		this(cIn, collectionResultIn, clazzIn, urlIn, paramsIn, new HashMap<String, String>(), authenticateIn);
	}
    
	public AsyncCollection(Context cIn, CollectionResult<V> collectionResultIn, Class<V> clazzIn, String urlIn, HashMap<String, String>paramsIn, HashMap<String, String> filesIn, boolean authenticateIn) {
		collectionResult = collectionResultIn;
		url = urlIn;
		params = paramsIn;
		c = cIn;
		authenticate = authenticateIn;
		files = filesIn;
		
		clazz = clazzIn;
	}
	
	public byte[] resizeImage(String file, BitmapFactory.Options bmpFactoryOptions) throws IOException{
		// get the image and resize
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		int heightRatio = (int)android.util.FloatMath.ceil(bmpFactoryOptions.outHeight/(float)maxD);
		int widthRatio = (int)android.util.FloatMath.ceil(bmpFactoryOptions.outWidth/(float)maxD);
    
		if (heightRatio > 1 || widthRatio > 1){
			if (heightRatio > widthRatio){
				bmpFactoryOptions.inSampleSize = heightRatio;
			} else {
				bmpFactoryOptions.inSampleSize = widthRatio; 
			}
		}
    
		bmpFactoryOptions.inJustDecodeBounds = false;
		Bitmap image = BitmapFactory.decodeFile(file, bmpFactoryOptions);

		image.compress(Bitmap.CompressFormat.JPEG, 80, stream);
		
		byte[] byte_arr = stream.toByteArray();
		stream.close();
		
		image = null;
		
		return byte_arr;
	}
	
	protected V doInBackground(Void... arg0) {

		try{
	        Reader r = getInputStream();
	        
	        // with MySQL date format
			Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd" /* HH:mm:ss" */).create();
	        
	        product = gson.fromJson(r, clazz);
	        
	        if(r != null){
	        	r.close();
	        }

        } catch(Exception ex){
            ex.printStackTrace();
        }
		
		return product;
	}

	
	/*
	 * Appends the params to the URL
	 */
    protected String buildURL(){
		String urlOut = Functions.API_BASE + url;

		System.out.println("**** (@" + this.getClass() + ") URL: " + urlOut);
		
		return urlOut;
	}

	/*
	 * Just a helper for adding a param
	 */
	public void addParam(String key, String value){
		params.put(key, value);
	}
	
	public Reader getInputStream() throws IOException {
		
		HttpClient httpclient = new DefaultHttpClient();
		   
		
		MultipartEntity reqEntity = new MultipartEntity();
		reqEntity = buildParams(reqEntity);
				
		HttpPost httppost = new HttpPost(buildURL());
		
        if(authenticate){
        	String encoding = Base64.encodeToString((Functions.API_USER + ":" + Functions.API_PASS).getBytes(), Base64.NO_WRAP);
        	httppost.setHeader("Authorization", "Basic " + encoding);
        }
        
		httppost.setEntity(reqEntity);
		
		HttpResponse response = httpclient.execute(httppost);
		
		InputStream is = response.getEntity().getContent();
		
		return new InputStreamReader(is);
	}
	
	protected MultipartEntity buildParams(MultipartEntity entity) throws IOException{
		
		for (String key : params.keySet()) {

			String value = params.get(key);
			
			if(value == null || value.compareTo("null") == 0){
				value = "";
			}
			
			entity.addPart(key, new StringBody(value));

		}
		
		if(files != null){

			for(String key : files.keySet()){
				String value = files.get(key);

				BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
		        bmpFactoryOptions.inJustDecodeBounds = true;

				byte[] resized = resizeImage(value, bmpFactoryOptions);
				
				ContentBody imageOut = new ByteArrayBody(resized, key + ".jpg");
				entity.addPart(key, imageOut);
				
			}
			
		}

		if(authenticate){
			entity.addPart("apikey", new StringBody(Functions.API_KEY));
		
			if(UserManager.getLogin(c) && UserManager.user != null){
				entity.addPart("user_id", new StringBody("" + UserManager.user.id));
				entity.addPart("passphrase", new StringBody(UnixCrypt.crypt("" + UserManager.user.id, Functions.API_SALT)));
			}
		}
		
		return entity;
	}
	
	@Override
	protected void onPreExecute(){
		collectionResult.start();
	}
	
	@Override
    protected void onPostExecute(V product) {
		
		// check for error messages / alerts to display
		if(product != null){
			if(product.response.compareToIgnoreCase("Error") == 0){
				Toast.makeText(c, product.message, Toast.LENGTH_SHORT).show();
			} else if(product.response.compareToIgnoreCase("Alert") == 0){
				Toast.makeText(c, product.message, Toast.LENGTH_SHORT).show();
			}
		
			collectionResult.getResult(product);
			
		} else {
			// generally no internet connection
			Toast.makeText(c, "Sorry, the service is current unavailable. Please make sure you have a working internet connection", Toast.LENGTH_SHORT).show();
		}

		collectionResult.end();
	}
	
	public static interface CollectionResult<T>{
        public abstract void start();
		public abstract void getResult(T products);
        public abstract void end();
    }

}
