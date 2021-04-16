package com.kishore.androidqbeta4;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";
    List<WifiNetworkSuggestion> suggestionList = null;
    WifiManager wifiManager = null;
    private EditText mSSIDEditText;
    private EditText mPasswordEditText;
    private Button mConnectButton;
    private Button mRemoveButton;
    private CheckBox isHiddenSSIDcheckBox;
    private String ssidName = "";
    private String password = "";
    SpannableStringBuilder spannableString = null;
    private BroadcastReceiver       wifiNetworkSuggestionReceiver = null;
    int                             wifiControl;
    private ProgressDialog mProgressDialog;
    String message = "";
    Boolean isHiddenSSIDChecked = false;

    @TargetApi(Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                100);

        mSSIDEditText = (EditText) findViewById(R.id.editText);
        mPasswordEditText = (EditText) findViewById(R.id.editText2);
        mConnectButton = (Button) findViewById(R.id.button);
        mRemoveButton = (Button) findViewById(R.id.remove);
        isHiddenSSIDcheckBox = (CheckBox) findViewById(R.id.isHiddenSSIDcheckBox);

        mConnectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ssidName = mSSIDEditText.getText().toString();
                password = mPasswordEditText.getText().toString();
                isHiddenSSIDChecked = isHiddenSSIDcheckBox.isChecked();
                buildSuggestion();

            }
        });

        mRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCredentials();
            }
        });

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        AppOpsManager appOpsManager = (AppOpsManager) getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
        wifiControl = appOpsManager.unsafeCheckOp("android:change_wifi_state",this.getApplicationInfo().uid,this.getPackageName());

        registerPostConnectionReceiver();
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private void buildSuggestion(){
        //      Configuring wifi using WifiNetworkSuggestion
        final WifiNetworkSuggestion suggestion =
                new WifiNetworkSuggestion.Builder()
                        .setSsid(ssidName)
                        .setWpa2Passphrase(password)
                        .setIsAppInteractionRequired(true)
                        .setPriority(1000)
                        .setIsHiddenSsid(isHiddenSSIDChecked)
                        .build();

        suggestionList = new ArrayList<WifiNetworkSuggestion>();
        suggestionList.add(suggestion);

        message = "\nConnecting. . .\n\n\nDo follow the steps:\n\n1. Click Proceed to go to Wifi Settings.\n\n2. Forget your current wifi connection.\n\n3. Then return back";
        showProgressDialog(message,"Proceed");

    }

    @TargetApi(Build.VERSION_CODES.Q)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity", "onActivityResult()::" + requestCode + "::" + resultCode);

        if(requestCode == 500){

            if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q){
                message = "\nConnecting. . .\n\n\nYou will get notification as \n'Connect to Wi-Fi networks?'\n\nClick 'yes' on it, to get Wi-Fi";
                showProgressDialog(message, null);
            }
            configureNetworkSuggestion();
        }
        else if(requestCode == 501){

        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private void configureNetworkSuggestion(){
        //------------------------WifiSuggestionApi-----------------------------

        wifiManager.removeNetworkSuggestions(suggestionList);
        final int status = wifiManager.addNetworkSuggestions(suggestionList);

        if(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS){
            Log.i("Main","success");
        }else if(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE){
            Log.i("Main","Duplicate");
        }else if(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP){
            Log.i("Main","Exceeds max per app");
        }else if(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED){
            Log.i("Main","Disallowed");
        }else if(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL){
            Log.i("Main","Internal");
        }else if(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID){
            Log.i("Main","Remove Invalid");
        }else {
            Log.i("Main","Not Success");
        }
        wifiManager.getScanResults();
    }

    private void registerPostConnectionReceiver() {

        IntentFilter intent = new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

        wifiNetworkSuggestionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // Broadcast will receive if successfully connected to suggested ssid
                if (intent != null && intent.getAction().equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {

                    Log.i("Main","Device connected successfully");
                    String mConnectedSSIDName = wifiManager.getConnectionInfo().getSSID();
                    _dismissProgressDialog();

                    Toast.makeText(MainActivity.this, "Connected to "+mConnectedSSIDName, Toast.LENGTH_SHORT).show();
                }
            }
        };

        this.getApplicationContext().registerReceiver(wifiNetworkSuggestionReceiver,intent);

    }

    private void showProgressDialog(String message, String buttonText){
        mProgressDialog = new ProgressDialog(this);

        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage(message);

        if(buttonText != null){
            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE, buttonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS),500);
                }
            });
        }
        mProgressDialog.show();
    }

    private void _dismissProgressDialog(){
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private void removeCredentials(){

        List<WifiNetworkSuggestion> removeSuggestionList = new ArrayList<WifiNetworkSuggestion>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            List<WifiNetworkSuggestion> oldConfiguredSuggestions = wifiManager.getNetworkSuggestions();

            String suggestionName = ssidName;
            if(oldConfiguredSuggestions.size() > 0){
                for (WifiNetworkSuggestion oldConfiguredSuggestion : oldConfiguredSuggestions){

                    if(oldConfiguredSuggestion.getSsid() != null){
                        if(suggestionName.equals(oldConfiguredSuggestion.getSsid())){
                            Log.d("TestingPOC","Old SSID Suggestion : "+oldConfiguredSuggestion.getSsid());
                            removeSuggestionList.add(oldConfiguredSuggestion);
                            break;
                        }
                    }
                }
            }
        }
        else{
            final WifiNetworkSuggestion removeSuggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssidName)
                            .build();

            removeSuggestionList.add(removeSuggestion);
        }

        if(removeSuggestionList.size() != 0){
            wifiManager.removeNetworkSuggestions(removeSuggestionList);
        }
    }
}

