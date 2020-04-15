package com.jek.Pokemote;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class FaceROI {
    public Point[] ROIContour;
    private double area;
    public FaceROI(int[] subsetInd, Point[] keyPoints, Rect bound){
        this.ROIContour = CvUtils.getSubsetPoints(subsetInd, keyPoints, bound);
        this.area = CvUtils.polylineArea(this.ROIContour);
    }

    public double getArea(){ return this.area; }
}
