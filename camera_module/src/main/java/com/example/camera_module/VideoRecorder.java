package com.example.camera_module;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;

import android.content.Context;

import android.media.MediaRecorder;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;


public class VideoRecorder {
    private static final String TAG = "VideoRecorder";
    private MediaRecorder mediaRecorder; //配置1
    private MediaRecorder mediaRecorderFront; //前置摄像头配置1
    private MediaRecorder mediaRecorderBack; //后置摄像头配置2
    private CameraDevice cameraDevice;
    private CameraDevice frontCameraDevice;
    private CameraDevice backCameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private Surface previewSurface;
    private Surface recorderSurface;
    private boolean isRecording = false;
    private Handler mainHandler;
    private AtomicInteger recordingCounter = new AtomicInteger(0);
    private int cameraFacing; // 定义摄像头方向变量
    /**
     * 构造函数
     * 初始化相机设备和预览表面
     *
     * @param cameraDevice  相机设备
     * @param previewSurface 预览表面
     */
    public VideoRecorder(CameraDevice cameraDevice, Surface previewSurface) {
        this.cameraDevice = cameraDevice;
        this.previewSurface = previewSurface;
        this.mediaRecorder = new MediaRecorder();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 初始化 MediaRecorder
     *
     * @param videoFile 视频文件
     * @throws IOException IO异常
     */
    public void initMediaRecorder(MediaRecorder mediaRecorder,File videoFile,String direction) throws IOException {
        //设置音频源为麦克风，即录制声音的输入来源。
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置视频源为 Surface，这通常用于捕获屏幕内容或相机预览。
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //设置输出文件格式为 MPEG-4。
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //设置视频编码比特率，即视频的压缩率，这里设置为 10 Mbps。
        mediaRecorder.setVideoEncodingBitRate(10000000);
        //设置视频帧率为 30 帧每秒。
        mediaRecorder.setVideoFrameRate(30);
        //设置视频的宽度和高度，这里设置为 1920x1080 像素。
        mediaRecorder.setVideoSize(1920, 1080); // 根据需要设置
        //设置视频编码器为 H.264，这是一种常用的视频编码格式。
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //设置音频编码器为 AAC，这也是一种常用的音频编码格式。
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // 设置视频旋转方向为90度，即逆时针旋转90度
        if(direction == "back"){
            mediaRecorder.setOrientationHint(90);
        }else{
            mediaRecorder.setOrientationHint(270); // 如果是前置摄像头，通常需要逆时针旋转270度
        }
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.prepare();
        recorderSurface = mediaRecorder.getSurface();
    }

    /**
     * 开始录制视频
     *
     * @param videoFile 视频文件
     */
    public void startRecording(File videoFile,String direction) {
        try {
            // 初始化 MediaRecorder
            initMediaRecorder(mediaRecorder,videoFile,direction);

            // 创建用于录制视频的 Surface 列表
            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(previewSurface);
            surfaces.add(recorderSurface);
            Log.d("CameraOpen----", "videoRecorder--创建 ");
            if(cameraDevice != null) {
                // 创建相机捕获会话
                cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        cameraCaptureSession = session;
                        Log.d("CameraOpen----", "videoRecorder--0111录制 ");
                        try {
                            // 创建捕获请求
                            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                            captureRequestBuilder.addTarget(previewSurface);
                            captureRequestBuilder.addTarget(recorderSurface);
                            Log.d("CameraOpen----", "videoRecorder--录制 ");
                            // 设置重复请求，开始录制视频
                            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            mediaRecorder.start();
                            isRecording = true;
                        } catch (CameraAccessException | IllegalStateException e) {
                            Log.e("CameraOpen----", "videoRecorder--err", e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "Failed to configure camera capture session");
                    }
                }, mainHandler);
            }else{
                Log.d("CameraOpen----", "cameraDevice 为 null");
            }
        } catch (CameraAccessException | IOException e) {
            Log.d("CameraOpen----", "录制异常 ");
            throw new RuntimeException(e);
        }
    }
    public void stopDoubleRecording() {
        if (!isRecording) {
            return;
        }
        isRecording = false;
        try {
            mediaRecorderFront.stop();
            mediaRecorderFront.reset();
            mediaRecorderFront.release();
            mediaRecorderBack.stop();
            mediaRecorderBack.reset();
            mediaRecorderBack.release();
            frontCameraDevice.close();
            backCameraDevice.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 停止录制视频
     */
    public void stopRecording() {
        if (!isRecording) {
            return;
        }
        isRecording = false;
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
        mediaRecorder = null;

        cameraCaptureSession.close();
        cameraCaptureSession = null;
    }

    /**
     * 检查当前是否正在录制
     *
     * @return 如果正在录制返回 true，否则返回 false
     */
    public boolean isRecording() {
        return isRecording;
    }

    public void setIsRecording(boolean isRecording) {
        this.isRecording=isRecording;
    }
    /**
     * 保存录制的视频到本地文件
     *
     * @param filePath 文件路径
     */
    public void saveRecordingToFile(String filePath) {
        // 实现将录制的文件保存到本地的逻辑，此处省略
    }
    //双录方法
    public void startFrontCameraDeviceRecording(File videoFile, CameraDevice cameraDevice, TextureView preRecorderSurface, CameraCaptureSession mPreviewSession1) {
        try {
            if (mPreviewSession1 != null) {

                mPreviewSession1.close();
            }
            mediaRecorderFront = new MediaRecorder();
            // 初始化 MediaRecorder
            initMediaRecorder(mediaRecorderFront,videoFile,"front");
            // 创建录制视频的Surface
            Surface frontRecorderSurface = mediaRecorderFront.getSurface();
            frontCameraDevice=cameraDevice;
            // 前置摄像头的surface
            Surface previewSurfaceFront = new Surface(preRecorderSurface.getSurfaceTexture());
            List<Surface> frontSurfaces = new ArrayList<>();
            frontSurfaces.add(frontRecorderSurface);
            frontSurfaces.add(previewSurfaceFront);
            // 创建前置摄像头的CaptureSession
            Log.d("CameraOpen----", "font录制开始");
            frontCameraDevice.createCaptureSession(frontSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // 创建CaptureRequest以及录制视频的Surface
                        CaptureRequest.Builder frontCaptureRequest = frontCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        frontCaptureRequest.addTarget(frontRecorderSurface);
                        frontCaptureRequest.addTarget(previewSurfaceFront);
                        //回调
                        session.setRepeatingRequest(frontCaptureRequest.build(), null, null);
                        mediaRecorderFront.start();
                        if (recordingCounter.incrementAndGet() == 2) {
                            isRecording = true;
                        }
                        Log.d("CameraOpen----", "font录制成功");
                    } catch (CameraAccessException | IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // 处理配置失败
                    Log.e("CameraOpen----", "font 录制结束");
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startBackCameraDeviceRecording(File videoFile, CameraDevice cameraDevice,TextureView preRecorderSurface,CameraCaptureSession mPreviewSession2) {
        isRecording = false;
        try {
            if (mPreviewSession2 != null) {
                //关闭预览视图
                mPreviewSession2.close();
            }
            mediaRecorderBack = new MediaRecorder();
            // 初始化 MediaRecorder
            initMediaRecorder(mediaRecorderBack,videoFile,"back");
            // 创建录制视频的Surface
            Surface backRecorderSurface = mediaRecorderBack.getSurface();
            backCameraDevice=cameraDevice;
            // 前置摄像头的surface
            Surface previewSurfaceFront = new Surface(preRecorderSurface.getSurfaceTexture());
            List<Surface> backSurfaces = new ArrayList<>();
            backSurfaces.add(backRecorderSurface);
            backSurfaces.add(previewSurfaceFront);
            // 创建前置摄像头的CaptureSession
            Log.d("CameraOpen----", "back开始录制");
            backCameraDevice.createCaptureSession(backSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // 创建CaptureRequest以及录制视频的Surface
                        CaptureRequest.Builder backCaptureRequest = backCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        backCaptureRequest.addTarget(backRecorderSurface);
                        backCaptureRequest.addTarget(previewSurfaceFront);
                        session.setRepeatingRequest(backCaptureRequest.build(), null, null);
                        mediaRecorderBack.start();
                        isRecording = true;
                        Log.d("CameraOpen----", "back录制成功");
                    } catch (CameraAccessException | IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // 处理配置失败
                    Log.e(TAG, "Back camera 录制失败");
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //检测文件符不符合
    public static void checkVideoFile(String videoFilePath) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            File videoFile = new File(videoFilePath);
            if (!videoFile.exists()) {
                Log.e("CameraOpen--*--", "视频文件不存在。");
                return;
            }

            extractor.setDataSource(videoFilePath);
            int trackCount = extractor.getTrackCount();
            if (trackCount == 0) {
                Log.e("CameraOpen--*--", "视频文件中未找到轨道。");
                return;
            }

            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d("CameraOpen--*--", "轨道 " + i + " 的 MIME 类型: " + mime + ":" + videoFilePath);

                // 检查 MIME 类型是否受支持
                if (!isSupportedMime(mime)) {
                    Log.e("CameraOpen--*--", "不支持的 MIME 类型: " + mime + ":" + videoFilePath);
                }

                // 根据需要添加更多检查...
            }

        } catch (IOException e) {
            Log.e("CameraOpen--*--", "读取视频文件失败: " + e.getMessage());
        } finally {
            extractor.release();
        }
    }

    private static boolean isSupportedMime(String mime) {
        // Define a list of supported MIME types
        List<String> supportedMimeTypes = Arrays.asList(
                "video/mp4",
                "video/avc"
                // Add more supported MIME types as needed...
        );
        return supportedMimeTypes.contains(mime);
    }
}
