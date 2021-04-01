package com.kone.camera_v3;

import org.opencv.core.Mat;

import static java.lang.Math.abs;

/**
 * Created by Tran_ on 2/7/2018.
 */
/*
* This class is used for estimating strips width and calculate error according to YIN algorithm
* Main function is estimateWidth function at line 179
* */
public class WidthEstimation {

    private final static int LEFT = 1;
    private final static int RIGHT = 0;
    private final static int NO_MIX = -1;

    private Mat inputMat;

    private int rows;
    private int cols;
    private int isLeft = -2;
    private int firstLocationLeft;
    private int firstLocationRight;

    private float widthLeft;
    private float widthRight;
    private float errorLeft;
    private float errorRight;
    private float width;
    private float error;
    private float widthSymbol;
    private float widthSS;
    private float errorSymbol;
    private float errorSS;

    public WidthEstimation(Mat mat){
        inputMat = mat;
        rows = inputMat.rows();
        cols = inputMat.cols();
    }

    public void setMat(Mat mat){
        inputMat = mat;
        rows = inputMat.rows();
        cols = inputMat.cols();
    }

    public Mat getMat(){
        return inputMat;
    }

    public float getWidthLeft(){
        float [] data = getWidth(rows/2,cols/2 - 200,cols/2,10);
        firstLocationLeft = (int)data[1];
        widthLeft = data[0];
        errorLeft = getError(rows/2,firstLocationLeft,(int)widthLeft);
        return widthLeft;
    }

    public float getWidthRight(){
        float [] data = getWidth(rows/2,cols/2+50,cols/2+250,10);
        firstLocationRight = (int)data[1];
        widthRight = data[0];
        errorRight = getError(rows/2,firstLocationRight,(int)widthRight);
        return widthRight;
    }

    public int getFirstLocationLeft(){
        return firstLocationLeft;
    }

    public int getFirstLocationRight(){
        return firstLocationRight;
    }

    public float getErrorLeft(){
        return errorLeft;
    }

    public float getErrorRight(){
        return errorRight;
    }

    public float getWidthSS(){
        return widthSS;
    }

    public float getWidthSymbol(){
        return widthSymbol;
    }

    public float getErrorSS(){
        return errorSS;
    }

    public float getErrorSymbol(){
        return  errorSymbol;
    }

    public float getWidth(){
        return width;
    }

    public float getError(){
        return error;
    }

    public int getIsLeft(float width1, float width2, int firstLocation1, int firstLocation2){

        if(  (float)48.5 <(width1) && (width1) < (float)51.5 && abs(width1 - width2) > (float)4 ){ // checl slipster is on left?
            if(firstLocation1 < cols/2){
                isLeft = LEFT;
            }
        } else if ((float)48.5 <(width2) && (width2) < (float)51.5 && abs(width1 - width2) > (float)4){ // check is slipster is on right?
            if(firstLocation2 > cols/2){
                isLeft = RIGHT;
            }
        } else{ // no mix-symbol
            isLeft = NO_MIX;
        }
        return isLeft;
    }

    public void assignWidth(){

        if(isLeft == LEFT){ // slipster is at left so width of slipster is width left + error at left and symbol is at right
            widthSS = widthLeft + errorLeft;
            widthSymbol = widthRight + errorRight;
            errorSS = errorLeft;
            errorSymbol = errorRight;
        }else if(isLeft == RIGHT){ //slipster is at right so width of slipster is width right + error at right and symbol is at left
            widthSymbol = widthLeft + errorLeft;
            widthSS = widthRight + errorRight;
            errorSymbol = errorLeft;
            errorSS = errorRight;
        }else if(isLeft == NO_MIX){ // no mix-symbol
            widthSymbol = (widthLeft + widthRight)/2 + (errorLeft + errorRight)/2;
            errorSymbol = (errorLeft + errorRight)/2;
        }
    }

