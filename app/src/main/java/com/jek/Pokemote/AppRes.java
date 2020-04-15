package com.jek.Pokemote;

import android.content.Context;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.HashMap;

public class AppRes {

    public HashMap<Integer, Mat> pokeDB;

    public AppRes(Context current) {
        pokeDB = new HashMap<>();
        pokeadd(current, 0,R.drawable.unknown);
        pokeadd(current, 1, R.drawable.jynx);
        pokeadd(current, 2, R.drawable.patrat);
        pokeadd(current, 3, R.drawable.lickitung);
        pokeadd(current, 4, R.drawable.magikarp);
        pokeadd(current, 5, R.drawable.pikachu);
        pokeadd(current, 6, R.drawable.grookey);
        pokeadd(current, 7, R.drawable.rattata);
        pokeadd(current, 8, R.drawable.snorlax);
    }

    private void pokeadd(Context current, int key, int resID){
        try {
            Mat pokeImg = Utils.loadResource(current, resID, CvType.CV_8UC4);
            Imgproc.cvtColor(pokeImg,pokeImg,Imgproc.COLOR_BGRA2RGBA);
            this.pokeDB.put(key, pokeImg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}