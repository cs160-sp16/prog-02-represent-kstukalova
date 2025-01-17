package com.cs160.joleary.catnip;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by joleary and noon on 2/19/16 at very late in the night. (early in the morning?)
 */
public class WatchToPhoneService extends Service implements GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "WatchToPhoneService";

    private GoogleApiClient mWatchApiClient;
    //private List<Node> nodes = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        //initialize the googleAPIClient for message passing
        mWatchApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks(this)
                .build();
        //and actually connect it
        mWatchApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        mWatchApiClient.disconnect();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override //alternate method to connecting: no longer create this in a new thread, but as a callback
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected");
        Wearable.NodeApi.getConnectedNodes(mWatchApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        //nodes = getConnectedNodesResult.getNodes();
                        Log.d("T", "found nodes");
                        //when we find a connected node, we populate the list declared above
                        //finally, we can send a message
                        //sendMessage("/send_toast", "Good job!");
                        Log.i(TAG, "onConnected/onResult");
                    }
                });
    }

    private String createWearData(String name, String id, String picture_url, String party,
                                  String term) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("name", name);
        data.put("id", id);
        data.put("picture_url", picture_url);
        data.put("party", party);
        data.put("term", term);

        return data.toString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Which cat do we want to feed? Grab this info from INTENT
        // which was passed over when we called startService
        Bundle extras = intent.getExtras();
        final String name = extras.getString("name");
        final String id = extras.getString("id");
        final String picture_id = extras.getString("picture_url");
        final String party = extras.getString("party");
        final String term = extras.getString("term");
        final Boolean random = extras.getBoolean("random");
        Log.i(TAG, "onStartCommand " + name);

        // Send the message with the cat name
        new Thread(new Runnable() {
            @Override
            public void run() {
                //first, connect to the apiclient
                //now that you're connected, send a massage with the cat name
                Log.e("naem: ", name + "WHAT");
                if (!random && name.isEmpty()) {
                    return; // if user clicks on screen when nothing is htere
                }
                else if (random) {
                    try {
                        JSONArray all_zipcodes = new JSONArray(loadJSONFromAsset());
                        // Log.e("all counties: ", all_zipcodes.toString());
                        Log.e("number of zipcodes: ", all_zipcodes.length() + "");
                        Random rn = new Random();
                        int random_index = rn.nextInt(all_zipcodes.length());
                        String zip = all_zipcodes.getString(random_index);
                        sendMessage("/random", zip);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                else {
                    try {
                        Log.e("watch to phone: ", picture_id);
                        String wearData = createWearData(name, id, picture_id, party, term);
                        sendMessage("/name", wearData);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();

        return START_STICKY;
    }


    @Override //we need this to implement GoogleApiClient.ConnectionsCallback
    public void onConnectionSuspended(int i) {}

    private void sendMessage(final String path, final String text ) {
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mWatchApiClient ).await();
        for(Node node : nodes.getNodes()) {
            Wearable.MessageApi.sendMessage(
                    mWatchApiClient, node.getId(), path, text.getBytes());
        }
        Log.i(TAG, "sendMessage " + path  + " " + text + " done");
    }
    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("all_zips.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}