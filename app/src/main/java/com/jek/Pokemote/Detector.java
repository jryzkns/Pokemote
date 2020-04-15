package com.jek.Pokemote;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.face.Face;
import org.opencv.face.Facemark;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.util.ArrayList;

public class Detector {

    private Facemark fm;
    private final int NUM_KEYPOINTS = 68;
    private Point[] keyPoints;

    private CascadeClassifier cascade;
    private MatOfRect facesDetected;

    public Detector(Context current,
                    File LBPResFile, int LBPResID,
                    File LBFResFile, int LBFResID){

        this.keyPoints = new Point[NUM_KEYPOINTS];

        AppUtils.loadFileContext(current,LBFResFile, LBFResID);
        AppUtils.loadFileContext(current,LBPResFile, LBPResID);

        this.fm = Face.createFacemarkLBF();
        this.fm.loadModel(LBFResFile.getAbsolutePath());

        this.cascade = new CascadeClassifier(LBPResFile.getAbsolutePath());

        this.facesDetected = new MatOfRect();
    }

    public Point[] getKeyPoints(Mat in){

        ArrayList<MatOfPoint2f> landmarks = new ArrayList<>();
        this.fm.fit(    in,
                        new MatOfRect( new Rect( 0, 0, in.width(), in.height())),
                        landmarks);

        int count = 0;
        for (int i = 0; i < landmarks.size(); i++) {
            MatOfPoint2f lm = landmarks.get(i);
            for (int j = 0; j < lm.rows(); j++) {
                double[] dp = lm.get(j, 0);
                this.keyPoints[count] = new Point(  dp[0]/in.width(),
                                                    dp[1]/in.height());
                count++;
            }
        }

        return this.keyPoints;
    }

    public Rect[] getFaces(Mat in){
        this.cascade.detectMultiScale(in,this.facesDetected);
        return CvUtils.filterOverlap(this.facesDetected.toArray());
    }
}
