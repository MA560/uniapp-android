package com.example.camera_module;

import static android.hardware.camera2.CaptureRequest.*;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;

import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class CameraHelper {
    private static final String TAG = "CameraHelper";
    public static final int CAMERA_FRONT = 0;
    public static final int CAMERA_BACK = 1;
    private final CameraManager mCameraManager;
    private CameraDevice mCameraDevice1; // 前置摄像头
    private CameraDevice mCameraDevice2; // 后置摄像头
    private Builder mPreviewRequestBuilder1;
    private Builder mPreviewRequestBuilder2;
    private CameraCaptureSession mPreviewSession1;
    private CameraCaptureSession mPreviewSession2;
    private final TextureView textureView;
    private VideoRecorder videoRecorder;
    private final Context context;
    private CameraStateCallback cameraStateCallback;
    private final CameraDeviceWrapper mCameraDeviceWrapper1 = new CameraDeviceWrapper(); // 前置摄像头
    private final CameraDeviceWrapper mCameraDeviceWrapper2 = new CameraDeviceWrapper(); // 后置摄像头

    private CameraDevice frontCameraDevice;
    private CameraDevice backCameraDevice;



    // 封装CameraDevice的包装器类
    private static class CameraDeviceWrapper {
        private CameraDevice mCameraDevice;

        public CameraDevice getCameraDevice() {
            return mCameraDevice;
        }

        public void setCameraDevice(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
        }
    }

    // 构造函数，可能需要传入上下文和 TextureView 实例等参数
    public CameraHelper(Context context, int cameraType, TextureView textureView) {
        this.textureView = textureView;
        this.context = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    // 回调接口，相机开启或关闭时通知外部类
    public interface CameraStateCallback {
        void onCameraOpened(CameraDevice cameraDevice);

        void onCameraClosed(CameraDevice cameraDevice);
        // 还可以根据需要添加其他回调方法
    }
    //回调绑定
    public void setCameraStateCallback(CameraStateCallback callback) {
        this.cameraStateCallback = callback;
    }

    // 打开前置或后置摄像头 同时只能有一个
    public void openCameras() {
        closeCameras();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            String[] cameraIds = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (lensFacing != null && lensFacing == getLensFacing()) {
                    if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                        openCamera(cameraId, mCameraDeviceWrapper1,lensFacing);
                    } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        openCamera(cameraId, mCameraDeviceWrapper2, lensFacing);
                    }
                } else {
                    Log.e(TAG, "无法获取相机特性.");
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // 设置CameraDevic
//    private void setCameraDevice(@Nullable CameraDevice camera, @Nullable Integer lensFacing) {
//        if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
//            mCameraDevice1 = camera; // 前置摄像头
//        } else if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
//            mCameraDevice2 = camera; // 后置摄像头
//        } else {
//            // 如果无法确定摄像头类型，则根据默认摄像头类型进行设置
//            int defaultLensFacing = getLensFacing();
//            if (defaultLensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
//                mCameraDevice1 = camera; // 前置摄像头
//            } else if (defaultLensFacing == CameraCharacteristics.LENS_FACING_BACK) {
//                mCameraDevice2 = camera; // 后置摄像头
//            }
//        }
//    }

    private void openCamera(String cameraId, final CameraDeviceWrapper cameraDeviceWrapper,int lensFacing) {
        // 检查相机权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: 请求相机权限
            return;
        }
        try {
            mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDeviceWrapper.setCameraDevice(camera);
//                setCameraDevice(camera,null);
                    if (cameraStateCallback != null) {
                        cameraStateCallback.onCameraOpened(camera);
                    }
                    if(lensFacing ==  CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraDevice1 = camera;
                    } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        mCameraDevice2 = camera;
                    }
                    //createCameraPreviewSession(camera);
                    createCameraPreviewSession(cameraDeviceWrapper.getCameraDevice());
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDeviceWrapper.setCameraDevice(null);
                    if(lensFacing ==  CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraDevice1 = null;
                    } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        mCameraDevice2 = null;
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDeviceWrapper.setCameraDevice(null);
                    if(lensFacing ==  CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraDevice1 = null;
                    } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        mCameraDevice2 = null;
                    }
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void switchCamera(int lensFacing) {
        closeCameras();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            String[] cameraIds = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == lensFacing) {
                    Log.d("CameraOpen----", "cameraId--switch " + cameraId);
                    mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            if (cameraStateCallback != null) {
                                cameraStateCallback.onCameraOpened(camera);
                            }
                            if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                                mCameraDevice1 = camera;
                            } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                                mCameraDevice2 = camera;
                            }
                            createCameraPreviewSession(camera);
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            camera.close();
                            if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                                mCameraDevice1 = null;
                            } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                                mCameraDevice2 = null;
                            }
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            camera.close();
                            if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                                mCameraDevice1 = null;
                            } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                                mCameraDevice2 = null;
                            }
                        }
                    }, null);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 打开前后摄像头并创建预览会话 双录的打开方法
    public Map<String,Object> openDualCameras(TextureView frontTextureView, TextureView backTextureView) {
        closeCameras();
        Map<String,Object> map=new HashMap<>();
        // 检查相机权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 处理权限问题
            return null;
        }
        try {
            String[] cameraIds = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (lensFacing != null) {
                    if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                        // 打开前置摄像头并创建预览会话
                        mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                            @Override
                            public void onOpened(@NonNull CameraDevice camera) {
                                mCameraDevice1 = camera;
                                // 创建前置摄像头的预览会话
                                createCameraPreviewSessionTwo(mCameraDevice1, frontTextureView);
                                map.put("device1",mCameraDevice1);

                            }

                            @Override
                            public void onDisconnected(@NonNull CameraDevice camera) {
                                camera.close();
                                mCameraDevice1 = null;
                            }

                            @Override
                            public void onError(@NonNull CameraDevice camera, int error) {
                                camera.close();
                                mCameraDevice1 = null;
                            }
                        }, null);
