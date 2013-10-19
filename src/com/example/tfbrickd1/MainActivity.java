/*
 * Tinkerforge Brickd für Android
 * Written by Andreas Krueger
 * 01.08.2013
 * eMail: androidBrickd@email.de
 */
package com.example.tfbrickd1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		((TextView) findViewById(R.id.tvListener)).setText("Listener (" + Listener.GetIPAddress() + ")");

        Brickd.Init(this, _masterBrickConnectedHandler, _connectionStatusChangedHandler, _receivedHandler, _sentHandler);
        
        
    }
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    }
    private Handler _connectionStatusChangedHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		if (msg.what == 0)
    			((TextView) findViewById(R.id.tvConnected)).setText("Disconnected :-(");
    		if (msg.what == 1)
    			((TextView) findViewById(R.id.tvConnected)).setText("Connected :-)");
    	}
    };
    private Handler _receivedHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		if (msg.obj instanceof String)
    			((TextView) findViewById(R.id.tvReceived)).setText((String) msg.obj);
    	}
    };
    private Handler _sentHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		if (msg.obj instanceof String)
    			((TextView) findViewById(R.id.tvSent)).setText((String) msg.obj);
    	}
    };
    private Handler _masterBrickConnectedHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		if (msg.what == 0)
    			((TextView) findViewById(R.id.tvFound)).setText("Not found :-(");
    		if (msg.what == 1)
    			((TextView) findViewById(R.id.tvFound)).setText("Tinkerforge Brick Found :-)");
    	}
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
    public void onReceive(Context context, Intent intent) 
    {
        _masterBrickConnectedHandler.sendMessage(_masterBrickConnectedHandler.obtainMessage(1));
    }
};
}