    public void getWidthError(int STATE ){

        //Packet: P-S-1-S-4-S-2-S-3-S - EoP
        //STATE:  0 1 2 3 4 5 6 7 8 9   10
        //STATE = 2,4,6,8: is symbol
        //STATE = 1,3,5,7,9: is splitter
        //STATE = 0: is preamble
        //STATE = 6 or STATE = 10: is EoP

        switch (STATE){
            case 0:
            case 2:
            case 4:
            case 6:
            case 8:
            case 10:{
                width = widthSymbol;
                error = errorSymbol;
                break;
            }
            case 1:
            case 3:
            case 5:
            case 7:{
                if(isLeft == -1){ // no mix-symbol
                    width = widthSymbol;
                    error = errorSymbol;
                }else {
                    width = widthSS;
                    error = errorSS;
                }
                break;
            }
        }
    }

    public void estimateWidth(int STATE){
        getWidthLeft(); // conculate width in 1/2 frame left
        getWidthRight(); // conculate width in 1/2 frame right
        getIsLeft(widthLeft,widthRight,firstLocationLeft,firstLocationRight); // check slipster is at left of right of no mix-sumbol
        assignWidth(); // assign width which is symbol width and which is slipster width
        getWidthError(STATE); // assign output width and output error
    }

    public float[] getWidth(int row, int colLow, int colHigh, int size) {// size = number of time to calculate width
        float[] returnVal = new float[2];
        float width = 0;
        float distance = 0;

        int numberEdge = 0;
        int firstLocation = 0;
        int secondLocation;

        // when perform edge detection, edge is mark by change data of pixel to [0,255,0] with R = 0, G = 255, B = 0;
        // so to calculate width, we count number of pixel between two pixel which have data are [0,255,0].
        for (int k = (-1) * size; k < size; k = k + 2) {
            for (int i = colLow; i < colHigh; i++) {
                double[] data = inputMat.get(row + k, i);
                if (data[0] == 0 && data[1] == 255 && data[2] == 0) { // check if pixel data is [0,255,0]?
                    if (numberEdge == 0) {
                        firstLocation = i;
                    }
                    numberEdge++;
                    if (numberEdge == 3) { // count number pixel between 3 edge
                        secondLocation = i;
                        distance = (float) (secondLocation - firstLocation);
                        distance = distance / (float)(numberEdge - 1); // because 3 edge is two strip, so distance (pixel) equal distance divide by 2
                        break;
                    }
                }
            }
            width = distance + width;
        }
        width = width/(float)size;
        returnVal[0] = width;
        returnVal[1] = firstLocation; // first location is used for conculate error of estimation strip width
        return returnVal;
    }

    public float getError(int row,int col,int theta){ // detail in reference [1] in user guide
        double I1 = 0;
        double I2 = 0;
        double I3 = 0;
        double I4 = 0;
        float diff1 = 0;
        float diff2 = 0;
        float diff3 = 0;
        float a;
        float error = 0;

        for(int i = col; i < col+theta/2;i++){
            for(int j = row-5;j < row+5;j++){
                double[] data1 = inputMat.get(j,j/2);
                double[] data2 = inputMat.get(j,j+theta);
                double[] data3 = inputMat.get(j,j+theta-1);
                double[] data4 = inputMat.get(j,j+theta+1);
                float Y1 = (float) (data1[0]*65.481 + data1[1]*128.533 + data1[2]*24.966 + 16); // convert to RGB to get Y in YCrCb
                float Y2 = (float) (data2[0]*65.481 + data2[1]*128.533 + data2[2]*24.966 + 16);
                float Y3 = (float) (data3[0]*65.481 + data4[1]*128.533 + data3[2]*24.966 + 16);
                float Y4 = (float) (data4[0]*65.481 + data4[1]*128.533 + data4[2]*24.966 + 16);
                I1 = I1 + Y1;
                I2 = I2 + Y2;
                I3 = I3 + Y3;
                I4 = I4 + Y4;
            }
            diff1 = (float) (diff1 + (I1 - I2)*(I1 - I2));
            diff2 = (float) (diff2 + (I1 - I3)*(I1 - I3));
            diff3 = (float) (diff3 + (I1 - I4)*(I1 - I4));
        }
        a = 4*(2*diff1-diff3-diff2);
        if(a != 0){
            error = (diff3 - diff2)/a;
        }
        return error;
    }
}
