package geodrop.geodrop;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class DropActivity extends AppCompatActivity {

    TextView label;
    String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drop);

        label = (TextView) findViewById(R.id.textView2);
    }

    public void sendDrop(View view) {

        final EditText EDIT =  (EditText) findViewById(R.id.editText);

        try
        {
            message = EDIT.getText().toString();

            if(message.length() >= 256)
            {
                label.setText("Message must be less then 256");
            }
            else
            {
                //GRANTS MAGIC VODOO HERE
            }
        }
        catch(java.lang.NullPointerException e)
        {
            label.setText("NullPointerException");
        }

    }
}
