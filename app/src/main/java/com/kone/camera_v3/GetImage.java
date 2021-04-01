package com.kone.camera_v3;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;


/**
 * Created by Tran_ on 2/15/2018.
 */
/*
This class is used for getting image from urls which contain images on the internet.
 */
public class GetImage extends AppCompatActivity {

    private ImageView imageView ;
    private Button btnBack;

    private final static String url1 = "https://ae01.alicdn.com/kf/HTB1vRqbKFXXXXcxXVXXq6xXFXXXz/Sales-promotion-100pcs-Chinese-vegetables-seeds-50-pieces-broccoli-seeds-and-50-pieces-tomatoes-vegetable-seeds.jpg_640x640.jpg";
    private final static String url2 = "http://fairburyilattractions.com/wp-content/uploads/2017/10/Web-BIG-Meat-Sale.jpg";
    private final static String url3 = "http://www.dealstomealsblog.com/wp-content/uploads/2017/10/Maceys_Version_2_-_Weekly_Ad_-_October_4-10-2.png";
    private final static String url4 = "http://www.couponingtodisney.com/wp-content/uploads/2012/03/CVS-Coke-Deal.jpg";

    private final static int FAILURE = 1;

    private Handler myHandler;

    private  Message myMsg ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_image_layout);

        btnBack = (Button) findViewById(R.id.btnBacktoMainActivity);
        imageView = (ImageView)findViewById(R.id.imageView);

        initHandler();

        final Intent intent = this.getIntent();
        int key = intent.getIntExtra("KEY",0);
        switch (key){
            case 0:{
                break;
            }
            case 1:{
                loadImageFromURL(url1);
                break;
            }
            case 2:{
                loadImageFromURL(url2);
                break;
            }
            case 3:{
                loadImageFromURL(url3);
                break;
            }
            case 4:{
                loadImageFromURL(url4);
                break;
            }
        }

        btnBack.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onClick(View view) {
                    setResult(MainActivity.RESULT_CODE_2, intent);
                    //Intent myIntent = new Intent(GetImage.this, MainActivity.class);
                    //startActivity(myIntent);
                    finish();
            }
        });
    }

    private void initHandler() {
        myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case FAILURE:{
                        Toast.makeText(GetImage.this, "!!Can not load image!!", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        };
    }

    public void loadImageFromURL(String url){
        Picasso.with(this).load(url).placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                       // Log.i(MainProcessing.TAG,"FAILURE");
                        myMsg = new Message();
                        myMsg.what = FAILURE;
                        myHandler.sendMessage(myMsg);
                    }
                });
    }
}
