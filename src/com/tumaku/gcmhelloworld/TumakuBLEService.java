package com.tumaku.gcmhelloworld;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class TumakuBLEService extends Service implements LeScanCallback{
	
	public static final String NOTIFICATION = "com.tumaku.ble.NOTIFICATION";
	public static final String STATE = "com.tumaku.ble.STATE";
	public static final String MOVEMENT = "com.tumaku.ble.MOVEMENT";
	public static final String YEELIGHT_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
	public static final String CHARACTERISTIC_CONTROL = "0000fff1-0000-1000-8000-00805f9b34fb";
	public static final String DEVICE_CONNECTED ="com.yeelight.blue.DEVICE_CONNECTED2";
	public static final String DEVICE_DISCONNECTED ="com.yeelight.blue.DEVICE_DISCONNECTED2";		
	public static final String SERVICES_DISCOVERED ="com.yeelight.blue.SERVICES_DISCOVERED";
	public static final String WRITE_SUCCESS = "com.yeelight.blue.WRITE_SUCCESS";
	public static final String WRITE_DESCRIPTOR_SUCCESS = "com.yeelight.blue.WRITE_DESCRIPTOR_SUCCESS";
	public static final String READ_SUCCESS = "com.yeelight.blue.READ_SUCCESS";
	public static final String EXTRA_ADDRESS = "address";
	public static final String EXTRA_NAME = "name";
	public static final String EXTRA_CHARACTERISTIC  = "characteristic";
	public static final String EXTRA_VALUE = "value";
	public static final String EXTRA_VALUE_BYTE_ARRAY = "valueByteArray";
	public static final String EXTRA_SCANNING = "scanning";
	public static final String EXTRA_DEVICE  = "device";
	public static final String EXTRA_SERVICE  = "service";
	public static final String EXTRA_RESULT  = "result";
	public static final String EXTRA_FULL_RESET = "fullreset";
	public static final String EXTRA_ACTIVITY = "activity";
	public static final String EXTRA_RED = "red";
	public static final String EXTRA_GREEN = "green";
	public static final String EXTRA_BLUE = "blue";
	public static final String EXTRA_INTENSITY = "intensity";

	
	private static BluetoothDevice mDevice;
	
	private static String mDeviceAddress;
	
	private static BluetoothGatt mGatt;
	
	private static TumakuBLEService mTumakuBLEService;

	private static List<ServiceType> mServices = new ArrayList<ServiceType>();
	
	private  MyCallBack mCallBack =new MyCallBack();
	
	private static BluetoothAdapter mBtAdapter=null;	
	
	
	private final IBinder mBinder = new TumakuBLEBinder();

	
	private final static int STATE_CONNECT=0;
	private final static int STATE_SEARCH_SERVICES=1;
	private final static int STATE_READ=2;
	private final static int STATE_WRITE=3;
	private final static int STATE_DISCONNECTED=4;
	
	private final static HashMap <Integer, String> mHashStateNames = new HashMap<Integer, String>() {
		{ put(STATE_CONNECT, "Connect"); 
		  put(STATE_SEARCH_SERVICES, "Search Services"); 
		  put(STATE_READ, "Read Characteristic"); 
		  put(STATE_WRITE, "Write Characteristic"); 
		  put(STATE_DISCONNECTED, "Disconnected"); 		  
		}};
	
	private static int mState=STATE_DISCONNECTED;
	private static boolean mBroadcastFlag=false;
	private static boolean mServiceOn=false;
	

	// http://www.jjoe64.com/2011/09/show-toast-notification-from-service.html
	private Handler mToastHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		mToastHandler = new Handler();

		if (Constant.DEBUG) {
			Log.i("JMG", "onCreate() TumakuBLEServer");				
			//Toast.makeText(getApplicationContext(),"onCreate() TumakuBLEServer", Toast.LENGTH_SHORT).show();
		}	
		mTumakuBLEService=this;
        SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);
        mBroadcastFlag=settings.getBoolean(getString(R.string.receiveBroadcast), false);
	    mServiceOn= settings.getBoolean(getString(R.string.serviceOn), false);
	    mDeviceAddress= settings.getString(getString(R.string.address), getResources().getString(R.string.defaultAddress));
		mState=STATE_DISCONNECTED;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (Constant.DEBUG) {
			Log.i("JMG", "onStartCommand() TumakuBLEServer");				
			//Toast.makeText(getApplicationContext(),"onStartCommand() TumakuBLEServer", Toast.LENGTH_SHORT).show();
		}
		if (intent==null) {
			if (Constant.DEBUG) {
				Log.i("JMG", "onStartCommand() by System. START_STICKY");				
				if (mServiceOn) {
					startBLEFunctions();					
				}
				sendToast("START_STICKY TumakuBLEServer");				
			}
		} else {
			if (intent.getStringExtra(EXTRA_VALUE)!=null) {
				if (Constant.DEBUG) {
					Log.i("JMG", "onStartCommand() because of onTaskRemoved() TumakuBLEServer");				
					sendToast("onStartCommand() because of onTaskRemoved() TumakuBLEServer");
				}		
				if (mServiceOn) {
					startBLEFunctions();
				}			
			} else {
				if (intent.getStringExtra(EXTRA_ACTIVITY)!=null) {
					if (Constant.DEBUG) {
						Log.i("JMG", "onStartCommand() TumakuBLEServer from Activity");				
						//.makeText(getApplicationContext(),"onStartCommand() TumakuBLEServer from Activity", Toast.LENGTH_SHORT).show();
					}		
					if (isStateDisconnected()) {
						startBLEFunctions();
					}			
				} else {
				    if (intent.getIntExtra(EXTRA_RED,9999)!=9999) {
				    	int red =intent.getIntExtra(EXTRA_RED, 0);
				    	if ((red<0)||(red>255))red=0;
				    	int green =intent.getIntExtra(EXTRA_GREEN, 0);
				    	if ((green<0)||(green>255))green=0;
				    	int blue =intent.getIntExtra(EXTRA_BLUE, 0);
				    	if ((blue<0)||(blue>255))blue=0;
				    	int intensity =intent.getIntExtra(EXTRA_INTENSITY, 0);			
				    	if ((intensity<0)||(intensity>100))intensity=0;
				    	storeYeelightValues(red, green, blue, intensity);
				    	if (isConnected()&&mState==STATE_WRITE) nextState();
				    	else sendToast("TumakuBLEService not started or ready. Start BLE in COntrolLightActivity");
				    }
				}
			}
			
		}
		
        return Service.START_STICKY;
	}
	
	
	@Override
	public void onDestroy(){
		resetTumakuBLE();
		if (Constant.DEBUG) {
			Log.i("JMG", "onDestroy() TumakuBLEServer");				
			//Toast.makeText(getApplicationContext(),"onDestroy() TumakuBLEServer", Toast.LENGTH_SHORT).show();
		}	
	}
	
	public class TumakuBLEBinder extends Binder {
		TumakuBLEService getService() {
			return TumakuBLEService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (Constant.DEBUG) {
			Log.i("JMG", "onBind() TumakuBLEServer");				
			//Toast.makeText(getApplicationContext(),"onBindCommand() TumakuBLEServer", Toast.LENGTH_SHORT).show();
		}
		return mBinder;

	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
		if (Constant.DEBUG) {
			Log.i("JMG", "onUnbind() TumakuBLEServer");				
			//Toast.makeText(getApplicationContext(),"onUnbindCommand() TumakuBLEServer", Toast.LENGTH_SHORT).show();
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onTaskRemoved(Intent rootIntent){
		// https://groups.google.com/forum/#!topic/android-developers/H-DSQ4-tiac
		if (Constant.DEBUG) {
			Log.i("JMG", "onTaskRemoved() TumakuBLEServer");				
			//Toast.makeText(getApplicationContext(),"onTaskRemoved() TumakuBLEServer", Toast.LENGTH_SHORT).show();
		}
		Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
		restartServiceIntent.setPackage(getPackageName());
		restartServiceIntent.putExtra(EXTRA_VALUE, EXTRA_VALUE);
		
		PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
		AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		alarmService.setExact(
		AlarmManager.ELAPSED_REALTIME,
		SystemClock.elapsedRealtime() + 5000,
		restartServicePendingIntent);
		super.onTaskRemoved(rootIntent);
	}
	
	private void sendToast(final String text) {
		if (mToastHandler!=null) {
			mToastHandler.post(new Runnable() {
			   @Override
			   public void run() {
			      Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();			   
			   }
			});
		} else {
			try {
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();	
			} catch (Exception ex) {
				if (Constant.DEBUG) {
					Log.i("JMG", "Could not send Toast from TumakuBLEServer because of wrong context: "+ text);				
					Toast.makeText(getApplicationContext(),"onUnbindCommand() TumakuBLEServer", Toast.LENGTH_SHORT).show();
				}				
			}
		}

	}
	
	public static void  resetTumakuBLE() {
		disconnect();
		if (mServices!=null) mServices.clear();
	}

	public void  fullResetTumakuBLE() {
		resetTumakuBLE();
		bluetoothInitialisation();
	}

	public void bluetoothInitialisation() {	
        BluetoothManager btManager = (BluetoothManager) mTumakuBLEService.getSystemService(BLUETOOTH_SERVICE);
        mBtAdapter = btManager.getAdapter();
	    if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
			if (Constant.DEBUG) {
				Log.i("JMG", "Bluetooth is disabled. Stop TumakuBLEService");				
				sendToast("Enable Bluetooth on this device");
			}
	        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        mTumakuBLEService.startActivity(enableBtIntent);
	        mTumakuBLEService.stopSelf();
	        return;
	    }
	    if (!mTumakuBLEService.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			if (Constant.DEBUG) {
				Log.i("JMG", "Bluetooth Low energy not supported. Stop TumakuBLEService");				
				sendToast("Bluetooth Low energy not supported on this device");
			}
			mTumakuBLEService.stopSelf();
	    	return;
	    }
	}

	public void startBLEFunctions(String address) {
		mDeviceAddress=address;	
        SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);
        SharedPreferences.Editor editor = settings.edit();
    	editor.putString(getString(R.string.address),mDeviceAddress);
        editor.commit();

		startBLEFunctions();
	}
	
	
	public void storeYeelightValues(int red, int green, int blue, int intensity) {
        SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);
        SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(getString(R.string.red),red);
    	editor.putInt(getString(R.string.green),green);
    	editor.putInt(getString(R.string.blue),blue);
    	editor.putInt(getString(R.string.intensity),intensity);
        editor.commit();
	}	
	
	public String formatYeelightControlString() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);
        int red =settings.getInt(getString(R.string.red), 0);
        int green =settings.getInt(getString(R.string.green), 0);
        int blue =settings.getInt(getString(R.string.blue), 0);
        int intensity =settings.getInt(getString(R.string.intensity), 0);
    	String result= "" + red + "," + green + "," + blue + "," + intensity + ",";
        while (result.length()<18){result+=",";}
    	return result;
	}

	private  void startBLEFunctions() {
		fullResetTumakuBLE();
		setTumakuBLEState(STATE_CONNECT);
		nextState();
	}
	
	public  void setBroadcastFlag(boolean newFlag) {
		mBroadcastFlag=newFlag;
        SharedPreferences settings = getSharedPreferences(getString(R.string.prefs_file), 0);
        SharedPreferences.Editor editor = settings.edit();
    	editor.putBoolean(getString(R.string.receiveBroadcast),mBroadcastFlag);
        editor.commit();		
	}

	
	public String getCurrentValues() {
		String result="";
		result+="State: " + getState() + "\n";
		result+="Address: " + mDeviceAddress + "\n";
		return result;
	}

	public void stopBLEFunctions(){
		disconnect();
		setTumakuBLEState(STATE_DISCONNECTED);
	}
	
	public  void setTumakuBLEState(int newState){
		mState=newState;
		
		if (mBroadcastFlag){
			Intent intent = new Intent(STATE);
			intent.putExtra(EXTRA_VALUE, mHashStateNames.get(mState));
			mTumakuBLEService.sendBroadcast(intent);				
		}
	}

	public String getState(){
		return mHashStateNames.get(mState);
	}

	public boolean isStateDisconnected() {
		return (mState==STATE_DISCONNECTED);
	}
	
	public String getDeviceAddress() {
		return mDeviceAddress;		
	}
	
	public List<ServiceType> getServices() {
		return mServices;		
	}
	
	public ServiceType getService(String serviceUUID) {
		for (ServiceType serviceInList : mServices)  {
			if (serviceInList.getService().getUuid().toString().equalsIgnoreCase(serviceUUID)) 
				return serviceInList;
		}
		return null;
	}
	
	
	
	protected  void nextState(){
		switch(mState) {			 
		   case (STATE_CONNECT):
		       if (Constant.DEBUG) Log.i("JMG","State Connected");
		       connect();
		       break;
		   case(STATE_SEARCH_SERVICES):
		       if (Constant.DEBUG) Log.i("JMG","State Search Services");
			   discoverServices();
		       break;			   
		   case(STATE_READ):
		       if (Constant.DEBUG) Log.i("JMG","State Read");
		   	   read(YEELIGHT_SERVICE,CHARACTERISTIC_CONTROL);
			   break;
		   case(STATE_WRITE):
		       if (Constant.DEBUG) Log.i("JMG","State Write");
		   	   String controlString= formatYeelightControlString();
		   	   write(YEELIGHT_SERVICE,CHARACTERISTIC_CONTROL, controlString);
			   break;
		   case(STATE_DISCONNECTED):
		       if (Constant.DEBUG) Log.i("JMG","State Disconect");
			   break;   
		   default:
		       if (Constant.DEBUG) Log.i("JMG","State Notify IR TEmperatureUnknown State: " + Integer.toString(mState));			   
		}			
		
	}
	
	
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		String name="unknown";
		if (device.getName()!=null) name=device.getName();
		final String finalName =name;
		final String  finalAddress = device.getAddress();		
	    if (Constant.DEBUG) Log.i("JMG", "Found new device "+ finalAddress + " --- Name: " + finalName);
	}
	
	public void startLeScan() {
		mBtAdapter.startLeScan(this);
	}
	
	public void stopLeScan() {
		mBtAdapter.stopLeScan(this);
	}
	
	public  void connect() {
		mDevice   = mBtAdapter.getRemoteDevice(mDeviceAddress);
		mServices.clear();
		if(mGatt!=null){
			mGatt.connect();
		}else{
			mDevice.connectGatt(this, true, mCallBack);  //false: connect now to peripheral
					//true: connect whenever the peripheral device is available
		}
	}

	public static void discoverServices() {
		if (Constant.DEBUG)
			Log.i("JMG", "Scanning services and caracteristics");				
		mGatt.discoverServices(); 
	}
	
	public static void disconnect(){
		if (mGatt!=null) {
			try{
				mGatt.disconnect();
				mGatt.close();
				if (Constant.DEBUG)
					Log.i("JMG", "Disconnecting GATT");				
			} catch(Exception ex){};
		}
		mGatt = null;
	}

	public static boolean isConnected(){
		return (mGatt!=null);
	}

		
	
	public static BluetoothGattCharacteristic findCharacteristic(String serviceUUID, String characteristicUUID){
		
		for (ServiceType serviceInList : mServices) {
			if (serviceInList.getService().getUuid().toString().equalsIgnoreCase(serviceUUID) ){
				for (BluetoothGattCharacteristic characteristicInList : serviceInList.getCharacteristics()) {
					if (characteristicInList.getUuid().toString().equalsIgnoreCase(characteristicUUID) ){
						return characteristicInList;
					}
				}											
			}
		}
		if(Constant.DEBUG)
			Log.i("JMG", "Characterisctic not found. Service: " + serviceUUID + " Characterisctic: " + characteristicUUID);			
		return null;
	}
	
	
	public static void read(String serviceUUID, String characteristicUUID){
		BluetoothGattCharacteristic characteristic = findCharacteristic(serviceUUID, characteristicUUID);	
		if(characteristic!=null){
			mGatt.readCharacteristic(characteristic);
		} else {
			if(Constant.DEBUG) Log.i("JMG","Read Characteristic not found in device");
		}

	}
	
	public static void write(String serviceUUID, String characteristicUUID, String data){
		write(serviceUUID,characteristicUUID,data.getBytes());
	}
	
	public static void write(String serviceUUID, String characteristicUUID, byte[] data){
		BluetoothGattCharacteristic characteristic = findCharacteristic(serviceUUID, characteristicUUID);	
		if(characteristic!=null){
			characteristic.setValue(data);
			mGatt.writeCharacteristic(characteristic);
			if(Constant.DEBUG) Log.i("JMG","Write Characteristic " + characteristicUUID + " with value " + data);
		} else {
			if(Constant.DEBUG) Log.i("JMG","Write Characteristic not found in device");
		}
		
	}
	
	
	public class ServiceType {
		private BluetoothGattService mService;
		private List<BluetoothGattCharacteristic> mCharacteristics;
		
		ServiceType(BluetoothGattService service) {
			mService = service;
			mCharacteristics= new ArrayList <BluetoothGattCharacteristic> ();
		}		
		
		public BluetoothGattService getService() {return mService;}
		public List<BluetoothGattCharacteristic> getCharacteristics () {return mCharacteristics;}
	}
	
	
    public static String bytesToString(byte[] bytes){
  	  StringBuilder stringBuilder = new StringBuilder(
                  bytes.length);
          for (byte byteChar : bytes)
              stringBuilder.append(String.format("%02X ", byteChar));
          return stringBuilder.toString();
    }
    


	
	public class MyCallBack extends BluetoothGattCallback{

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			
			
			if(Constant.DEBUG){
				if (status!=0) {
					Log.i("JMG", "Error status received onConnectionStateChange: " + status + " - New state: " + newState);	
				} else {
					Log.i("JMG", "onConnectionStateChange received. status = " + status +
							" - State: " + newState);
				}
			}
			
			if ((status==133)||(status==257)) {
				if(Constant.DEBUG)
					Log.i("JMG", "Unrecoverable error 133 or 257. DEVICE_DISCONNECTED intent broadcast with full reset");	
					sendToast("Unrecoverable error 133 or 257. DEVICE_DISCONNECTED intent broadcast with full reset. TumakuBLEService");
				startBLEFunctions();
				nextState();
				return;
			}
							
			if (newState==BluetoothProfile.STATE_CONNECTED&&status==BluetoothGatt.GATT_SUCCESS){ //Connected
				    mGatt=gatt;
					if(Constant.DEBUG){
						Log.i("JMG", "New connected Device. DEVICE_CONNECTED intent broadcast");	
						sendToast("Searching for BLE Services after BLE connect");
					}
	        	    setTumakuBLEState(STATE_SEARCH_SERVICES);
	        	    nextState();		
					return;
			}
			
			if (newState==BluetoothProfile.STATE_DISCONNECTED&&status==BluetoothGatt.GATT_SUCCESS){ //Disconnected
				if(Constant.DEBUG) {
					Log.i("JMG", "Disconnected Device success");	
					sendToast("Device disconnected unexpectedly. Reconnecting.");
				}
       		    resetTumakuBLE();
        	    setTumakuBLEState(STATE_CONNECT);
        	    nextState();
        	    return;
			}
		
			if(Constant.DEBUG)
				Log.i("JMG", "Unknown values received onConnectionStateChange. Status: " + status + " - New state: " + newState);				
		}


		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			super.onReadRemoteRssi(gatt, rssi, status);
		}
		

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			if(Constant.DEBUG){
				if(status==0){
					Log.i("JMG", "Write success ,characteristic uuid=:"+characteristic.getUuid().toString());
				}else{
					Log.i("JMG", "Write fail ,characteristic uuid=:"+characteristic.getUuid().toString()+" status="+status);
				}
			}   
      	    
			return;

		}

		
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicRead(gatt, characteristic, status);
			if(Constant.DEBUG) {
				if(status==0){
					Log.i("JMG", "Read from:"+characteristic.getUuid().toString()+" value: "+ bytesToString(characteristic.getValue()));
				} else {
					Log.i("JMG", "Read fail ,characteristic uuid=:"+characteristic.getUuid().toString()+" status="+status);
				}					
			}

		    String readValue= bytesToString(characteristic.getValue());
		    byte [] readValueArray=characteristic.getValue();
    	    setTumakuBLEState(STATE_WRITE);
    	    nextState();

			if (mBroadcastFlag) {
				if(Constant.DEBUG) Log.i("JMG", "Broadcasting Read success for  "+characteristic.getUuid().toString());
				Intent intent = new Intent(NOTIFICATION);
				intent.putExtra(EXTRA_CHARACTERISTIC, characteristic.getUuid().toString());
				intent.putExtra(EXTRA_VALUE, readValue);
				intent.putExtra(EXTRA_VALUE_BYTE_ARRAY, readValueArray);
				mTumakuBLEService.sendBroadcast(intent);	
			}
		}
		

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);
			String characteristicUUID = characteristic.getUuid().toString();
			byte [] notificationValueByteArray=  characteristic.getValue();
	        String notificationValue= bytesToString(notificationValueByteArray);
	        
			if(Constant.DEBUG){
				Log.i("JMG", "NOTIFICATION onCharacteristicChanged for characteristic " + characteristicUUID + 
						" value: " + notificationValue);
			}
			
			if (mBroadcastFlag) {			
				Intent intent = new Intent(NOTIFICATION);
				intent.putExtra(EXTRA_CHARACTERISTIC, characteristic.getUuid().toString());
				intent.putExtra(EXTRA_VALUE, bytesToString(characteristic.getValue()));
				intent.putExtra(EXTRA_VALUE_BYTE_ARRAY, characteristic.getValue());
				mTumakuBLEService.sendBroadcast(intent);	
			}
		}
		
		
		@Override
		public void  onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			if(Constant.DEBUG)
				Log.i("JMG", "onServicesDiscovered status: " + status);
			
			for(BluetoothGattService serviceInList: gatt.getServices()){
				String serviceUUID=serviceInList.getUuid().toString();
				ServiceType serviceType=new ServiceType(serviceInList);
				List <BluetoothGattCharacteristic> characteristics= serviceType.getCharacteristics();
				if(Constant.DEBUG)
					Log.i("JMG", "New service: " + serviceUUID);
				for(BluetoothGattCharacteristic characteristicInList : serviceInList.getCharacteristics()){
					if(Constant.DEBUG)
						Log.i("JMG", "New characteristic: " + characteristicInList.getUuid().toString());
					characteristics.add(characteristicInList);
				}
				mServices.add(serviceType);
			}
			setTumakuBLEState(STATE_READ);
			nextState();
		}
		

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
			if(Constant.DEBUG) Log.i("JMG", "onDescriptorWrite "+ descriptor.getUuid().toString() + " - characteristic: " + 
					descriptor.getCharacteristic().getUuid().toString() + " - Status: " + status);		
    	    
		}
	}

	
}
