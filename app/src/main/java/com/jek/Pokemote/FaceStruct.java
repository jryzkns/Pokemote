package com.jek.Pokemote;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class FaceStruct {

    Point tl;
    Rect bound;
    Point[] keyPoints, baselineKeyPoints;
    int     absentFrames, n;
    boolean ready;
    boolean collecting;

    FaceROI bLEye, bREye, bTLip, bLLip, bmouth;

    public FaceStruct(Point[] keyPoints, Rect bound){

        this.absentFrames = 0;
        this.tl = bound.tl().clone();
        this.bound = bound.clone();
        this.keyPoints = keyPoints.clone();
        this.ready = false;
        this.n = 0;
        this.collecting = false;

    }

    public void establishBaseline(){

        baselineKeyPoints = this.keyPoints.clone();

        bLEye =      new FaceROI(new int[]{  39,40,41,36,37,38}, baselineKeyPoints, bound);
        bREye =      new FaceROI(new int[]{  45,46,47,42,43,44}, baselineKeyPoints, bound);

        bTLip =      new FaceROI(new int[]{  54,64,63,62,61,60,48,49,50,51,52,53}, baselineKeyPoints, bound);
        bLLip =      new FaceROI(new int[]{  54,55,56,57,58,59,48,60,67,66,65,64}, baselineKeyPoints, bound);
        bmouth =     new FaceROI(new int[]{  60,61,62,32,64,65,66,67,60}, baselineKeyPoints, bound);

    }
}