package com.jek.Pokemote;

import android.text.TextUtils;
import android.util.Log;


public class InferenceEngine {

    public InferenceEngine(){
    }

    int infer(FaceStruct inFace){

        Integer[] votes = new Integer[]{0,0,0,0,0,0,0,0,0};

        FaceROI LEye =      new FaceROI(new int[]{  39,40,41,36,37,38}, inFace.keyPoints, inFace.bound);
        FaceROI REye =      new FaceROI(new int[]{  45,46,47,42,43,44}, inFace.keyPoints, inFace.bound);

        FaceROI TLip =      new FaceROI(new int[]{  54,64,63,62,61,60,48,49,50,51,52,53}, inFace.keyPoints, inFace.bound);
        FaceROI LLip =      new FaceROI(new int[]{  54,55,56,57,58,59,48,60,67,66,65,64}, inFace.keyPoints, inFace.bound);
        FaceROI mouth =     new FaceROI(new int[]{  60,61,62,32,64,65,66,67,60}, inFace.keyPoints, inFace.bound);

        double noseLipDist    = (inFace.keyPoints[57].y - inFace.keyPoints[33].y)
                                /(inFace.baselineKeyPoints[57].y - inFace.baselineKeyPoints[33].y);
        double eyeOpeness     = (LEye.getArea() + REye.getArea())
                                /(inFace.bLEye.getArea() + inFace.bREye.getArea());
        double mouthOpenness  = mouth.getArea()/inFace.bmouth.getArea();
        double lipThickness   = (TLip.getArea() + LLip.getArea())
                                /(inFace.bTLip.getArea() + inFace.bLLip.getArea());

//        1 Jynx
//        fat lips    + eyes normal + norm noselip  + mouth big
//        2 Patrat:
//        nonfat lips + eyes wide   + short noselip + mouth close
//        3 Lickitung
//        nonfat lips + eyes normal + norm noselip  + mouth open      + tongue
//        4 Magikarp
//        nonfat lips + eyes wide   + norm noselip  + mouth big
//        5 Pikachu:
//        nonfat lips + eyes normal + norm noselip  + mouth small
//        6 Grookey:
//        fat lips    + eyes normal + long noselip  + mouth close
//        7 Rattata
//        nonfat lips + eyes small  + norm noselip  + mouth small      + teeth(?)
//        8 Snorlax:
//        nonfat lips + eyes small  + norm noselip  + mouth close

        Log.d("face",String.format("lips: %f",lipThickness));
        if (lipThickness > 1.2) { // thick lips
            votes[1]+=3;
            votes[6]+=1;
        } else {
            votes[0]+=1;
            votes[2]+=1;
            votes[3]+=1;
            votes[4]+=1;
            votes[5]+=1;
            votes[7]+=1;
            votes[8]+=1;
        }

        Log.d("face",String.format("eyes: %f",eyeOpeness));
        if (eyeOpeness < 0.6) { // eyes closed or wide smile
            votes[8]+=4;
            votes[7]+=2;
        } else if (eyeOpeness < 1){   // eyes regular
            votes[0]+=1;
            votes[3]+=1;
            votes[5]+=1;
            votes[6]+=1;
        } else {    // eyes wide open
            votes[2]+=1;
            votes[4]+=1;
        }

        Log.d("face",String.format("noselip: %f",noseLipDist));
        if (noseLipDist > 1.08) {
            votes[6]+=4;
        } else if (noseLipDist > 0.9){
            votes[0]+=1;
            votes[3]+=1;
            votes[4]+=1;
            votes[5]+=1;
            votes[7]+=1;
            votes[8]+=1;
        } else {
            votes[2]+=4;
        }

        Log.d("face",String.format("mouth: %f",mouthOpenness));
        if (mouthOpenness > 1.2) {
            votes[1] += 3;
            votes[3] += 3;
            votes[4] += 3;
        } else if (mouthOpenness < 0.9){
            votes[2] += 1;
            votes[5] += 3;
            votes[6] += 1;
            votes[8] += 3;
        } else{
            votes[7] += 3;
        }


        Log.d("face", "?,J,P,L,M,P,G,R,S");
        Log.d("face", TextUtils.join(",",votes));
        int decision = 0; // defaults to 0
        Integer[] topVotes = AppUtils.getMaxInds(votes);

        if (topVotes.length > 1){
            decision = topVotes[AppUtils.getRandomInt(0,topVotes.length-1)];
        } else if (topVotes.length == 1){
            decision = topVotes[0];
        }

        return decision;

    }
}
