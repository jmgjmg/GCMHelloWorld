GCMHelloWorld
=============

Android project to remotely control Yeelight light bulb over Bluetooth Low Energy using Google Cloud Messsaging(GCM)

It includes two activities:
- MainActivity: used to retrieve a registrationId from Google Cloud Messaging server for the corresponding Google API project.  Must be run once before the app can start using GCM. It also includes a button to generate a dummy push notification that is then received by the app through GCM. The registration Id number presented on screen must be used by the server application together with its API key to send messages to the user's mobile/app 

- ControlLightActivity: used to define the light configuration (color and intensity) and to generate push notifications accordingly. 
This activity is also used to start or stop the Bluetooth Low Energy (BLE) service that manages the connection from the smartphone/tablet 
to the light bulb. The BLE service must be started before any push messages can be sent to the light bulb.

For security resasons, GCM key values are stored in a file in the src directory (Constant.java). The original file
is removed from the github repository, however a dummy Constant.java.txt file is available. Rename it to Constant.java and update its content with the right values obtained when creating the application in the Google Developers Console.

The MAC value of the Yeelight bulb must be updated in file Strings.XML (deafultAddress).

The configuration of the light bulb can be updated by sending a JSON message over http POST to Google GCM server.

Example (replace values of API-key XXXXXXXXXXXXXX and registration-id YYYYYYYYY with your own values

```
POST https://android.googleapis.com/gcm/send HTTP/1.1
Authorization: key=XXXXXXXXXXXXXX
Content-Type: application/json
Host: android.googleapis.com
Content-Length: 272

{"data":{"Intensity":100,"Color":{"Blue":0,"Green":0,"Red":255}},"registration_ids":["YYYYYYYYY"]}

```

Intensity value is an integer between 0 and 100.

Red, Green and Blue values are integers between 0 and 255.

For more information about the format of this message, please read Google's documentation: 
https://developer.android.com/google/gcm/http.html
