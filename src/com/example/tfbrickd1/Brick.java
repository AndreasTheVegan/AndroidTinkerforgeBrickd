/*
 * Tinkerforge Brickd für Android
 * Written by Andreas Krueger
 * 01.08.2013
 * eMail: androidBrickd@email.de
 */
package com.example.tfbrickd1;

import android.hardware.usb.UsbDeviceConnection;
import android.os.AsyncTask;

public class Brick extends AsyncTask<String, Void, String> {
	private UsbDeviceConnection _conn;
	public Brick(UsbDeviceConnection conn)
	{
		_conn = conn;
		
	}
	@Override
	protected String doInBackground(String... urls)
	{
		return null;
		
	}
}