//                        list.add(mCameraDevice1);

                    } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        // 打开后置摄像头并创建预览会话
                        mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                            @Override
                            public void onOpened(@NonNull CameraDevice camera) {
                                mCameraDevice2 = camera;
                                // 创建后置摄像头的预览会话
                                createCameraPreviewSessionTwo(mCameraDevice2, backTextureView);
                                map.put("device2",mCameraDevice2);

                            }

                            @Override
                            public void onDisconnected(@NonNull CameraDevice camera) {
                                camera.close();
                                mCameraDevice2 = null;
                            }

                            @Override
                            public void onError(@NonNull CameraDevice camera, int error) {
                                camera.close();
                                mCameraDevice2 = null;
                            }
                        }, null);
//                        list.add(mCameraDevice2);

                    }
                } else {
                    Log.e(TAG, "无法获取相机特性");
                }
            }
            map.put("mPreviewSession1",mPreviewSession1);
            map.put("mPreviewSession2",mPreviewSession2);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return map;
    }
    // 创建摄像头预览会话 双录的创建方法
    private void createCameraPreviewSessionTwo(CameraDevice cameraDevice, TextureView textureView) {
        try {
            Surface surface = new Surface(textureView.getSurfaceTexture());
            Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            // 根据摄像头设备不同选择不同的会话
            CameraCaptureSession.StateCallback sessionCallback = new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == mCameraDevice1) {
                        mPreviewSession1 = session;
                    } else if (cameraDevice == mCameraDevice2) {
                        mPreviewSession2 = session;
                    }
                    // 更新预览
                    updatePreview(session, previewRequestBuilder);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "无法配置摄像头预览会话");
                }
            };

            // 根据摄像头设备不同选择不同的输出
            if (cameraDevice == mCameraDevice1) {
                mCameraDevice1.createCaptureSession(Arrays.asList(surface), sessionCallback, null);
            } else if (cameraDevice == mCameraDevice2) {
                mCameraDevice2.createCaptureSession(Arrays.asList(surface), sessionCallback, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void createCameraPreviewSession(CameraDevice cameraDevice) {
        try {
            Surface surface = new Surface(textureView.getSurfaceTexture());
            Builder previewRequestBuilder;
            CameraCaptureSession previewSession;

            if (cameraDevice  == mCameraDevice1) {
                previewRequestBuilder = mCameraDevice1.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                // 设置人脸检测相关的控制选项
//                previewRequestBuilder.set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE);//对焦
//                previewRequestBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON_ALWAYS_FLASH);//曝光
//                previewRequestBuilder.set(CONTROL_AE_PRECAPTURE_TRIGGER, CONTROL_AE_PRECAPTURE_TRIGGER_START);//预拍触发器
//                previewRequestBuilder.set(CaptureRequest.CONTROL_MAX_REGIONS_AF, 1);//最大自动对焦区域数量
//                previewRequestBuilder.set(CaptureRequest.CONTROL_MAX_REGIONS_AE, 1);//追打自动曝光区域数量
//                previewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL); // 设置人脸检测模式
//                previewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MAX_COUNT, 2); //设置最大人脸检测数量

                previewRequestBuilder.addTarget(surface);
                mPreviewRequestBuilder1 = previewRequestBuilder;
                mCameraDevice1.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        mPreviewSession1 = session;
                        updatePreview(mPreviewSession1,mPreviewRequestBuilder1);
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "Failed to configure camera preview session.");
                    }
                }, null);
            } else if (cameraDevice == mCameraDevice2) {
                previewRequestBuilder = mCameraDevice2.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(surface);
                mPreviewRequestBuilder2 = previewRequestBuilder;
                mCameraDevice2.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        mPreviewSession2 = session;
                        updatePreview(mPreviewSession2, mPreviewRequestBuilder2);
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "Failed to configure camera preview session.");
                    }
                }, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview(CameraCaptureSession previewSession, Builder previewRequestBuilder) {
        if (previewSession  == null || previewRequestBuilder == null) {
            return;
        }

        try {
            previewSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //  返回当前打开的摄像头设备
    public CameraDevice getCurrentCameraDevice() {
        // 假设你有一个变量来记录当前使用的摄像头是前置或后置，你可以根据这个来返回相应的CameraDevice实例
        if (mCameraDevice1 != null) {
            return mCameraDevice1;
        } else if (mCameraDevice2 != null) {
            return mCameraDevice2;
        }
        return null;
    }
    // 关闭摄像头  // 关闭当前正在使用的相机
    public void closeCameras() {
        if (mCameraDevice1 != null) {
            mCameraDevice1.close();
            mCameraDevice1 = null;
        }
        if (mCameraDevice2 != null) {
            mCameraDevice2.close();
            mCameraDevice2 = null;
        }
    }
    // 其他必要的方法，如设置预览尺寸、启动预览等
    //获取是否有摄像头
    public int getLensFacing() {
        if (hasBackCamera()) {
            return CameraCharacteristics.LENS_FACING_BACK;
        }
        if (hasFrontCamera()) {
            return CameraCharacteristics.LENS_FACING_FRONT;
        }
        return -1;
    }
    //翻转朝向
    public int switchLensType(int lensType){
        return lensType == CameraCharacteristics.LENS_FACING_FRONT ? CameraCharacteristics.LENS_FACING_BACK : CameraCharacteristics.LENS_FACING_FRONT;
    }
    //是否有后摄像头
    public boolean hasBackCamera() {
        return hasCameraWithFacing(CameraCharacteristics.LENS_FACING_BACK);
    }

    //是否有前摄像头
    public boolean hasFrontCamera() {
        return hasCameraWithFacing(CameraCharacteristics.LENS_FACING_FRONT);
    }

    // 辅助方法：根据面向检查是否有指定的相机
    private boolean hasCameraWithFacing(int facing) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == facing) {
                    return true;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "检查相机可用性失败。", e);
        }
        return false;
    }
}