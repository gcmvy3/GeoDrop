package geodrop.geodrop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class SearchActivity extends AppCompatActivity implements SensorEventListener
{
    ActiveLocationService activeLocationService;

    String IP;

    URL url;

    // Tracks whether the location service is bound
    private boolean bound = false;

    private Location location;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private float[] r = new float[9];
    private float[] orientation = new float[3];
    private float currentDegree = 0f;
    private Queue<Float> floatQueue = new LinkedBlockingQueue<>();

    private double closestLong;
    private double closestLat;

    private double degree2 = 0;

    TextView tvHeading;

    ImageView arrowV;

    // This thread updates the location every few seconds
    Thread locationUpdater;

    ArrayList<Drop> dropsList = new ArrayList<Drop>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        IP = getResources().getString(R.string.IP) + "getdrops?";

        locationUpdater = new Thread()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted())
                {
                    if (bound)
                    {
                        location = activeLocationService.getLocation();

                        // Ask the server for nearby drops
                        new ServerTask().execute(location.getLatitude(), location.getLongitude());
                    }

                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch(Exception e)
                    {
                    }
                }
            }
        };

        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) findViewById(R.id.tvHeading);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        arrowV = (ImageView)findViewById(R.id.arrowView);
        arrowV.setImageResource(R.drawable.arrow);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Launch a location service and bind to it
        Intent intent = new Intent(this, ActiveLocationService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        locationUpdater.start();
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        // Unbind from the location service
        if (bound)
        {
            unbindService(connection);
            bound = false;
        }

        locationUpdater.interrupt();
    }

    protected void onResume()
    {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause()
    {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
    }

    // Used to communicate with the ActiveLocationService
    private ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ActiveLocationService.LocalBinder binder = (ActiveLocationService.LocalBinder) service;
            activeLocationService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            bound = false;
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor == accelerometer)
        {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
        } else if (event.sensor == magnetometer)
        {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }
        if (lastAccelerometerSet && lastMagnetometerSet)
        {
            SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(r, orientation);
            float azimuthInRadians = orientation[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

            currentDegree = -azimuthInDegress;
        }
        
        floatQueue.add(currentDegree);

        float total = 0;
		for (Float f : floatQueue)
			total += f;
		total /= floatQueue.size();

        if (floatQueue.size() > 20)
            floatQueue.remove();

        if(closestLat != 0)
        {
            degree2 = Math.cosh((location.getLatitude() * closestLong) + (location.getLongitude() * closestLat))/
                    (Math.sqrt(location.getLongitude() * location.getLongitude() + closestLong * closestLong)+
                            Math.sqrt(location.getLatitude() * location.getLatitude() + closestLat * closestLat));
        }
        else
        {
            degree2 = 0;
        }
        arrowV.setRotation(total + (float) degree2);
        tvHeading.setText("Heading: " + Float.toString(total) + " degrees");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    // This AsyncTask takes in a latitude and longitude and sends them to the server
    // The server returns a list of nearby drops
    private class ServerTask extends AsyncTask<Double, Void, Drop[]>
    {
        @Override
        protected Drop[] doInBackground(Double... params) {
            Drop[] drops = new Drop[0];

            String urlString = "uninitialized";
            try {
                urlString = IP + "latitude=" + params[0] + "&longitude=" + params[1];

                url = new URL(urlString);
            } catch (MalformedURLException e) {
                Log.i("Active Location Service", "Invalid URL!");
            }

            URLConnection connection = null;
            try {
                connection = url.openConnection();
                connection.setConnectTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                {
                    String currentLine = null;
                    while ((currentLine = reader.readLine()) != null) {
                        //Reads in a huge string from the server and parses drops from it
                        //Format of data from the server:

                        // success
                        // num drops
                        // for : drop:
                        //     lat
                        //     lon
                        //     numLines of message
                        //     message
                        //     */

                        System.out.println("CurrentLine: " + currentLine);

                        if (currentLine.equals("Success")) {
                            System.out.println("Success!");
                            int numDrops = Integer.parseInt(reader.readLine());

                            drops = new Drop[numDrops];

                            double lat;
                            double lon;
                            int numLines;
                            String message;

                            for (int i = 0; i < numDrops; i++) {
                                lat = Double.parseDouble(reader.readLine());
                                lon = Double.parseDouble(reader.readLine());

                                numLines = Integer.parseInt(reader.readLine());
                                message = "";

                                for (int j = 0; j < numLines; j++) {
                                    message += reader.readLine();
                                }

                                Drop drop = new Drop(lat, lon, message);
                                drops[i] = drop;
                            }
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.i("Active Location Service", "Pulling drops from server!");

            return drops;
        }

        @Override
        protected void onPostExecute(Drop[] drops) {
            System.out.println("Succesfully pulled drops: " + drops.length);

            dropsList = new ArrayList<Drop>();

            for (Drop d : drops) {
                dropsList.add(d);
            }

            String[] messages = new String[drops.length];
            for (int i = 0; i < messages.length; i++) {
                messages[i] = drops[i].message;
            }

            if(drops.length > 0 && drops[0] != null)
            {
                closestLat = drops[0].latitude;
                closestLong = drops[0].longitude;
            }
            else
            {
                closestLat = 1;
                closestLong = 1;
            }

            MessagesActivity.updateMessages(messages);

            super.onPostExecute(drops);
        }
    }
}
