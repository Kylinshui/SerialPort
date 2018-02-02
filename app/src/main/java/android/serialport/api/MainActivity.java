package android.serialport.api;

import android.app.Dialog;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {
    private Handler handler;
    private readThread read_thread;
    private static final int readLength = 512;
    byte[] readData;
    char[] readDataToText;

    private Button btSelect;
    private Button btSend;
    private Button btClean;
    private EditText edSend;
    private EditText edRecv;
    private TextView tvSend;
    private TextView tvRecv;
    private SerialPort   mSerialPort;
    private OutputStream mOutputStream;
    private InputStream  mInputStream;
    private int sendlen;
    private int recvlen;
    private String port;
    private int baudrate;
    private int iavailable = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btSelect = (Button)findViewById(R.id.btSelect);
        btSend = (Button)findViewById(R.id.btSend);
        btClean = (Button)findViewById(R.id.btClean);

        edSend = (EditText)findViewById(R.id.edSend);
        edRecv = (EditText)findViewById(R.id.edRecv);
        tvSend = (TextView)findViewById(R.id.tvSend);
        tvRecv = (TextView)findViewById(R.id.tvReceive);

        readData = new byte[readLength];
        readDataToText = new char[readLength];

        btSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("串口选择");
                        builder.setIcon(R.mipmap.setting);
                        builder.setCancelable(false);
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                View v = inflater.inflate(R.layout.layoutselect,null);
                final Spinner serial = (Spinner)v.findViewById(R.id.spinner_serial);
                final Spinner sp_baudrate = (Spinner)v.findViewById(R.id.spinner_boudrates);
                serial.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        port = (String)serial.getSelectedItem();
                        Log.i("bshui","port:"+port);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                sp_baudrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                        baudrate=Integer.parseInt((String)sp_baudrate.getSelectedItem());
                        Log.i("bshui","baudrate:"+baudrate);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                Button btOk = (Button)v.findViewById(R.id.btFOk);
                Button btCancel = (Button)v.findViewById(R.id.btFCancel);

                builder.setView(v);

                final Dialog dialog = builder.create();
                //弹出坐标

                dialog.getWindow().setGravity(Gravity.CENTER);
                dialog.show();

                btOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //初始化串口
                        dialog.dismiss();
                        try {
                            mSerialPort = new SerialPort(new File(port), baudrate);
                            mOutputStream = mSerialPort.getOutputStream();
                            mInputStream  = mSerialPort.getInputStream();
                            read_thread = new readThread();
                            read_thread.start();
                        }catch (IOException e){
                            e.printStackTrace();
                        }

                    }
                });
                btCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();

                    }
                });
            }
        });

        btClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edRecv.setText("");
                tvSend.setText("发送数据:0");
            }
        });

        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = edSend.getText().toString();
                try {
                    if (mOutputStream != null) {
                        mOutputStream.write(data.getBytes());
                        sendlen += data.length();
                        tvSend.setText("发送数据:"+sendlen);

                    } else {
                        return;
                    }

                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        });

        //主线程
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if(iavailable > 0){
                    edRecv.append(String.copyValueOf(readDataToText,0,iavailable));
                    recvlen += iavailable;
                    tvRecv.setText("接收数据:"+recvlen);
                }
            }
        };

    }

    private class readThread extends Thread{
        @Override
        public void run() {
            super.run();
            while(true){
                try{
                    Thread.sleep(50);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

                try{
                    if(mInputStream == null)
                        return;

                    iavailable = mInputStream.read(readData);

                    if(iavailable > 0) {

                        //字符串显示
                        for (int i = 0; i < iavailable; i++) {
                            readDataToText[i] = (char) readData[i];
                        }
                        Message msg = handler.obtainMessage();
                        handler.sendMessage(msg);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                    return;
                }
            }
        }
    }


}
