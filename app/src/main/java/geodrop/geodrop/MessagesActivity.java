package geodrop.geodrop;


import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MessagesActivity extends Activity
{
    private static String[] messageArray = {};

    private static ListView messagesListView;
    private static ArrayAdapter arrayAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        messagesListView = (ListView) findViewById(R.id.months_list);

        // this-The current activity context.
        // Second param is the resource Id for list layout row item
        // Third param is input array
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, messageArray);
        messagesListView.setAdapter(arrayAdapter);
    }

    public static void updateMessages(String[] messages)
    {
        messageArray = messages;

        if(arrayAdapter != null)
        {
            arrayAdapter.notifyDataSetChanged();
        }
    }
}