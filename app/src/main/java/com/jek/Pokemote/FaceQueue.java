package com.jek.Pokemote;

import android.util.Log;

import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.ArrayList;


public class FaceQueue {

    public ArrayList<FaceStruct> queue;
    private final int missingLifetime = 10;
    private final int BASELINE_N = 10;

    public FaceQueue(){
        this.queue = new ArrayList<>();
    }

    public void update(){

        for (int faceInd = queue.size() - 1; faceInd >= 0; faceInd -- ){
            queue.get(faceInd).absentFrames += 1;

            if (queue.get(faceInd).n >= BASELINE_N && !queue.get(faceInd).ready){
                queue.get(faceInd).ready = true;
                queue.get(faceInd).establishBaseline();
            }

            if (queue.get(faceInd).absentFrames >= missingLifetime){
                queue.remove(faceInd);
                Log.d("main",Integer.toString(queue.size()));
            }
        }

    }


    public int getQueuePos(Point[] keyPoints, Rect bound){

        int ind = -1;

        Boolean faceExisting = false;

        for (int fInd = 0; fInd < queue.size(); fInd++ ) {
            if (CvUtils.L2Dist( queue.get(fInd).tl, bound.tl()) < CvUtils.neglDisp) {

                faceExisting = true;
                queue.get(fInd).absentFrames = 0;

                if (!queue.get(fInd).ready && queue.get(fInd).collecting) {

                    for (int pInd = 0; pInd < keyPoints.length; pInd++) {
                        keyPoints[pInd] = new Point(
                                CvUtils.updateMean( queue.get(fInd).keyPoints[pInd].x,
                                                    queue.get(fInd).n,
                                                    keyPoints[pInd].x),
                                CvUtils.updateMean( queue.get(fInd).keyPoints[pInd].y,
                                                    queue.get(fInd).n,
                                                    keyPoints[pInd].y));
                    }

                    queue.get(fInd).n += 1;
                }

                queue.get(fInd).keyPoints = keyPoints.clone();
                queue.get(fInd).bound = bound.clone();

                ind = fInd;
                break;
            }
        }

        if (!faceExisting){
            queue.add(new FaceStruct(keyPoints, bound));
            ind = queue.size() - 1;
            Log.d("main",Integer.toString(queue.size()));
        }

        return ind;
    }

    public void clearAll(){
        queue.clear();
    }

}

