package com.tumaku.gcmhelloworld;


import java.io.IOException;
import java.io.UnsupportedEncodingException;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ControlLightActivity extends Activity implements OnSeekBarChangeListener {
	

    private Context mContext;
	private SeekBar mSeekBarRed;
	private SeekBar mSeekBarGreen;
	private SeekBar mSeekBarBlue;
	private SeekBar mSeekBarIntensity;
	private TextView mTextRed;
	private TextView mTextGreen;
	private TextView mTextBlue;
	private TextView mTextIntensity;
	private Button mButtonSend;
	private Button mButtonStartBLE;
	private Button mButtonStopBLE;
	private JSONObject jsonPost;

	private TextView mTextColourSample;


	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.light_control);           
        mContext=this;
        mSeekBarRed = (SeekBar) findViewById(R.id.seekBarRed);
        mSeekBarRed.setOnSeekBarChangeListener(this);
        mSeekBarRed.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.OVERLAY));
        mSeekBarGreen = (SeekBar) findViewById(R.id.seekBarGreen);
        mSeekBarGreen.setOnSeekBarChangeListener(this);
        mSeekBarGreen.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.OVERLAY));
        mSeekBarBlue = (SeekBar) findViewById(R.id.seekBarBlue);
        mSeekBarBlue.setOnSeekBarChangeListener(this);
        mSeekBarBlue.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.OVERLAY));
        mSeekBarIntensity = (SeekBar) findViewById(R.id.seekBarIntensity);
        mSeekBarIntensity.setOnSeekBarChangeListener(this);
        mSeekBarIntensity.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
       
        mTextRed = (TextView) findViewById(R.id.textRed);
        mTextGreen = (TextView) findViewById(R.id.textGreen);
        mTextBlue = (TextView) findViewById(R.id.textBlue);
        mTextIntensity = (TextView) findViewById(R.id.textIntensity);
        mTextColourSample = (TextView) findViewById(R.id.colourSample);
        mTextRed.setTypeface(null, Typeface.BOLD);
        mTextGreen.setTypeface(null, Typeface.BOLD);
        mTextBlue.setTypeface(null, Typeface.BOLD);
        mTextIntensity.setTypeface(null, Typeface.BOLD);
        mButtonSend= (Button) findViewById(R.id.buttonSend);
        mButtonStartBLE= (Button) findViewById(R.id.buttonStartBLE);
        mButtonStopBLE= (Button) findViewById(R.id.buttonStopBLE);
        restoreValues();
        
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
        	    postGCM();
            }
        });
        
        mButtonStartBLE.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent serviceIntent= new Intent(mContext, TumakuBLEService.class);
                serviceIntent.putExtra(TumakuBLEService.EXTRA_ACTIVITY,TumakuBLEService.EXTRA_ACTIVITY);
                startService(serviceIntent);  
            }
        });
        
        mButtonStopBLE.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Intent intent= new Intent(mContext,TumakuBLEService.class);
            	stopService(intent);           
            }
        });
	
	}
	
	@Override
	public void onStop(){
		super.onStop();
		storeValues();
	}

	
    @Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	   updateDisplay();
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		storeValues();
		updateDisplay();
	}
	
	 
	 private String restoreRegId() {
		    SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);        
		    return settings.getString(getString(R.string.regId), null);
	 }		

	void postGCM(){
		try {
	         String regIdValue= restoreRegId();
	         if (regIdValue==null) {
	         	Toast.makeText(this, "You must first obtain a RegId", Toast.LENGTH_SHORT).show();         	
	         	return;
	         }
	         
			 JSONObject jsonObj = new JSONObject();
			 JSONObject jsonData = new JSONObject();
			 JSONObject jsonColor = new JSONObject();

			 jsonColor.put(getResources().getString(R.string.red), mSeekBarRed.getProgress()); 
			 jsonColor.put(getResources().getString(R.string.green), mSeekBarGreen.getProgress()); 
			 jsonColor.put(getResources().getString(R.string.blue), mSeekBarBlue.getProgress()); 

			 jsonData.put(getResources().getString(R.string.color), jsonColor); 
			 jsonData.put(getResources().getString(R.string.intensity), mSeekBarIntensity.getProgress()); 
			 
		     jsonObj.put(getResources().getString(R.string.data), jsonData);
		     
		     JSONArray jsonArray = new JSONArray();
		     jsonArray.put(regIdValue);
			 jsonObj.put(getResources().getString(R.string.regIds), jsonArray); 
			 Log.i("JMG",jsonData.toString());
			 //Toast.makeText(this, jsonObj.toString(), Toast.LENGTH_LONG).show();  
			 jsonPost=jsonObj;
        	 new MyAsyncTask().execute(null,null,null);

		} catch(JSONException ex) {
	        ex.printStackTrace();
	    }
	}

	
	
	
    private void restoreValues() {
	    SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);        
	    int redValue= settings.getInt(getString(R.string.red),0);
	    int greenValue= settings.getInt(getString(R.string.green),0);
	    int blueValue= settings.getInt(getString(R.string.blue),0);
	    int intensityValue= settings.getInt(getString(R.string.intensity),0);
	    
	    mSeekBarRed.setProgress(redValue);
	    mSeekBarGreen.setProgress(greenValue);
	    mSeekBarBlue.setProgress(blueValue);
	    mSeekBarIntensity.setProgress(intensityValue);
	    updateDisplay();
    }
	
    private void storeValues(){
	    SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putInt(getString(R.string.red),mSeekBarRed.getProgress());
	    editor.putInt(getString(R.string.green),mSeekBarGreen.getProgress());
	    editor.putInt(getString(R.string.blue),mSeekBarBlue.getProgress());
	    editor.putInt(getString(R.string.intensity),mSeekBarIntensity.getProgress());
	    editor.commit();
    }
    
    
    private  void updateDisplay(){
	   	 mTextRed.setText(getString(R.string.red)+": "+ mSeekBarRed.getProgress());
		 mTextGreen.setText(getString(R.string.green)+": "+ mSeekBarGreen.getProgress());
		 mTextBlue.setText(getString(R.string.blue)+": "+ mSeekBarBlue.getProgress());
		 mTextIntensity.setText(getString(R.string.intensity)+": "+ mSeekBarIntensity.getProgress());
    	 mTextColourSample.setBackgroundColor(
    	    Color.rgb(mSeekBarRed.getProgress(),mSeekBarGreen.getProgress(),mSeekBarBlue.getProgress()));
    }
    

    
    private class MyAsyncTask extends AsyncTask<String, Integer, Void> {

        @Override
        protected Void doInBackground(String... params) {
            // TODO Auto-generated method stub
            sendPostHttpJSON();
            return null;
        }

        public  void sendPostHttpJSON() {

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://android.googleapis.com/gcm/send");
                    HttpResponse response;

            httpPost.setHeader("Authorization",
                    Constant.gcmAPIKey);
            httpPost.setHeader("Content-Type",
                    "application/json");

        
            try {
                StringEntity se = new StringEntity(jsonPost.toString());
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
