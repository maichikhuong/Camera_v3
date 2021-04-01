package com.kone.camera_v3;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import static java.lang.Math.round;

/**
 * Created by Tran_ on 2/15/2018.
 */
public class MainProcessing extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    public final static String TAG = "MainActivity";

    public final static String str1 = "PS3S2SEoP"; //id1
    public final static String str2 = "PS1S2SEoP"; //id2
    public final static String str3 = "PS4S3SEoP"; //id3
    public final static String str4 = "PS1S4SEoP"; //id4

    private final static int MESSAGE_READ_OUT = 1; // message for handler update UI thread
    private final static int MESSAGE_RESULT = 2;
    private final static int MESSAGE_DECODE_FAILURE = 3;

    public final static int INITIAL = 0;   // state for decode method
    public final static int LEARNING_READOUT = 1;
    public final static int DECODE_PACKET = 2;

    private JavaCameraView javaCameraView;

    private TextView textViewResult;
    private TextView textViewReadOutTime;

    private Mat mRGBa;
    private Mat matT;

    private String result = "";

    private byte[][] frameBuffer;

    private int state = 0;
    private int step = 0;
    private int id1 = 0;
    private int id2 = 0;
    private int decodeEnable = 0;

    private Handler myHandler;
    private Message msg;

    private float readOutTimeTemp = 0;
    private float readOutTime = 0;

    private GetPacket getPacket = new GetPacket();

    private ImageProcessing imageProcessing;

    private WidthEstimation widthEstimation;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_processing_layout);

        textViewResult = (TextView) findViewById(R.id.textViewResult);
        textViewReadOutTime = (TextView) findViewById(R.id.textViewReadOutTime);

        final Button buttonInitial = (Button) findViewById(R.id.btnRestart);

        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.setFocusable(true);

        id1 = 0;
        id2 = 12;

        Intent intent = this.getIntent();
        float readOutTime1 = intent.getFloatExtra("ReadOutTime",0);
        if(readOutTime1 != 0){
            readOutTime = readOutTime1;
            //Log.i(TAG,"ReadOutTime = "+readOutTime);
            textViewReadOutTime.setText("Tr = "+readOutTime+" s");
        }
        int step1 = intent.getIntExtra("STEP",0);
        if(step1 != 0){
            step = step1;
        }

        step = DECODE_PACKET;
        readOutTime = (float) 0.00002;
        initHandler();

        // function for restart button is pressed
        buttonInitial.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                id1 = 0;
                id2 = 12;
                decodeEnable = 0;
                getPacket.setResult();
                state = 0;
                readOutTimeTemp = (float) 0.00002;
                result = "";
                step = DECODE_PACKET;
                textViewResult.setText(result);
                textViewReadOutTime.setText("");
                Thread.currentThread().interrupt();
            }
        });
    }

    // this method is used for child thread update UI thread
    private void initHandler(){
        myHandler = new Handler(){
          @Override
            public void handleMessage(Message msg){
              switch (msg.what){
                  case MESSAGE_READ_OUT: { //update read out duration time in textViewReadOutTime
                      textViewReadOutTime.setText("Tr = "+readOutTime+" s");
                      break;
                  }
                  case MESSAGE_RESULT:{ //update result in textViewResult
                      textViewResult.setText(result);
                      break;
                  }
                  case MESSAGE_DECODE_FAILURE:{ // show toast in UI thread
                      Toast.makeText(MainProcessing.this, "Sorry!!!, code is decoded failure", Toast.LENGTH_SHORT).show();
                      break;
                  }
              }
          }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            //Log.i(TAG, "Open loaded successfully");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            //Log.i(TAG, "Open not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, baseLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
        id1 = 0;
        id2 = 12;
        cleanBuffer(frameBuffer);
        Thread.currentThread().interrupt();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
        id1 = 0;
        id2 = 12;
        cleanBuffer(frameBuffer);
        Thread.currentThread().interrupt();
    }

    // method need override when implement CameraBridgeViewBase.CvCameraViewListener2
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBa = new Mat(height, width, CvType.CV_8UC4);              // create new Mat for variable mRGBa
        matT = new Mat(mRGBa.height(),mRGBa.width(),CvType.CV_8UC4);
        frameBuffer = new byte[100][(int)(mRGBa.total()*mRGBa.channels())]; // cread buffer
    }


    @Override
    public void onCameraViewStopped() {
        mRGBa.release();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBa = inputFrame.rgba();   // take frame from camera

        //create new thread for put frame into buffer
        new Thread(new Runnable() {
            @Override
            public void run() {
                frameBuffer = put2Buffer(mRGBa,id1,frameBuffer); // put frame into buffer
                id1 ++;
                if(id1  >= 100){
                    id1 = 0;
                }
                if(id1 >= 12){
                    decodeEnable =1; // enable flag to allow decode method
                }
            }
        }).start();
        if(decodeEnable == 1 && id1 != id2){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    matT = getFromBuffer(matT,id2-12,frameBuffer); // get frame from buffer
                    decoder(matT); // decode frame to get packet
                    id2++;
                    if(id2 >= 112){
                        id2 = 12;
                    }
                }
            }).start();
        }

        msg = new Message();  // create message to update UI thread
        msg.what = MESSAGE_RESULT;
        myHandler.sendMessage(msg); // send message to handler
        //if(STATE == 11){ // length = 4 so when receive EoP the state = 11
        if(state == 7){ // length = 2 so when receive EoP the state = 7
            getImageFormID(result); // state == 7 is camera receive EoP symbol, so if send ID to get Image from URLs
        }

        Thread.currentThread().interrupt();

        ImageProcessing imageProcessing = new ImageProcessing(mRGBa);

        return imageProcessing.rotationMat(); //rotation frame for display in javacameraview
    }

    // method decode frame to get packer
    public void decoder(final Mat inputMat) {
        Mat output;
        float width;
        float frequency;

        // perform some image processing works
        imageProcessing = new ImageProcessing(inputMat);
        output = imageProcessing.imageProcessing();

        // perform width estimation
        widthEstimation = new WidthEstimation(output);
        widthEstimation.estimateWidth(state);
        //get width
        width = 2*widthEstimation.getWidth(); // 1 pair - 1 bright and 1 dark

        //state for learning read out duration and get packet when learn read out duration complete
        switch(step){
            case INITIAL:{
                if (width == 0) {
                    step = INITIAL;
                    Log.i(TAG, "----------------------------------");
                    Log.i(TAG, "STEP  = " + step);
                    Log.i(TAG, "STATE = "+ step);
                    Log.i(TAG, "Tr = " + readOutTime + " s");
                    Log.i(TAG, "Width = " + width);
                    break;
                } else {
                    readOutTimeTemp = 1 / (2 * width * GetPacket.PREAMBLE);
                    step = LEARNING_READOUT;
                    Log.i(TAG, "----------------------------------");
                    Log.i(TAG, "STEP  = " + step);
                    Log.i(TAG, "STATE = "+ step);
                    Log.i(TAG, "Tr = " + readOutTime + " s");
                    Log.i(TAG, "Width = " + width);
                    break;
                }
            }
            case LEARNING_READOUT:{
                frequency = round(1 / (2 * width * readOutTimeTemp));
                if (frequency > (GetPacket.PREAMBLE - GetPacket.THRESHOLD) && frequency < (GetPacket.PREAMBLE + GetPacket.THRESHOLD) && readOutTimeTemp > 1.90E-5 && readOutTimeTemp < 1.92E-5 ) {
                    readOutTime = readOutTimeTemp;
                    step = DECODE_PACKET;

                    // create message to update UI thread
                    Message msg = new Message();
                    msg.what = MESSAGE_READ_OUT;
                    myHandler.sendMessage(msg);
                } else {
                    step = INITIAL;
                }
                break;
            }
            case DECODE_PACKET:{
                frequency = 1/(2*width*readOutTime);
                Log.i(TAG, "----------------------------------");
                Log.i(TAG, "STEP  = " + step);
                Log.i(TAG, "STATE = "+ step);
                Log.i(TAG, "F = " + frequency + " Hz");
                Log.i(TAG, "Tr = " + readOutTime + " s");
                Log.i(TAG, "Width = " + width);
                state = getPacket.getPacket(frequency,state); // get packet
                result = getPacket.getResult(); // get result which is a string include some symbol
                break;
            }
        }
    }

    //method for putting mat frame into buffer
    private byte[][] put2Buffer(Mat inputMat,int id,byte[][] buffer){
        inputMat.get(0,0,buffer[id]);
        return buffer;
    }

    //method for getting Mat frame from buffer
    private Mat getFromBuffer(Mat inputMat,int id,byte[][] buffer){
        Mat outputMat = new Mat(inputMat.height(),inputMat.width(),CvType.CV_8UC4);
        outputMat.put(0,0,buffer[id]);
        return outputMat;
    }

    //method for clearning bufffer
    private byte[][] cleanBuffer(byte[][] buffer){
        for(int i = 0; i < buffer.length;i++){
            buffer[i] = null;
        }
        return buffer;
    }

    // method get Image from url by sending a key to GetImage.class by create new intent
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void getImageFormID(String str){
        int key;
        switch (str){ // check ID to send key to new thread for getting image
            case str1:{
                key = 1;
                break;
            }
            case str2:{
                key = 2;
                break;
            }
            case str3:{
                key = 3;
                break;
            }
            case str4:{
                key = 4;
                break;
            }
            default:{
                state = 0;
                id1 = 0;
                id2 = 12;
                result = "";
                getPacket.setResult();
                msg = new Message();
                msg.what = MESSAGE_DECODE_FAILURE;
                myHandler.sendMessage(msg);
                return;
            }
        }
        //create new intent to get image and show that image on new screen
        Intent myIntent = new Intent(MainProcessing.this,GetImage.class);
        myIntent.putExtra("KEY",key);
        startActivityForResult(myIntent,MainActivity.REQUEST_CODE_2);
    }

    // method for getting result from intent
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == MainActivity.REQUEST_CODE_2){
            if(resultCode == MainActivity.RESULT_CODE_2) {
                id1 = 0;
                id2 = 12;
                result = "";
                state = 0;
                getPacket.setResult();
                cleanBuffer(frameBuffer);
                Thread.currentThread().interrupt();
                //Log.i(TAG, "result = 2");
                data.putExtra("ReadOutTime",readOutTime);
                data.putExtra("STEP",step);
                setResult(MainActivity.RESULT_CODE_1, data); // back to MainActivity
                finish();
            }
        }
    }
}


