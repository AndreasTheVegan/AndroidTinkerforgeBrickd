/*
 * Tinkerforge Brickd für Android
 * Written by Andreas Krueger
 * 01.08.2013
 * eMail: androidBrickd@email.de
 */
package com.example.tfbrickd1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Enumeration;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class Listener implements Runnable
{
	private int _port;
	private Handler _connectionChanged;
	private Brickd _brickd;
	public Listener(int port, Handler connectionChanged, Brickd bd )
	{
		_brickd = bd;
		_port = port;
		_connectionChanged = connectionChanged;
		Thread t = new Thread(this);
		_inputStream = null;
		_outputStream = null;
		t.start();
	}
	private ServerSocket _serverSocket;
	private BufferedReader _bufferedReader;
	private BufferedWriter _bufferedWriter;
	@Override
	public void run() {
		try {
			_serverSocket = new ServerSocket(_port);
			while(true)
			{
				_connectionChanged.sendEmptyMessage(0);
				Socket socket = _serverSocket.accept();
				_connectionChanged.sendEmptyMessage(1);
				try
				{
					InputStream is = null;
					OutputStream os = null;
					synchronized(this)
					{
						_inputStream = is = socket.getInputStream();
						_outputStream = os = socket.getOutputStream();
					}
					byte[] buffer = new byte[1024];
					int i = 0;
					while(( i = is.read(buffer)) != -1)
					{
						byte[] ba2 = new byte[i];
						for(int q = 0; q < i; q++)
							ba2[q] = buffer[q];
						_brickd.receiveData(ba2);
					}
					synchronized(this)
					{
						_inputStream = null;
						_outputStream = null;
					}
					socket.close();
				}
				catch (Exception ex)
				{
					
				}
			}
			
		} catch (Exception e) {
		}
	}
	private InputStream _inputStream;
	private OutputStream _outputStream;
	public void send(byte[] buffer)
	{
		synchronized(this)
		{
			try
			{
				if (_outputStream != null)
				{
					_outputStream.write(buffer);
					_outputStream.flush();
				}
			}
			catch (Exception ex)
			{
			}
		}
	}
	public static String GetIPAddress()
	{
		String s = "";
		try
		{
			Enumeration<NetworkInterface> netInter = NetworkInterface.getNetworkInterfaces();
			int n = 0;
	
			while ( netInter.hasMoreElements() )
			{
			  NetworkInterface ni = netInter.nextElement();
			  for ( InetAddress iaddress : Collections.list(ni.getInetAddresses()) )
			  {
				  if (!iaddress.isLoopbackAddress() && (iaddress.getHostAddress().length() < 16))
					  s+= iaddress.getHostAddress() + " ";
			  }
			}
		}
		catch (Exception ex)
		{
		}
		return s;
	}
}
