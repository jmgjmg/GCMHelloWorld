package com.tumaku.gcmhelloworld;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity  {

    Button btnRegId, btnSendPush;
    EditText etRegId;
    GoogleCloudMessaging gcm;
    String regid;
    String ServerId="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRegId = (Button) findViewById(R.id.btnGetRegId);
        btnSendPush = (Button) findViewById(R.id.btnPushNotification);
        etRegId = (EditText) findViewById(R.id.etRegId);

        btnRegId.setOnClickListener(new OnClickListener() {
    	    public void onClick(View v) {
    	    	getRegId();
    	    }		    
          }); 
        btnSendPush.setOnClickListener(new OnClickListener() {
    	    public void onClick(View v) {
    	    	pushMessage();
    	    }		    
          });        
    }
    
    public void getRegId(){
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }
                    regid = gcm.register(Constant.projectNumber);
                    msg = "Device registered, registration ID=" + regid;
                    storeRegId(regid);
                    Log.i("GCM",  msg);

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();

                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                etRegId.setText(msg + "\n");
            }
        }.execute(null, null, null);
    }
    
	 public void pushMessage(){
         String regIdValue= restoreRegId();
         if (regIdValue==null) {
         	Toast.makeText(this, "You must first obtain a RegId", Toast.LENGTH_SHORT).show();         	
         } else {
        	 new MyAsyncTask().execute(null,null,null);
         }
	 }
    
	 
	 private String restoreRegId() {
		    SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);        
		    return settings.getString(getString(R.string.regId), null);
	 }

    private void storeRegId(String value){
	    SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString(getResources().getString(R.string.regId),value);
	    editor.commit();
    }
	 
    private class MyAsyncTask extends AsyncTask<String, Integer, Void> {

        @Override
        protected Void doInBackground(String... params) {
            // TODO Auto-generated method stub
            sendPostHttpJSON();
            return null;
        }

        protected void onPostExecute(Double result) {

        }


        public  void sendPosHttpText() {

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();		
            String regIdValue= restoreRegId();

            formparams.add(new BasicNameValuePair("registration_id", restoreRegId()));

            formparams.add(new BasicNameValuePair("data.score", "5x1"));
            formparams.add(new BasicNameValuePair("data.time", "10:25"));
            // UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams,
            // "UTF-8");

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://android.googleapis.com/gcm/send");
                    HttpResponse response;

            httpPost.setHeader("Authorization",Constant.gcmAPIKey);
            httpPost.setHeader("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(formparams, "UTF-8"));
                //Get the response
                response = httpclient.execute(httpPost);

                int responseCode = response.getStatusLine().getStatusCode();
                 String responseText = Integer.toString(responseCode);      
                 System.out.println("HTTP POST : " + responseText);

                 /*Checking response */
                 if(response!=null){
                	 
                	 HttpEntity entity = response.getEntity();
                	 String responseBody="";
                	 if(entity != null)
                	 {
                	     responseBody = EntityUtils.toString(entity);
                	 } 
                     System.out.println("HTTP POST : " + responseBody);
                 }
                //Print result
                System.out.println(response.toString());

            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        public  void sendPostHttpJSON() {

            String regIdValue= restoreRegId();
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://android.googleapis.com/gcm/send");
                    HttpResponse response;

            httpPost.setHeader("Authorization", Constant.gcmAPIKey);
            httpPost.setHeader("Content-Type",
                    "application/json");
            String content= "{ \"data\": {\"score\": \"5x1\", \"time\": \"15:10\"}, " +
            				"  \"registration_ids\": [\""+ regIdValue +"\"]"+
            				"}";
        
            try {
                StringEntity se = new StringEntity(content);
                httpPost.setEntity(se);
                response = httpclient.execute(httpPost);

                int responseCode = response.getStatusLine().getStatusCode();
                 String responseText = Integer.toString(responseCode);      
                 System.out.println("HTTP POST : " + responseText);

                 /*Checking response */
                 if(response!=null){
                	 
                	 HttpEntity entity = response.getEntity();
                	 String responseBody="";
                	 if(entity != null)
                	 {
                	     responseBody = EntityUtils.toString(entity);
                	 } 
                     System.out.println("HTTP POST : " + responseBody);
                 }
                //Print result
                System.out.println(response.toString());

            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        
    }
}