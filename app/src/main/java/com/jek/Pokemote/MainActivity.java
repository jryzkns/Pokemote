package com.jek.Pokemote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.File;

import static org.opencv.imgproc.Imgproc.fillPoly;
import static org.opencv.imgproc.Imgproc.rectangle;

public class MainActivity extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{

    // camera ind 0: back cam; camera ind 1: selfie cam
    private int                     currentCamera = 1;
    private int                     frameCount;
    private int                     refreshRate = (currentCamera == 1) ? 3 : 2;
    
    private CameraBridgeViewBase    cameraBridgeViewBase;
    private BaseLoaderCallback      baseLoaderCallback;
    private GestureDetectorCompat   gestureDetector;

    private AppRes                  res;

    private boolean                 isFrozen = false;
    private Mat                     currentFrame, frozenFrame, processingFrame, canvas;
    private Mat                     pokeImage;

    private Detector                detect;
    private InferenceEngine         infEng;

    private boolean                 debug = false;
    private boolean                 easterEgg = false;

    private FaceQueue               faceQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // remove UI elements
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);     //  remove title bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);    //  remove notification bar
        setContentView(R.layout.activity_main);

        gestureDetector  = new GestureDetectorCompat(this,this);
        gestureDetector.setOnDoubleTapListener(this);

        // start opencv
        OpenCVLoader.initDebug();

        //  Request Camera permissions
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                                                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(  MainActivity.this,
                                                    new String[]{Manifest.permission.CAMERA},
                                                    1);
            }
        }

        // start camera bridge view base
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setCameraIndex(currentCamera);
        cameraBridgeViewBase.setMaxFrameSize(AppUtils.frameW + 1,AppUtils.frameH + 1);

        // set up base loader
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch(status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        cameraBridgeViewBase.enableFpsMeter();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        frameCount       = 0;

        processingFrame  = new Mat();
        frozenFrame      = new Mat(); // needed to prevent race condition (?)
        canvas           = new Mat();

        res              = new AppRes(this.getApplicationContext());
        faceQueue        = new FaceQueue();


        infEng           = new InferenceEngine();
        detect           = new Detector(this.getApplicationContext(),
                            new File(getDir(    "cascade", Context.MODE_PRIVATE),
                                                "lbpcascade_frontalface.xml"),
                            R.raw.lbpcascade_frontalface,
                            new File(getDir(    "lbf", Context.MODE_PRIVATE),
                                                "lbfmodel.yaml"),
                            R.raw.lbfmodel);

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat baseFrame = isFrozen ? frozenFrame.clone() : inputFrame.rgba();

        if (!isFrozen) {
            // img has to be flipped so it displays correctly when selfie camera is used
            if (currentCamera == 1) { Core.flip(baseFrame, baseFrame, 1); }
            currentFrame = baseFrame.clone();
        }

        Imgproc.cvtColor(baseFrame, processingFrame, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.equalizeHist(processingFrame, processingFrame);

        if (frameCount % refreshRate == 0) {

            faceQueue.update();

            canvas.release();
            canvas = AppUtils.getBlankFrame();

            for (Rect faceBound : detect.getFaces(processingFrame)) {

                Rect orig = faceBound.clone();
                faceBound = AppUtils.rectExpand(faceBound, 1.7);
                Mat faceMat = processingFrame  .colRange(   (int) faceBound.tl().x,
                                                            (int) faceBound.br().x)
                                               .rowRange(   (int) faceBound.tl().y,
                                                            (int) faceBound.br().y);

                Point[] keyPoints = detect.getKeyPoints(faceMat);

                int queueInd = faceQueue.getQueuePos(keyPoints, faceBound);

                if (!faceQueue.queue.get(queueInd).collecting){

                    rectangle(  canvas,
                                faceBound.tl(),
                                faceBound.br(),
                                AppUtils.RED,
                                3);
                }

                if (debug) {

                    rectangle(  canvas,
                            faceBound.tl(),
                            faceBound.br(),
                            faceQueue.queue.get(queueInd).collecting ?
                                    faceQueue.queue.get(queueInd).ready
                                            ? AppUtils.GREEN
                                            : AppUtils.PURPLE
                                    : AppUtils.RED,
                            3);

                    Imgproc.circle( canvas,
                                    faceBound.tl(),
                                    CvUtils.neglDisp,
                                    AppUtils.RED,
                                    1);

                    int counter = 0;
                    for (Point facePoint : keyPoints) {
                        Point drawPoint = new Point(
                                facePoint.x * faceMat.width()  + faceBound.tl().x,
                                facePoint.y * faceMat.height() + faceBound.tl().y);
                        Imgproc.circle(canvas, drawPoint, 1, AppUtils.RED, 1);
                        Imgproc.putText(canvas, Integer.toString(counter), drawPoint,
                                Core.FONT_HERSHEY_COMPLEX, 0.25, AppUtils.WHITE);
                        counter++;
                    }

                }

                if (faceQueue.queue.get(queueInd).collecting) {
                    pokeImage = res.pokeDB.get(
                            faceQueue.queue.get(queueInd).ready
                                    ? infEng.infer(faceQueue.queue.get(queueInd))
                                    : 0);

                    AppUtils.putImg(canvas, pokeImage,
                            new Point(orig.tl().x + orig.width,
                                    faceBound.tl().y
                            ));
                }
            }
        }

        canvas.copyTo(baseFrame, AppUtils.getAlphaMask(canvas));

        if (easterEgg){
            for (Rect b_ : detect.getFaces(processingFrame)) {
                b_ = AppUtils.rectExpand(b_, 1.7);
                Point[] k_ = detect.getKeyPoints(processingFrame
                        .colRange(  (int) b_.tl().x, (int) b_.br().x)
                        .rowRange(  (int) b_.tl().y, (int) b_.br().y));
                Mat p_ = AppUtils.getBlankFrame();
                fillPoly(p_, CvUtils.getExpandedROIContour(
                        new int[]{22, 23, 24, 25, 26},1.3, k_, b_), AppUtils.WHITE);
                fillPoly(p_, CvUtils.getExpandedROIContour(
                        new int[]{21, 17, 18, 19, 20},1.3, k_, b_), AppUtils.WHITE);
                Imgproc.cvtColor(p_, p_,Imgproc.COLOR_RGBA2GRAY);
                Imgproc.cvtColor(baseFrame, baseFrame,Imgproc.COLOR_RGBA2RGB);
                Photo.inpaint(baseFrame, p_, baseFrame,8,Photo.INPAINT_NS);
                Imgproc.cvtColor(baseFrame, baseFrame,Imgproc.COLOR_RGB2RGBA);
            }
        }

        frameCount = (frameCount + 1);

        return baseFrame;
    }

    private void swapCamera() {
        Toast.makeText(getApplicationContext(),"Camera Switch!", Toast.LENGTH_SHORT).show();
        //bitwise not operation to flip 1 to 0 and vice versa
        currentCamera = currentCamera^1;
        cameraBridgeViewBase.disableView();
        cameraBridgeViewBase.setCameraIndex(currentCamera);
        cameraBridgeViewBase.enableView();

        refreshRate = (currentCamera == 1) ? 4 : 2;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {

        if (!isFrozen){
            Toast.makeText(getApplicationContext(),"Fling!", Toast.LENGTH_SHORT).show();
            swapCamera();
        }

        faceQueue.clearAll();

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {

        isFrozen = !isFrozen;

        Toast.makeText(getApplicationContext(),
                isFrozen ? "FREEZE" : "UNFREEZE",
                Toast.LENGTH_SHORT).show();

        frozenFrame = currentFrame.clone();

        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {

        if (isFrozen){
            debug = !debug;
            if (!debug){
                cameraBridgeViewBase.disableFpsMeter();
            }else{
                cameraBridgeViewBase.enableFpsMeter();
            }

            Toast.makeText(getApplicationContext(),
                    "DEBUG ".concat(debug ? "ON":  "OFF"),
                    Toast.LENGTH_SHORT).show();

        } else{
            easterEgg = !easterEgg;
            Toast.makeText(getApplicationContext(),
                    easterEgg   ? "I got your eyebrows!"
                                :  "Okay I'll give them back to you geez",
                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        if (this.gestureDetector.onTouchEvent(event)) {
            return true;
        }

        for (int i = 0; i < faceQueue.queue.size(); i++){
            if (    faceQueue.queue.get(i).bound.contains(
                        AppUtils.downScalePoint(event.getX(), event.getY()))
                    && !faceQueue.queue.get(i).collecting){
                faceQueue.queue.get(i).collecting = true;
            }
        }

        return super.onTouchEvent(event);
    }


// -------------------------------------------------------------------------------------------------
    // look into GestureDetector.SimpleOnGestureListener
    @Override protected void onResume() { super.onResume();
        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"Load Failed!", Toast.LENGTH_SHORT).show();
        } else { baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS); } }
    @Override protected void onPause() { super.onPause();
        if (cameraBridgeViewBase!=null){ cameraBridgeViewBase.disableView(); } }
    @Override protected void onDestroy() { super.onDestroy();
        if (cameraBridgeViewBase!=null){ cameraBridgeViewBase.disableView(); } }
    @Override public void onCameraViewStarted(int width, int height) { }
    @Override public void onCameraViewStopped() { }
    @Override public boolean onDoubleTapEvent(MotionEvent event) { return true; }
    @Override public boolean onDown(MotionEvent event){ return true; }
    @Override public void onShowPress(MotionEvent event) {}
    @Override public boolean onSingleTapUp(MotionEvent event) { return true; }
    @Override public boolean onSingleTapConfirmed(MotionEvent event) { return true; }
    @Override public boolean onScroll(MotionEvent event1, MotionEvent event2,
                            float distanceX, float distanceY) { return true; }

}

