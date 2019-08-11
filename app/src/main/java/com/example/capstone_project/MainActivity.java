package com.example.capstone_project;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {
    public static final UUID MY_UUID = UUID.fromString("28c8e525-5c57-46fa-ba87-d4a7619c431c");

    private BluetoothAdapter BA;
    private Set<BluetoothDevice> pairedDevices;
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;
    public static final int NO_SOCKET_FOUND = 4;
    public BluetoothSocket socket;
    ListView lv;
    String valXY[];
    Double Yval;
    Double seconds;


    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);

            switch (msg_type.what) {
                case MESSAGE_READ:
                    byte[] readbuf = (byte[]) msg_type.obj;
                    String string_recieved = new String(readbuf);
                    String lines[] = string_recieved.split("\\r?\\n");
                    Toast.makeText(getApplicationContext(), "Data Received", Toast.LENGTH_SHORT).show();

                    //do some task based on recieved string

                    break;
                case MESSAGE_WRITE:

                    if (msg_type.obj != null) {
                        //ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg_type.obj);
                        //write method

                    }
                    break;

                case CONNECTED:
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    break;

                case CONNECTING:
                    Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                    break;

                case NO_SOCKET_FOUND:
                    Toast.makeText(getApplicationContext(), "No socket found", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BA = BluetoothAdapter.getDefaultAdapter();
        //lv = (ListView)findViewById(R.id.listView);

    }

    private double calculateAverage(List<Double> marks) {
        Double sum = 0.000;
        if(!marks.isEmpty()) {
            for (Double mark : marks) {
                sum += mark;
            }
            return sum.doubleValue() / marks.size();
        }
        return sum;
    }

    private List<Double> parseDataForHB(List<Double> ecgValues, double averageHeartBeatValue)
    {

        double previousVal = 0.000;
        List<Double> heartBeatValues = new ArrayList<>();
        Iterator it = ecgValues.iterator();

        while(it.hasNext())
        {
            double ecgVal = (Double) it.next();
            if(ecgVal >= (averageHeartBeatValue * .5))
            {
                if(((ecgVal > 0) && (previousVal < 0)) ||
                        ((ecgVal < 0) && (previousVal > 0)))
                {
                    heartBeatValues.add(ecgVal);
                    averageHeartBeatValue = (int) calculateAverage(heartBeatValues);
                }
            }
            previousVal = ecgVal;
        }
        return  heartBeatValues;
    }

    public void start_accepting_connection() {
        //call this on button click as suited by you

        final BluetoothServerSocket serverSocket;

        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = BA.listenUsingRfcommWithServiceRecord("NAME", MY_UUID);
        } catch (IOException e) {
        }
        serverSocket = tmp;
        socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                break;
            }

            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
//                    connectedThread.start();
                break;
            }
        }
//        acceptThread.start();
        Toast.makeText(getApplicationContext(), "accepting", Toast.LENGTH_SHORT).show();
    }

    public void plot_graph(View v) {
        final BluetoothSocket mmSocket;
        final InputStream mmInStream;
        final OutputStream mmOutStream;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        // Keep listening to the InputStream while connected

        // Read from the InputStream
        int bytesRead = -1;
        String message = "";
        while (message == "") {
            try {
                GraphView graph = (GraphView) findViewById(R.id.graph);
                final TextView textview = (TextView) findViewById(R.id.textview);
                if (mmInStream.available() > 0) {
                    bytesRead = mmInStream.read(buffer);
                    if (bytesRead > 0) {
                        while ((bytesRead == bufferSize) && (buffer[bufferSize - 1] != 0)) {
                            message = message + new String(buffer, 0, bytesRead);
                            bytesRead = mmInStream.read(buffer);
                        }
                        if ((buffer[bytesRead - 1] != 0)) {
                            message = message + new String(buffer, 0, bytesRead);
                        } else {
                            message = message + new String(buffer, 0, bytesRead - 1);
                        }
                        //mHandler.obtainMessage(MESSAGE_READ, message.getBytes().length, -1, message).sendToTarget();

                        String mline[] = message.split("\\r?\\n");
                        ArrayList<DataPoint> arrDataPoint = new ArrayList<>();
                        ArrayList<Double> data = new ArrayList<>();

                        for (int i = 0; i < mline.length - 1; i++) {
                            valXY = mline[i].split(",");
                            //       units = valXY[0].split(":"); //will break the string up into an array


                            //           minutes = Double.parseDouble(units[0].substring(1,2)); //first element
                            seconds = Double.parseDouble(valXY[0]); //second element
                            Yval = Double.parseDouble(valXY[1]);
                            data.add(Yval);
                            DataPoint dp = new DataPoint(seconds, Yval);
                            arrDataPoint.add(dp);
                        }
                        DataPoint[] listDp = new DataPoint[arrDataPoint.size()];
                        for (int i = 0; i < arrDataPoint.size(); i++) {
                            listDp[i] = arrDataPoint.get(i);
                        }

                        double sum = calculateAverage(data);
                        List<Double> data1 = parseDataForHB(data, sum);
                        int a = data1.size();
                        textview.setText("Heart Rate: " + a * 6 + " Bpm");

                        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(listDp);
                        graph.addSeries(series);
                    }
                    /*graph.getViewport().setXAxisBoundsManual(true);
                    graph.getViewport().setMinX(0);
                    graph.getViewport().setMaxX(1);

                    graph.getViewport().setScrollable(true); // enables horizontal scrolling
                    graph.getViewport().setScrollableY(true); // enables vertical scrolling*/

                }
            } catch (IOException e) {
            }
        }
    }

    public void on(View v){
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
            }
    }

    public void off(View v){
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }


    public  void visible(View v){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public  void accept_connection(View v){
        start_accepting_connection();
    }


//    public void list(View v){
//        pairedDevices = BA.getBondedDevices();
//
//        ArrayList list = new ArrayList();
//
//        for(BluetoothDevice bt : pairedDevices) list.add(bt.getName());
//        Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();
//
//        final ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
//
//        lv.setAdapter(adapter);
//    }

}
