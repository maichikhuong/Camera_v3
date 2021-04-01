package com.kone.camera_v3;

import android.util.Log;

/**
 * Created by Tran_ on 2/8/2018.
 */
/*
* This class is used for getting packet from frequencies have estimated from widths and read out duration*/
public class GetPacket {

    public final static int PREAMBLE = 2000;// 2000 Hz
    public final static int SYMBOL1 = 200; // 200 Hz
    public final static int SYMBOL2 = 250; // 250 Hz
    public final static int SYMBOL3 = 1000; // 1000 Hz
    public final static int SYMBOL4 = 1600; // 16000 Hz
    public final static int SPLITTER = 500; // 500 Hz
    public final static int LENGTH = 2; // Lenght = 2: EX : P-S-1-S-2-EoP, length = 4: EX: P-S-1-S-2-S-4-S-3-S-EoP
    public final static int THRESHOLD = 100; //Threshold of error

    private String result = "";
    private String result1 = "";

    private int STATE;

    public GetPacket(){
        result  = "";
        result1 = "";
    }

    public void setResult() {
        result = "";
    }
    public void setResult1(){
        result1 = "";
    }
    public String getResult(){
        return result;
    }
    public String getResult1(){
        return result1;
    }

    // function get packet from frequency
    public int getPacket(float frequencyInput,int state){
        switch (LENGTH){
            case 2:{
                STATE = getPacket_length2(frequencyInput,state);
                break;
            }
            case 4:{
                STATE = getPacket_length4(frequencyInput,state);
                break;
            }
        }
        return STATE;
    }

    private int getPreamble(float frequencyInput,int stateInput){
        if( (PREAMBLE - THRESHOLD) < frequencyInput && frequencyInput < (PREAMBLE + THRESHOLD)){
            Log.i(MainProcessing.TAG,"Symbol = P");
            result += "P";
            return stateInput+1;
        } else {
            return stateInput;
        }
    }

    private int getSS(float frequencyInput,int stateInput){
        if((SPLITTER - THRESHOLD) < frequencyInput && frequencyInput < (SPLITTER + THRESHOLD)){
            Log.i(MainProcessing.TAG,"Symbol = S");
            result += "S";
            return stateInput+1;
        } else {
            return stateInput;
        }
    }

    private int getSymbol(float frequencyInput, int stateInput){ // assign frequency to symbol, if frequency belong to [frequency - threshold, frequency + threshold] so frequency is assigned to corresponding symbol
        if((SYMBOL1 - THRESHOLD/4) < frequencyInput && frequencyInput < (SYMBOL1 + THRESHOLD/4)){
            Log.i(MainProcessing.TAG,"Symbol = 1");
            result += "1";
            return stateInput+1;
        }else if(SYMBOL2 - THRESHOLD/4 < frequencyInput && frequencyInput < SYMBOL2 + THRESHOLD/4) {
            Log.i(MainProcessing.TAG, "Symbol = 2");
            result += "2";
            return stateInput+1;
        }else if((SYMBOL3 - THRESHOLD) < frequencyInput && frequencyInput < (SYMBOL3 + THRESHOLD)) {
            Log.i(MainProcessing.TAG, "Symbol = 3");
            result += "3";
            return stateInput+1;
        }else if((SYMBOL4 - THRESHOLD) < frequencyInput && frequencyInput < (SYMBOL4 + THRESHOLD)) {
            Log.i(MainProcessing.TAG, "Symbol = 4");
            result += "4";
            return stateInput+1;
        } else {
            return stateInput;
        }
    }

    private int getEoP(float frequencyInput, int stateInput){
        if((PREAMBLE - THRESHOLD) < frequencyInput && frequencyInput < (PREAMBLE + THRESHOLD)){
           // Log.i(MainProcessing.TAG,"Symbol = EoP");
            result += "EoP";
            return stateInput+1;
        } else {
            return stateInput;
        }
    }

    //get packet with LENGTH is defined = 4;
    public int getPacket_length4(float frequencyInput,int stateInput){
        switch(stateInput){
            case 0:{
                stateInput = getPreamble(frequencyInput,stateInput);
                break;
            }
            case 1:{
                stateInput = getSS(frequencyInput,stateInput);
                break;
            }
            case 2:
            case 4:
            case 6:
            case 8:{
                stateInput = getSymbol(frequencyInput,stateInput);
                break;
            }
            case 3:
            case 5:
            case 7:
            case 9:{
                stateInput = getSS(frequencyInput,stateInput);
                break;
            }
            case 10:{
                stateInput = getEoP(frequencyInput,stateInput);
                break;
            }
        }
        return stateInput;
    }

    //get packet with LENGTH is defined = 2
    public int getPacket_length2(float frequencyInput,int stateInput){
        switch(stateInput){
            case 0:{
                stateInput = getPreamble(frequencyInput,stateInput);
                break;
            }
            case 1:{
                stateInput = getSS(frequencyInput,stateInput);
                break;
            }
            case 2:
            case 4:{
                stateInput = getSymbol(frequencyInput,stateInput);
                break;
            }
            case 3:
            case 5:{
                stateInput = getSS(frequencyInput,stateInput);
                break;
            }
            case 6:{
                stateInput = getEoP(frequencyInput,stateInput);
                break;
            }
        }
        return stateInput;
    }
}
