package com.kone.camera_v3;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/*
* This class is used for some image processing function.
* Main function in imageProcesing at line 68
* */

public class ImageProcessing {
    private Mat inputMat;
    private Mat outputMat;

    public ImageProcessing(Mat mat){
        inputMat = mat;
    }

    public void setMat(Mat mat){
        inputMat = mat;
    }

    public Mat getMat(){
        return outputMat;
    }

    private Mat convertRGB2Gray(){ // method convert RGB color to gray scale color
        Mat gray =new Mat();
        Imgproc.cvtColor(inputMat,gray, Imgproc.COLOR_RGB2GRAY);
        return gray;
    }

    public Mat rotationMat(){ // method rotate frame 90 degree
        Mat ouputMat = this.inputMat.t();
        Core.flip(this.inputMat.t(), ouputMat, 1);
        Imgproc.resize(ouputMat, ouputMat, this.inputMat.size());
        return ouputMat;
    }

    private Mat edgeDetection(Mat gray, double threshold1, double threshold2){ // method perform edge detection
        Mat canny = new Mat();
        Imgproc.Canny(gray,canny,threshold1,threshold2,3,false);
        return canny;
    }

    private Mat findContours(Mat canny){ // method find contours according to edge have found

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(canny.clone(), contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            // for each contour, display it in green
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                Imgproc.drawContours(inputMat, contours, idx, new Scalar(0, 255, 0)); // set pixel at contours is  green : R = 0, G = 255, B = 0
            }
        }
        return inputMat;
    }

    public Mat imageProcessing(){
        inputMat = rotationMat(); // rotate the image 90 degrees
        Mat gray = convertRGB2Gray(); // convert rgb to grayscale
        Mat canny = edgeDetection(gray,50,210); // edge detection use canny filter
        outputMat = findContours(canny); // find the contours according to edges has found
        return outputMat; // return image frame after set green color at pixel at contours
    }
}