/*
        // Dialog box for User guidance message
        android.app.AlertDialog.Builder alertBuilder = new android.app.AlertDialog.Builder(this);

        alertBuilder.setCancelable(false);
        alertBuilder.setMessage(message);
        alertBuilder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
                MainActivity.this.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS),500);

//                Bundle bundle = new Bundle();
//                bundle.putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST,(ArrayList<? extends
//                        Parcelable>) suggestionList);
//                final Intent intent = new Intent(Settings.ACTION_WIFI_ADD_NETWORKS);
//                intent.putExtras(bundle);
//                startActivityForResult(intent, 501);

            }
        });

        AlertDialog alert = alertBuilder.create();
        alert.setTitle("Attention Please");
        alert.show();
*/

//------------------------To Check the spl app access for wifi-control-----------------------------
//    AppOpsManager appOpsManager = (AppOpsManager) getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
//        wifiControl = appOpsManager.unsafeCheckOp("android:change_wifi_state",this.getApplicationInfo().uid,this.getPackageName());


//------------------------Adding SSID like user saved network-----------------------------

//    message = "By clicking the proceed, you will be prompted for save network request. Click 'Save' then.";

//        suggestionList = null;
//        suggestionList = wifiManager.getNetworkSuggestions();
//        Bundle bundle = new Bundle();
//        bundle.putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST,
//                (ArrayList<? extends Parcelable>) suggestionList);
//
//        final Intent intent = new Intent(Settings.ACTION_WIFI_ADD_NETWORKS);
//        intent.putExtras(bundle);
//
//        startActivityForResult(intent, 0);


//------------------------For click "here" as clickable text in popup-----------------------------
//        spannableString = new BuildClickableMessage().buildClickableMessageUsingRegEx(
//                "\\$\\{wifiManagerLink\\}(\\s*.+?\\s*)\\$\\{/wifiManagerLink\\}",
//                "Do follow the steps:\n\n1. Forget your current wifi connection\n\n2. Click ${wifiManagerLink}here${/wifiManagerLink} to go to Wifi Settings.\n\n3. Click Yes in wifi connection notification",
//                "here",
//                this,
//                new Intent(Settings.ACTION_WIFI_SETTINGS),
//                true,
//                R.color.blue);

//        final TextView message = new TextView(this.getApplicationContext());
//        message.setMovementMethod(LinkMovementMethod.getInstance());
//        message.setText(spannableString);

//        alertBuilder.setView(message);

//------------------------Location Permission-----------------------------

//        ActivityCompat.requestPermissions(MainActivity.this,
//                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                100);
