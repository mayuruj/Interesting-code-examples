package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
import android.view.WindowManager;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;
    private Mat                    rgba;
    private Mat                    gray;
    private File                   cascadeFile;
    private CascadeClassifier      javaDetector;
    private DetectionBasedTracker  nativeDetector;

    private int                    detectorType       = JAVA_DETECTOR;
    private String[]               detectorName;

    private float                  relativeFaceSize   = 0.2f;
    private int                    absoluteFaceSize   = 0;

    private CameraBridgeViewBase   openCvCameraView;

    private BaseLoaderCallback  loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(cascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        javaDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
                        if (javaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            javaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

                        nativeDetector = new DetectionBasedTracker(cascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    openCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FdActivity() {
        detectorName = new String[2];
        detectorName[JAVA_DETECTOR] = "Java";
        detectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        openCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, loaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        openCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        gray = new Mat();
        rgba = new Mat();
    }

    public void onCameraViewStopped() {
        gray.release();
        rgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        rgba = inputFrame.rgba();
        gray = inputFrame.gray();

        if (absoluteFaceSize == 0) {
            int height = gray.rows();
            if (Math.round(height * relativeFaceSize) > 0) {
                absoluteFaceSize = Math.round(height * relativeFaceSize);
            }
            nativeDetector.setMinFaceSize(absoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (detectorType == JAVA_DETECTOR) {
            if (javaDetector != null)
                javaDetector.detectMultiScale(gray, faces, 1.1, 2, 2, 
                        new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }
        else if (detectorType == NATIVE_DETECTOR) {
            if (nativeDetector != null)
                nativeDetector.detect(gray, faces);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(rgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 2);
         
        return rgba;
    }

  /*  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        iFace50 = menu.add("Face size 50%");
        iFace40 = menu.add("Face size 40%");
        iFace30 = menu.add("Face size 30%");
        iFace20 = menu.add("Face size 20%");
        iType   = menu.add(mDetectorName[mDetectorType]);
        return true;
    }*/

   // @Override
  /*  public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == iFace50)
            setMinFaceSize(0.5f);
        else if (item == iFace40)
            setMinFaceSize(0.4f);
        else if (item == iFace30)
            setMinFaceSize(0.3f);
        else if (item == iFace20)
            setMinFaceSize(0.2f);
        else if (item == iType) {
            detectorType = (detectorType + 1) % detectorName.length;
            item.setTitle(detectorName[detectorType]);
            setDetectorType(detectorType);
        }
        return true;
    }
*/
   /* private void setMinFaceSize(float faceSize) {
        relativeFaceSize = faceSize;
        absoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (detectorType != type) {
            detectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                nativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                nativeDetector.stop();
            }
        }
    }*/
}
