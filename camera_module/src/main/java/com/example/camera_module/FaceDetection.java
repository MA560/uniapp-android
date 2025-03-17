package com.example.camera_module;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

public class FaceDetection {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    private Context context;
    private TextureView mTextureView;
    private CameraSource cameraSource;
    private Activity activity; // 添加 Activity 成员变量

    //参数接受 context 和 TextureView
    public FaceDetection(Context context, TextureView mTextureView, Activity activity) {
        this.context = context;
        this.mTextureView = mTextureView;
        this.activity = activity; // 初始化 Activity
    }

    public void startFaceDetection() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }

        // 相机权限已授予，执行人脸检测逻辑
        startFaceDetectionInternal();
    }

    private void startFaceDetectionInternal() {
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        if (!detector.isOperational()) {
            Toast.makeText(context, "Face detection not available", Toast.LENGTH_SHORT).show();
            return;
        }

        cameraSource = new CameraSource.Builder(context, detector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT) // 前置摄像头
                .setRequestedFps(30.0f)
                .build();

        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                try {
//                    cameraSource.start(surface);
//                } catch (IOException e) {
//                    Log.e("FaceDetectionHelper", "Failed to start camera source", e);
//                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

        });

        detector.setProcessor(new Detector.Processor<Face>() {
            @Override
            public void release() {}

            @Override
            public void receiveDetections(Detector.Detections<Face> detections) {
                int faceCount = detections.getDetectedItems().size();
                Log.d("FaceDetectionHelper", "Number of faces detected: " + faceCount);

                // 在此处添加活体检测的逻辑
                // 可以通过检查人脸关键点的移动来进行简单的活体检测
                // 假设直接返回人脸数量作为分析结果
                processFaceDetectionResult(faceCount);
            }
        });
    }

    private void processFaceDetectionResult(int faceCount) {
        // 处理人脸检测结果
        // 这里只是简单地打印人脸数量，你可以根据需要进行更复杂的处理
        Log.d("FaceDetectionHelper", "Processed face detection result: " + faceCount);
    }

    public void stopFaceDetection() {
        if (cameraSource != null) {
            cameraSource.stop();
        }
    }
}

