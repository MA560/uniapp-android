package com.example.camera_module;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

public class PhotoModule {
    private Context mContext; // 应用上下文
    private CameraDevice mCameraDevice; // 相机设备
    private CameraCaptureSession mCaptureSession; // 相机捕获会话
    private ImageReader mImageReader; // 图像读取器
    private Handler mBackgroundHandler; // 后台处理Handler
    private HandlerThread mBackgroundHandlerThread; // 后台处理线程
    private CaptureRequest.Builder mCaptureBuilder; // 捕获请求构建器

    public PhotoModule(Context context) {
        mContext = context;
        startBackgroundThread(); // 启动后台线程
    }

    // 拍摄照片并将ImageData回调给调用者
    public void takePictureAndReturn(CameraDevice cameraDevice, ImageAvailableListener listener) {
        initializeCameraAndImageReader(cameraDevice, listener, /* saveToFile */ false);
    }

    // 拍摄照片并保存到本地
    public void takePictureAndSave(CameraDevice cameraDevice, final String filePath) {
        ImageAvailableListener listener = new ImageAvailableListener() {
            @Override
            public void onImageAvailable(ByteBuffer imageData) {
                saveImageToFile(imageData, new File(filePath));
            }
        };
        initializeCameraAndImageReader(cameraDevice, listener, /* saveToFile */ true);
    }

    // 初始化CameraDevice和ImageReader，准备拍照
    private void initializeCameraAndImageReader(CameraDevice cameraDevice, ImageAvailableListener listener, boolean saveToFile) {
        mCameraDevice = cameraDevice;

        Size imageDimension = new Size(1920, 1080); // 替换成你希望的图片尺寸
        mImageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(), ImageFormat.JPEG, 1);

        mImageReader.setOnImageAvailableListener(reader -> {
            try (Image image = reader.acquireLatestImage()) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                listener.onImageAvailable(buffer);
            }
        }, mBackgroundHandler);

        createCaptureSession(mImageReader.getSurface());
    }

    // 保存图像到本地文件系统
    private void saveImageToFile(ByteBuffer buffer, File file) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // 创建用于拍照的CaptureSession
    private void createCaptureSession(Surface targetSurface) {
        try {
            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureBuilder.addTarget(targetSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(targetSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureSession = session;
                            captureImage();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            // 可以在这里处理配置失败的情况
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 执行实际拍照的方法
    private void captureImage() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // 发起还拍请求
            mCaptureSession.capture(mCaptureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                // 可以在这里处理捕获完成事件等
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 启动后台线程的私有方法
    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("CameraBackground");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    // 停止后台线程的私有方法
    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 释放资源方法
    public void release() {
        closeCaptureSession();
        closeCameraDevice();
        stopBackgroundThread(); // 停止后台线程
    }

    // 关闭捕获会话
    private void closeCaptureSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    // 关闭相机设备
    private void closeCameraDevice() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
    // ...（背景线程方法不变）

    // 图像可用时的回调接口
    public interface ImageAvailableListener {
        void onImageAvailable(ByteBuffer imageData);
    }
}