package com.example.jeff.move4klant;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;

import library.Bluetoothscanner;
import library.DatabaseFunctions;
import library.DatabaseHandler;
import library.ServerRequestHandler;


public class home extends Activity {
    Button b4;
    Button b5;
    TextView ijzerhandelWinkelApp;
    private DatabaseFunctions db;
    private Bitmap[] output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // set the font to helvetica neue
        Typeface font = Typeface.createFromAsset(getAssets(), "HelveticaNeue.ttf");

        ijzerhandelWinkelApp = (TextView) findViewById(R.id.tvIJzerhandelWinkelApp);
        ijzerhandelWinkelApp.setTypeface(font);


        DatabaseHandler.getInstance(getApplicationContext()).updateAll();
        DatabaseHandler.getInstance(getApplicationContext()).loadImages();
        getOverflowMenu();
        startService(new Intent(getApplicationContext(), Bluetoothscanner.class));
    }

    public void onClickManageProfile(View v)
    {
        Intent intent = new Intent(this, ManageAccount.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_settings:
                Intent settings = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settings);
                overridePendingTransition(R.anim.left_slide_in, R.anim.left_slide_out);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        StopscanLeDevice();
        super.onBackPressed();
        finish();
    }

    private void StopscanLeDevice() {
        //stop scan
    }

    private void showAd(int id) {
        Intent i = new Intent(getApplicationContext(), OfferActivity.class);
        i.putExtra("offerID", id);
        startActivity(i);
    }
    private void showProduct(int id) {
        Intent i = new Intent(getApplicationContext(), ProductInfoActivity.class);
        i.putExtra("productID", id);
        startActivity(i);
    }
    private void checkStatus(int id) {
        if (DatabaseHandler.getInstance(getApplicationContext()).checkinstatus(1)){
            Toast.makeText(getApplicationContext(),"ja", Toast.LENGTH_SHORT).show();
        }
        else{Toast.makeText(getApplicationContext(),"nee", Toast.LENGTH_SHORT).show();}
    }


    private void getOverflowMenu() {

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
