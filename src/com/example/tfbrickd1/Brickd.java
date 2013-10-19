/*
 * Tinkerforge Brickd für Android
 * Written by Andreas Krueger
 * 01.08.2013
 * eMail: androidBrickd@email.de
 */
package com.example.tfbrickd1;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.os.Message;
import android.app.PendingIntent;

public class Brickd implements Runnable {
	private Handler _masterBrickConnected;
	private Handler _dataReceived;
	private Handler _dataSent;
	private static Brickd _current;
	private Thread _thread;
	public static void Init(Activity activity, Handler mbch, Handler ipa, Handler dr, Handler ds)
	{
		_current = new Brickd(activity, mbch, ipa, dr, ds);
		_current._thread = new Thread(_current);
		_current._thread.start();
	}
	public static Brickd getCurrent()
	{
		return _current;
	}
    private static final String ACTION_USB_PERMISSION =
            "android.mtp.MtpClient.action.USB_PERMISSION";
	private PendingIntent _permissionIntent;
    private UsbManager _usbManager;
    private Listener _listener;
	private Brickd(Activity activity, Handler mbch, Handler csh, Handler dr, Handler ds)
	{
		_dataReceived = dr;
		_dataSent = ds;
		_masterBrickConnected = mbch;
		_listener = new Listener(4223, csh, this);

        _permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
		
		_usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        
        for (UsbDevice usbDevice : _usbManager.getDeviceList().values())
        {
        	OnNewDevice(usbDevice);
        }
        activity.registerReceiver(_usbReceiver, filter);

	}
	private boolean _permissionAccepted;
	private void OnNewDevice(UsbDevice usbDevice)
	{
		if (usbDevice.getDeviceClass() != 255)
			return;
		if (usbDevice.getVendorId() != 5840)
			return;
		if (usbDevice.getProductId() != 1597)
			return;

		_usbManager.requestPermission(usbDevice, _permissionIntent );
	}
	public final BroadcastReceiver _usbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent)
		{
            String action = intent.getAction();
            UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (usbDevice == null)
            	return;
            boolean b = false;
			synchronized (_lock) 
			{
				if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
				{
					OnNewDevice(usbDevice);
				}
				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
				{
					_masterBrickConnected.sendEmptyMessage(0);
					_usbDeviceConnection = null;
					_epIn = null;
					_epOut = null;
					
					b = true;
				}
				if (ACTION_USB_PERMISSION.equals(action))
				{
	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
	                {
	                	
	                    if(usbDevice != null){
	    					_usbDeviceConnection = _usbManager.openDevice(usbDevice);
	    					UsbInterface inf = usbDevice.getInterface(0);
	    					try
	    					{
	    						_usbDeviceConnection.claimInterface(inf, true);
		    					for(int q = 0; q < inf.getEndpointCount(); q++)
		    					{
		    						UsbEndpoint ep = inf.getEndpoint(q);
		    						if (ep.getEndpointNumber() == 4)
		    							_epIn = ep;
		    						if (ep.getEndpointNumber() == 5)
		    							_epOut = ep;
		    					}
		    					if ((_epIn == null) || (_epOut == null))
		    					{
		    						_epIn = null;
		    						_epOut = null;
		    						return;
		    					}
	    						_masterBrickConnected.sendEmptyMessage(1);

		    					_lock.notifyAll();
	    					}
	    					catch (Exception ex)
	    					{
	    					}
	                   }
	                    
	                   
	                } 

				}
			}

			
		}
	};
	private Object _lock = new Object();
	private UsbDeviceConnection _usbDeviceConnection;
	private UsbEndpoint _epIn;
	private UsbEndpoint _epOut;
	@Override
	public void run() {
		// TODO Auto-generated method stub
		int rr = 0;
		while(true)
		{
			try
			{
				UsbEndpoint epIn = null;
				UsbDeviceConnection udc = null;
				synchronized(_lock)
				{
					Message m = _dataSent.obtainMessage();
					m.obj = "Warte auf Masterbrick...";
					_dataSent.sendMessage(m);
					
					if ((_epIn == null) || (_usbDeviceConnection == null))
						_lock.wait();
					epIn = _epIn;
					udc = _usbDeviceConnection;
				}
				if ((epIn != null) && (udc != null))
				{
					
					while(true)
					{
						
						byte[] buffer = new byte[1024];
						int i = -1;
						synchronized(_lock)
						{
							udc = null;
							if (_usbDeviceConnection != null)
								udc = _usbDeviceConnection;
						}
						if (udc == null)
							break;
						i = udc.bulkTransfer(epIn, buffer, buffer.length, 0);
						if (i == -1) 
							break;
						byte[] ba = new byte[i];
						for(int q = 0; q < i; q++)
							ba[q] = buffer[q];

						Message m = _dataSent.obtainMessage();
						try {
							m.obj = new String(ba, "ASCII") + "(" + i + ", " + (rr++) + ")";
						} catch (Exception e) {
						}
						_dataSent.sendMessage(m);
						_listener.send(ba);
					}
					
				}
				udc = null;
				epIn = null;
				
			}
			catch (Exception ex)
			{
			}
		}
		
	}
	private int _i0 = 0;
	public void receiveData(byte[] ba)
	{
		synchronized(_lock)
		{
			if ((_usbDeviceConnection != null) && (_epOut != null))
			{
				_usbDeviceConnection.bulkTransfer(_epOut, ba, ba.length, 0);
				Message m = _dataReceived.obtainMessage();
				try {
					m.obj = new String(ba, "ASCII") + "(" + (_i0++) + ")";;
				} catch (UnsupportedEncodingException e) {
				}
				_dataReceived.sendMessage(m);
			}
			else
			{
				Message m = _dataReceived.obtainMessage();
				m.obj = "NULL";
				_dataReceived.sendMessage(m);
			}
		}
	}
}
