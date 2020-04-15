package com.jek.Pokemote;

import android.content.Context;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

public class AppUtils {

    // CONSTANTS
    private static final int JACK   = 0;
    private static final int MEL    = 1;
    private static final int WORKER = JACK;

    private static final int bufferSize = 4096;

    public static final int frameW = (WORKER == JACK) ? 640 - 1 : 1280 - 1;
    public static final int frameH = (WORKER == JACK) ? 360 - 1 : 720 - 1;

    public static final int nativeFrameW = 1920;
    public static final int nativeFrameH = 1080;

    public static final Scalar GREEN  = new Scalar(0,    255,    0,      100);
    public static final Scalar RED    = new Scalar(255,  0,      0,      100);
    public static final Scalar BLUE   = new Scalar(0,    0,      255,    100);
    public static final Scalar YELLOW = new Scalar(255,  255,    0,      100);
    public static final Scalar WHITE  = new Scalar(255,  255,    255,    100);
    public static final Scalar BLACK  = new Scalar(0,    0,      0,      100);
    public static final Scalar PURPLE = new Scalar(148,  0,      211,    100);

    private static Mat blank = new Mat( AppUtils.frameH+1, AppUtils.frameW+1,
                                        CvType.CV_8UC4, Scalar.all(0));

    public static Mat getBlankFrame(){ return blank.clone(); }

    public static void loadFileContext(Context current, File fp, int resId){
        InputStream is = current.getResources().openRawResource(resId);
        try {
            FileOutputStream os = new FileOutputStream(fp);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
        }catch (Exception e){
            // error
        } finally {
            try{
                is.close();
            } catch (Exception e){
                // error
            }
        }
    }

    public static void putImg(Mat frame, Mat img, Point offset){
        int adjustedX = (int)((offset.x < 0) ? 0 :
                            (offset.x + img.width() >= frameW) ?
                                    frameW - img.width(): offset.x);
        int adjustedY = (int)((offset.y < 0) ? 0 :
                            (offset.y + img.height() >= frameH) ?
                                    frameH - img.height(): offset.y);
        Mat frame_mask = frame.colRange(adjustedX, adjustedX + img.width())
                              .rowRange(adjustedY, adjustedY + img.height());
        Core.add(frame_mask,img,frame_mask);
    }

    public static int getRandomInt(int min, int max){
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    public static double clamp (double in, double min, double max){
        return (in < min) ? min : (in > max) ? max : in;
    }

    public static Rect rectExpand(Rect in, double factor){

        int X = (int)clamp((in.tl().x + in.br().x - factor*in.width)/2.,0.,frameW);
        int Y = (int)clamp((in.tl().y + in.br().y - factor*in.height)/2.,0.,frameH);
        int X2 = (int)clamp(X + in.width*factor,0,frameW);
        int Y2 = (int)clamp(Y + in.height*factor,0,frameH);

        return new Rect(X,Y,X2-X,Y2-Y);
    }

    public static Integer[] getMaxInds(Integer[] in){

        int max = -1; for (int val : in){ max = (val > max) ? val : max; }
        int nMax = 0; for (int val : in){ nMax += (val == max) ? 1 : 0; }

        Integer[] ret = new Integer[nMax]; int idx = 0;
        for (int i = 0; i < in.length; i++){
            if (in[i] == max){
                ret[idx] = i;
                idx++;
            }
        }

        return ret;
    }

    public static Mat getAlphaMask(Mat in){

        ArrayList<Mat> mChannels = new ArrayList<>();
        Core.split(in, mChannels);
        return mChannels.get(3);
    }

    public static Point downScalePoint(double X, double Y){
        return new Point(X / nativeFrameW * frameW, Y / nativeFrameH * frameH);
    }
}
