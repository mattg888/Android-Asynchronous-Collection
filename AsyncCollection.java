package uk.co.mattgrundy.remote;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import android.os.AsyncTask;

public class AsyncCollection<T> extends AsyncTask<Void, Void, ArrayList<T>> implements Iterable<T> { 
    private CollectionResult<T> collectionResult;
	
	public ArrayList<T> products;
    public int count = 0;
    public String url;
    public HashMap<String, String> params;
    
    public Class<T> clazz;
    
    public int errorCode;
    
    public static int ERROR_NO_CONNECTION = 0;
    public static int ERROR_DATA = 1;
    public static int ERROR_LOCATION = 2;
    
	public AsyncCollection(CollectionResult<T> collectionResultIn, Class<T> clazzIn, String urlIn) {
		collectionResult = collectionResultIn;
		products = new ArrayList<T>();
		url = urlIn;
		params = new HashMap<String, String>();
		
		clazz = clazzIn;
	}
	
	public Iterator<T> iterator() {
		return products.iterator();
	}
	
	public void add(T product) {  
		products.add(product);
		count++;
	}
	
	/*
	 * Appends the params to the URL
	 */
	private String buildURL(){
		String urlOut = url + "?";
		
		int i = 0;
		for (String key : params.keySet()) {
			try {
				if(i > 0){
					urlOut += '&';
				}
				
				String value = params.get(key);
				
				if(value == null || value.compareTo("null") == 0){
					value = "";
				}
				
				urlOut += URLEncoder.encode(key, "UTF-8").toString() + '=' + URLEncoder.encode(value, "UTF-8").toString();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			i++;
		}
		
		System.out.println("**** (@" + this.getClass() + ") URL: " + urlOut);
		
		return urlOut;
	}
	
	/*
	 * Just a helper for adding a param
	 */
	public void addParam(String key, String value){
		params.put(key, value);
	}
	
	public ArrayList<T> toArray(){
		
		ArrayList<T> stringSongs = new ArrayList<T>();

        for (T thisProduct : products) {
        	stringSongs.add(thisProduct);
        }
		
        return stringSongs;
        
	}
	
	public T getAtIndex(int i){
		
		int j = 0;
		
		for(T product : products){
			if(j++ == i){
				return product;
			}
		}
		
		return null;
	}

	public InputStream getJSONData(String url){
        DefaultHttpClient httpClient = new DefaultHttpClient();
        URI uri;
        InputStream data = null;
        try {
            uri = new URI(url);
            HttpGet method = new HttpGet(uri);
            HttpResponse response = httpClient.execute(method);
            data = response.getEntity().getContent();
        } catch (Exception e) {
        	errorCode = ERROR_NO_CONNECTION;
        	
            e.printStackTrace();
        }
        
        return data;
    }
	
	@Override
	protected ArrayList<T> doInBackground(Void... arg0) {
		
			Reader r = null;
			JsonElement je = null;

			try{
		        Gson gson = new Gson();
		        
		        System.out.println("Got some GSON");
		        
		        InputStream stream = getJSONData(buildURL());

		        r = new InputStreamReader(stream);

		        JsonParser parser = new JsonParser();
		        
		        je = parser.parse(r);
		        
		        JsonArray array = je.getAsJsonArray();
	
		    	products = new ArrayList<T>(array.size() - 1);
		    	
		    	T product = null;
		    	
		        for(int i = 0, j = array.size(); i < j; i++){
		        	product = gson.fromJson(array.get(i), clazz);
		        	
		        	if(product != null){
		        		products.add(product);
		        	}
		        
		        }

		        if(stream != null){
		        	stream.close();
		        }
		        
		        if(r != null){
		        	r.close();
		        }
		        
	
	        } catch(Exception ex){
				
	        	if(je != null){
					if(je.getAsString().compareTo("LOCATION") == 0){
						errorCode = ERROR_LOCATION;
					} else {	
						errorCode = ERROR_DATA;
					}
	        	}
	        	
	        	System.out.println("Failed here");
	            ex.printStackTrace();
	        }
		
		return (ArrayList<T>) products;
	}
	
	@Override
	protected void onPreExecute(){
		collectionResult.start();
	}
	
	@Override
    protected void onPostExecute(ArrayList<T> prods) {    	
		collectionResult.getResult(prods);
		collectionResult.end();
	}
	
	public static abstract class CollectionResult<T>{
        public abstract void start();
		public abstract void getResult(ArrayList<T> products);
        public abstract void end();
    }
	
}
