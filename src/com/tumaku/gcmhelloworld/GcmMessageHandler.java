package com.tumaku.gcmhelloworld;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class GcmMessageHandler extends IntentService {

     String mes;
     private Handler handler;
    public GcmMessageHandler() {
        super("GcmMessageHandler");
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        handler = new Handler();
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        int red =0 ,green=0, blue=0, intensity=0;

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

       mes=null;
       String tmpString=extras.getString("score");
       if (tmpString!=null){
    	   mes = "Score: " + tmpString;
    	   tmpString= extras.getString("time");
    	   if (tmpString!=null){
    		   mes +="Time: " + tmpString;
    	   }
       } else {    	   
    	   tmpString=extras.getString(getResources().getString(R.string.intensity));
    	   if (tmpString!=null){
    		   mes =getResources().getString(R.string.intensity) +": " + tmpString; 
   			   try {
				   intensity=Integer.parseInt(tmpString);
			   } catch (Exception ex) {intensity=0;}
        	   tmpString=extras.getString(getResources().getString(R.string.color));
        	   if (tmpString!=null) {
        		   mes +="\n" +getResources().getString(R.string.color) +": " + tmpString; 
        		   try {
        			   JSONObject colorObject= new JSONObject(tmpString);
        			   tmpString=colorObject.getString(getResources().getString(R.string.red));
        			   try {
        				   red=Integer.parseInt(tmpString);
        			   } catch (Exception ex) {red=0;}
        			   if (tmpString!=null) mes +="\n" +getResources().getString(R.string.red) +": " + tmpString;
        			   tmpString=colorObject.getString(getResources().getString(R.string.green));
        			   try {
        				   green=Integer.parseInt(tmpString);
        			   } catch (Exception ex) {green=0;}
          			   if (tmpString!=null) mes +="\n" +getResources().getString(R.string.green) +": " + tmpString;
        			   tmpString=colorObject.getString(getResources().getString(R.string.blue));
        			   try {
        				   blue=Integer.parseInt(tmpString);
        			   } catch (Exception ex) {blue=0;}
        			   if (tmpString!=null) mes +="\n" +getResources().getString(R.string.blue) +": " + tmpString;
        		   } catch (JSONException jsonExc){}
        	   }
        	   //Send intent to TumakuBLEService service 
               Intent serviceIntent= new Intent(this, TumakuBLEService.class);
               serviceIntent.putExtra(TumakuBLEService.EXTRA_INTENSITY,intensity);
               serviceIntent.putExtra(TumakuBLEService.EXTRA_RED,red);
               serviceIntent.putExtra(TumakuBLEService.EXTRA_GREEN,green);
               serviceIntent.putExtra(TumakuBLEService.EXTRA_BLUE,blue);
               startService(serviceIntent);  

    	   }
       }
       if (mes==null) mes="BulbMessenger: Unsuported Google Cloud Messaging event";
       showToast();

       GcmBroadcastReceiver.completeWakefulIntent(intent);

    }

    public void showToast(){
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(),mes , Toast.LENGTH_SHORT).show();
            }
         });

    }
}