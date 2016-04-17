package geodrop.geodrop;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DropActivity extends AppCompatActivity {

    String IP;
    private static URL url;

    TextView label;
    TextView label2;
    String message;

    double latitude;
    double longitude;

    EditText EDIT;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drop);

        label = (TextView) findViewById(R.id.textView2);
        label2 = (TextView) findViewById(R.id.textView3);

        IP = getResources().getString(R.string.IP) + "adddrop?";

        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);

        EDIT = (EditText) findViewById(R.id.editText);
    }

    public void sendDrop(View view) {

        label.setText("");
        label2.setText("");

        try
        {
            message = EDIT.getText().toString();

            if(message.length() >= 256)
            {
                label.setText("Message must be less then 256");
            }
            else
            {
                new ServerTask().execute(latitude, longitude);
            }
        }
        catch(java.lang.NullPointerException e)
        {
            label.setText("NullPointerException");
        }

    }

    // This AsyncTask takes in a latitude and longitude and sends them to the server
    // If there is a nearby drop, the server returns true
    private class ServerTask extends AsyncTask<Double, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(Double... params)
        {
                String urlString = "uninitialized";
                try
                {
                    urlString = IP + "latitude=" + params[0] + "&longitude=" + params[1] + "&message=" +
                            message.replace(' ', '_').replace('\n',';');

                    url = new URL(urlString);
                }
                catch (MalformedURLException e)
                {
                    Log.i("Active Location Service", "Invalid URL!");
                }

                URLConnection connection = null;
                try
                {
                    connection = url.openConnection();
                    connection.setConnectTimeout(5000);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    {
                        String currentLine = null;
                        while ((currentLine = reader.readLine()) != null)
                        {
                            if (currentLine.equals("Success"))
                            {
                                return true;
                            }
                            else
                            {
                                return false;
                            }
                        }
                    }

                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                Log.i("Active Location Service", "Sending data to server!");
                return false;
        }

        @Override
        protected void onPostExecute(Boolean wasSuccessful)
        {
            super.onPostExecute(wasSuccessful);

            // If there is a nearby drop, show a notification
            if (wasSuccessful)
            {
                EDIT.setText("");
                label2.setText("Message Sent");
            }
            else
            {
                label.setText("Message did not Drop");
            }
        }
    }
}
