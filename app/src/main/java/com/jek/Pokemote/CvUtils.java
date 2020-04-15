package com.jek.Pokemote;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Arrays;

public class CvUtils {

    public static final int neglDisp = 50;

    public static Point[] getSubsetPoints(int[] subsetInd, Point[] points, Rect bound){

        Point[] r = new Point[subsetInd.length];
        for (int i = 0; i < subsetInd.length; i++){
            r[i] = new Point(   points[subsetInd[i]].x * bound.width + bound.tl().x,
                                points[subsetInd[i]].y * bound.height + bound.tl().y);
        }
        return r;
    }

    public static Point[] expandROI(Point[] inROI, double scale){

        Point centroid = getROICentroid(inROI);
        Point[] outROI = new Point[inROI.length];

        for (int i = 0; i < outROI.length; i++){
            outROI[i] = new Point(
                    centroid.x + (inROI[i].x - centroid.x)* scale,
                    centroid.y + (inROI[i].y - centroid.y)* scale
            );
        }

        return outROI;
    }

    public static Point getROICentroid(Point[] ROI){
        double sumX = 0.0;
        double sumY = 0.0;
        for (Point ROIPoint : ROI){
            sumX += ROIPoint.x;
            sumY += ROIPoint.y;
        }

        return new Point(sumX/ROI.length, sumY/ROI.length);
    }

    public static ArrayList<MatOfPoint> getExpandedROIContour(  int[] roiInds,
                                                                double scale,
                                                                Point[] points, Rect bound){

        Point[] ROIPoints = expandROI(getSubsetPoints(roiInds, points, bound),scale);

        return getROIContour(ROIPoints);
    }

    public static ArrayList<MatOfPoint> getROIContour(int[] roiInds, Point[] points, Rect bound){

        Point[] ROIPoints = getSubsetPoints(roiInds, points, bound);

        return getROIContour(ROIPoints);
    }

    public static ArrayList<MatOfPoint> getROIContour(Point[] ROIPoints){

        ArrayList<MatOfPoint> ROI = new ArrayList<>();
        MatOfPoint roiMat = new MatOfPoint();
        roiMat.fromList(new ArrayList<>(Arrays.asList(ROIPoints)));
        ROI.add(roiMat);

        return ROI;
    }

    public static double polylineArea(int[] roiInds, Point[] points, Rect bound){

        Point[] ROIPoints = getSubsetPoints(roiInds, points, bound);

        return polylineArea(ROIPoints);
    }

    public static double polylineArea(Point[] r){

        int n = r.length;
        double area = 0.0;
        for (int i = 0; i < n - 1; i++){
            area += r[i].x*r[i+1].y - r[i+1].x*r[i].y;
        }
        return Math.abs(area + r[n-1].x*r[0].y - r[0].x*r[n-1].y) / 2.0;
    }

    public static double L2Dist(Point a, Point b){

        return Math.sqrt(
                            Math.pow(a.x - b.x, 2)
                          + Math.pow(a.y - b.y, 2));
    }

    public static Rect[] filterOverlap(Rect[] in){

        ArrayList<Rect> validBuffer = new ArrayList<>();
        int i, j;
        Boolean overlapped;

        for (i = 0; i < in.length; i ++){
            overlapped = false;
            for (j = 0; j < in.length; j++){
                if (j == i){ continue; }
                if (        in[i].tl().x >= in[j].tl().x
                         && in[i].tl().y >= in[j].tl().y
                         && in[i].tl().x <= in[j].br().x
                         && in[i].tl().y <= in[j].br().y)
                {
                    overlapped = true;
                    break;
                }
            }

            if (!overlapped){
                validBuffer.add(in[i].clone());
            }
        }

        Rect[] retArr = new Rect[validBuffer.size()];

        for (i = 0; i < validBuffer.size(); i ++){
            retArr[i] = validBuffer.get(i).clone();
        }

        return retArr;
    }

    public static double updateMean(double oldMean, int oldN, double newVal){
        return (oldMean * oldN + newVal)/(oldN + 1);
    }

}
