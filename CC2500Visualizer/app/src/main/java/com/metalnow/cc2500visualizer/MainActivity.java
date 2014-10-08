package com.metalnow.cc2500visualizer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;

import tw.com.prolific.driver.pl2303.PL2303Driver;
import tw.com.prolific.driver.pl2303.PL2303Driver.DataBits;
import tw.com.prolific.driver.pl2303.PL2303Driver.FlowControl;
import tw.com.prolific.driver.pl2303.PL2303Driver.Parity;
import tw.com.prolific.driver.pl2303.PL2303Driver.StopBits;
import android.hardware.usb.UsbManager;
import android.content.Context;
import org.achartengine.GraphicalView;

import java.io.IOException;


public class MainActivity extends Activity {

    String TAG = "CC2500Visualizer_APLog";
    private static final boolean SHOW_DEBUG = false;

    GraphSpectrum graphSpectrum;
    FrameLayout frameLayout;

    PL2303Driver mSerial;
    private PL2303Driver.BaudRate mBaudrate = PL2303Driver.BaudRate.B9600;
    private PL2303Driver.DataBits mDataBits = PL2303Driver.DataBits.D8;
    private PL2303Driver.Parity mParity = PL2303Driver.Parity.NONE;
    private PL2303Driver.StopBits mStopBits = PL2303Driver.StopBits.S1;
    private PL2303Driver.FlowControl mFlowControl = PL2303Driver.FlowControl.OFF;

    private static final String ACTION_USB_PERMISSION = "com.metalnow.cc2500visualizer.USB_PERMISSION";

    public Spinner PL2303HXD_BaudRate_spinner;
    public int PL2303HXD_BaudRate;

    private static Thread thread;
    private static GraphicalView graphicalView;
    private static boolean gettingData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameLayout = (FrameLayout)findViewById(R.id.SpectrumLayout);
        graphSpectrum = new GraphSpectrum();
        graphicalView = graphSpectrum.getView(this);
        frameLayout.addView(graphicalView);


