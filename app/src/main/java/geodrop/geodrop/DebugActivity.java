package geodrop.geodrop;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;

public class DebugActivity extends AppCompatActivity implements LocationListener {

    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;

    protected TextView mLatitudeLabelText;
    protected TextView mLongitudeLabelText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLatitudeText = (TextView) findViewById((R.id.latitude_text));
        mLongitudeText = (TextView) findViewById((R.id.longitude_text));

        mLatitudeLabelText = (TextView) findViewById((R.id.latitude_label_text));
        mLongitudeLabelText = (TextView) findViewById((R.id.longitude_label_text));

        mLatitudeLabelText.setText(mLatitudeLabel);
        mLongitudeLabelText.setText(mLongitudeLabel);
    }


    @Override
    public void onLocationChanged(Location location)
    {
        MainActivity.mCurrentLocation = location;
        updateUI();
        Toast.makeText(this, R.string.location_updated, Toast.LENGTH_LONG).show();
    }

    private void updateUI()
    {
        mLatitudeText.setText(String.valueOf(MainActivity.mCurrentLocation.getLatitude()));
        mLongitudeText.setText(String.valueOf(MainActivity.mCurrentLocation.getLongitude()));
    }
}