        PL2303HXD_BaudRate_spinner = (Spinner)findViewById(R.id.spnBaudRate);

        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(this, R.array.BaudRate_Var, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        PL2303HXD_BaudRate_spinner.setAdapter(adapter);
        PL2303HXD_BaudRate_spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

        Button mButton01 = (Button)findViewById(R.id.btnOpen);
        mButton01.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                openUsbSerial();
            }
        });

        // get service
        mSerial = new PL2303Driver((UsbManager) getSystemService(Context.USB_SERVICE),
                this, ACTION_USB_PERMISSION);
        // check USB host function.
        if (!mSerial.PL2303USBFeatureSupported())
        {
            Toast.makeText(this, "No Support USB host API", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No Support USB host API");
            mSerial = null;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Enter onDestroy");
        if(mSerial!=null) {
            mSerial.end();
            mSerial = null;
        }
        super.onDestroy();
        Log.d(TAG, "Leave onDestroy");
    }

    public void onResume() {
        Log.d(TAG, "Enter onResume");
        super.onResume();
        String action =  getIntent().getAction();
        Log.d(TAG, "onResume:"+action);

        //if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
        if(!mSerial.isConnected()) {
            if (SHOW_DEBUG) {
                Log.d(TAG, "New instance : " + mSerial);
            }

            if( !mSerial.enumerate() ) {

                Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();
                return;
            } else {
                Log.d(TAG, "onResume:enumerate succeeded!");
            }
        }//if isConnected
        Toast.makeText(this, "attached", Toast.LENGTH_SHORT).show();

        Log.d(TAG, "Leave onResume");
    }


    /*cc2500 data parsing*/
    int rssi_offset = 72;  // CC2500's RSSI baseline offset in dBm.
    int nPoints = 256;
    int[] datapoints = new int[nPoints];
    int[] maxes = new int[nPoints];
    int[][] averages = new int[nPoints][2];

    int chan=0;
    int primed=0;
    int rssi;

    double freqMin = 2.400; // base frequency (ch. 0) in GHz
    double freqStep = 0.000405; // channel spacing
    double axisStep = .005; // axis display spacing

    double freqMax = freqMin + (256*freqStep);  //2.50368;

    private int ss(int unsigned)
    {
        if (unsigned < 128)
        {
            unsigned = (256 - unsigned);
        }
        return unsigned; // now signed...
    }

    private void renderSpectrum()
    {
        int len;
        byte[] rbuf = new byte[4096];

        Log.d(TAG, "Enter readDataFromSerial");

        if(null==mSerial)
            return;

        if(!mSerial.isConnected())
            return;

        len = mSerial.read(rbuf);
        if(len<0) {
            Log.d(TAG, "Fail to bulkTransfer(read data)");
            return;
        }

        if (len > 0) {
            if (SHOW_DEBUG) {
                Log.d(TAG, "read len : " + len);
            }
            for (int j = 0; j < len; j++) {

                rssi = rbuf[j];
                if ((rssi & 0x01) == 0x01) // '1' in lowest bit indicates start of frame (0th channel data)
                {
                    chan=0;
                    primed = 1;
                }
                else
                {
                    chan++;
                }

                if ( chan >= nPoints )
                    continue;

                //int org_rssi = rssi;

                // convert rssi byte to real output in dBm. After killing off LSB, output is in signed (dBm*2 + offset).
                rssi = rssi & 0xfe;
                rssi = ss(rssi);
                //Log.d(TAG, "chan = " + chan + " rssi = " + rssi + " (" +  Integer.toHexString(org_rssi) + ")");
                rssi = rssi/2 - rssi_offset;

                if (primed != 0)
                {
                    {
                        datapoints[chan] = rssi;
                        averages[chan][0] = averages[chan][0] + rssi;
                        averages[chan][1] = averages[chan][1] + 1;
                        if (rssi > maxes[chan])
                        {
                            maxes[chan] = rssi;
                        }
                    }
                }

                graphSpectrum.updateCurrentValue(freqMin + freqStep * chan, datapoints[chan] ); // Add it to our graph
                graphicalView.repaint();
            }
        }
        else {
            if (SHOW_DEBUG) {
                Log.d(TAG, "read len : 0 ");
            }
            return;
        }
/*
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
*/
    }

    private void openUsbSerial() {
        Log.d(TAG, "Enter  openUsbSerial");
        if(null==mSerial)
            return;

        if (mSerial.isConnected()) {
            if (SHOW_DEBUG) {
                Log.d(TAG, "openUsbSerial : isConnected ");
            }
            String str = PL2303HXD_BaudRate_spinner.getSelectedItem().toString();
            int baudRate= Integer.parseInt(str);
            switch (baudRate) {
                case 9600:
                    mBaudrate = PL2303Driver.BaudRate.B9600;
                    break;
                case 19200:
                    mBaudrate =PL2303Driver.BaudRate.B19200;
                    break;
                case 115200:
                    mBaudrate =PL2303Driver.BaudRate.B115200;
                    break;
                default:
                    mBaudrate =PL2303Driver.BaudRate.B9600;
                    break;
            }
            Log.d(TAG, "baudRate:"+baudRate);
            // if (!mSerial.InitByBaudRate(mBaudrate)) {
            if (!mSerial.InitByBaudRate(mBaudrate,700)) {
                if(!mSerial.PL2303Device_IsHasPermission()) {
                    Toast.makeText(this, "cannot open, maybe no permission", Toast.LENGTH_SHORT).show();
                }

                if(mSerial.PL2303Device_IsHasPermission() && (!mSerial.PL2303Device_IsSupportChip())) {
                    Toast.makeText(this, "cannot open, maybe this chip has no support, please use PL2303HXD / RA / EA chip.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();

                thread = new Thread() {
                    public void run()
                    {
                        gettingData = true;
                        while (gettingData)
                        {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            renderSpectrum();
                            graphicalView.repaint();
                        }
                    }
                };
                thread.start();


            }
        }//isConnected

        Log.d(TAG, "Leave openUsbSerial");
    }//openUsbSerial

    public class MyOnItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            if(null==mSerial)
                return;

            if(!mSerial.isConnected())
                return;

            int baudRate=0;
            String newBaudRate;
            Toast.makeText(parent.getContext(), "newBaudRate is-" + parent.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
            newBaudRate= parent.getItemAtPosition(position).toString();

            try	{
                baudRate= Integer.parseInt(newBaudRate);
            }
            catch (NumberFormatException e)	{
                System.out.println(" parse int error!!  " + e);
            }

            switch (baudRate) {
                case 9600:
                    mBaudrate =PL2303Driver.BaudRate.B9600;
                    break;
                case 19200:
                    mBaudrate =PL2303Driver.BaudRate.B19200;
                    break;
                case 115200:
                    mBaudrate =PL2303Driver.BaudRate.B115200;
                    break;
                default:
                    mBaudrate =PL2303Driver.BaudRate.B9600;
                    break;
            }

            int res = 0;
            try {
                res = mSerial.setup(mBaudrate, mDataBits, mStopBits, mParity, mFlowControl);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if( res<0 ) {
                Log.d(TAG, "fail to setup");
                return;
            }
        }
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }//MyOnItemSelectedListener
}
