package com.example.camera_module;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.camera2.CameraCaptureSession;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Size;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCharacteristics;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import java.io.IOException;
import java.util.Locale;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.nio.ByteBuffer;

import android.graphics.SurfaceTexture;
import java.util.concurrent.CountDownLatch;
import com.example.camera_module.FFmpegModule;

//相机预览页面
public class CameraPageActivity extends AppCompatActivity {
    private File outputDirectory;  // 输出的日志
    private File mVecordFile ;  // 输出的文件
    //相机进程提供者

    // 相机进程提供者 Future，相机初始化完成后返回相机进程提供者实例化对象
    private int mLensFacing;

    private TextureView mPreviewView; //相机和 单录的视图
    private PhotoModule photoModule;
    protected TextureView textureView1;
    protected TextureView textureView2;

    private CameraHelper textureViewCamera; // 相机录像(单录) 和 拍照使用

    private CameraHelper cameraHelper; // 双录使用

    private CameraHelper cameraHelperFront; // 双录使用
    private CameraHelper cameraHelperBack; // 双录使用
    private VideoRecorder videoRecorder; //录制视频

    //新的引入
    private static final String TAG = "CameraPageActivity";
    private ExecutorService cameraExecutor;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Surface previewSurface; // 录制视频使用的
    private String cameraId;
    private File videoFile;

    private File videoFile1;

    private File videoFile2;
    private Size videoSize; // 视频分辨率大小，需要根据实际情况设置
    private boolean isRecording = false;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private CameraDevice frontCameraDevice, backCameraDevice;
    private CameraCaptureSession frontCaptureSession, backCaptureSession;

    private  Map<String,Object> dMap ;

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,//相机
            Manifest.permission.RECORD_AUDIO,//录音
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    private CameraCaptureSession cameraCaptureSessionFront, cameraCaptureSessionBack;

    private ReturnCameraData returnCameraData; // 向uniapp传递数据

    // 定义一个枚举类型来表示模式
    enum Mode {
        RECORDING,
        PHOTOGRAPH,
        DOUBLE_RECORDING
    }
    // 在 CameraPageActivity 类中添加一个成员变量来记录当前模式，默认为录像模式
    private Mode currentMode = Mode.RECORDING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);
        //隐藏标题栏
        Objects.requireNonNull(getSupportActionBar()).hide();
        Log.d("CameraPageActivity", "onCreate method called successfully 被调用");
        // 初始化 cameraExecutor
        cameraExecutor = Executors.newSingleThreadExecutor();
        // 查找并初始化 previewView
        mPreviewView = findViewById(R.id.viewFinder);
        // 在此处添加日志输出
        //outputDirectory = getOutputDirectory();

        Button recordingBtn = findViewById(R.id.recording_button); // 录像
        Button photographBtn = findViewById(R.id.photograph_button); //拍照
        Button doubleRecording = findViewById(R.id.double_recording_button);//双录

        Button switchCameraBtn = findViewById(R.id.switch_camera_button); //切换前后摄像头
        Button confirmBtn = findViewById(R.id.custom_button); // 开始or结束

        // 获取布局中的两个 PreviewView 双录

        textureView1 = findViewById(R.id.texture1);
        textureView2 = findViewById(R.id.texture2);
        // 实例化 CameraHelper 对象，并将其与 TextureView 关联
        // cameraHelperFront = new CameraHelper(this, CameraHelper.CAMERA_FRONT, textureView1);
        // cameraHelperBack = new CameraHelper(this, CameraHelper.CAMERA_BACK, textureView2);
        cameraHelper = new CameraHelper(this, CameraHelper.CAMERA_FRONT, textureView1);
        //相机预览
        mLensFacing = cameraHelper.getLensFacing();
        //实例化 返回数据
        returnCameraData = new ReturnCameraData();
        //单摄像头
        if(mLensFacing != -1) {
            textureViewCamera = new CameraHelper(this, mLensFacing, mPreviewView);
            //相机打开和关闭的回调 必须都是textureViewCamera否则变量之间有隔离
            textureViewCamera.setCameraStateCallback(new CameraHelper.CameraStateCallback() {
                @Override
                public void onCameraOpened(CameraDevice cameraDevice) {
                    CameraPageActivity.this.cameraDevice = cameraDevice;
                    //获取 surfaceTexture
                    SurfaceTexture surfaceTexture = mPreviewView.getSurfaceTexture();
                    if(surfaceTexture != null) {
                        previewSurface = new Surface(surfaceTexture);
                        videoRecorder = new VideoRecorder(cameraDevice, previewSurface);
                        Log.d("CameraOpen----", "回调触发打开操作 " + cameraDevice);
                    }
                    // 现在可以安全地使用cameraDevice进行拍照或其他操作了
                }

                @Override
                public void onCameraClosed(CameraDevice cameraDevice) {
                    // 清理相关的资源
                }
                // 其他回调方法的实现
            });
        }
        // 实例化PhotoModule对象
        photoModule = new PhotoModule(this);
        // 创建 VideoRecorder 实例
//        videoRecorder = new VideoRecorder(cameraDevice, previewSurface);
        //事件绑定 为按钮设置点击监听器
        //录像
        recordingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在这里定义按钮被点击时要执行的操作
                if (currentMode == Mode.DOUBLE_RECORDING) {
                    cameraHelper.closeCameras();
                    switchCamera(mLensFacing); // 调用切换摄像头方法
                }
                currentMode = Mode.RECORDING;
                // 设置switchCamera按钮可见
                switchCameraBtn.setVisibility(View.VISIBLE);
                mPreviewView.setVisibility(View.VISIBLE);
                //双录的视图不可见
                textureView1.setVisibility(View.GONE);
                textureView2.setVisibility(View.GONE);
                // 例如，显示一个 Toast 消息
                Toast.makeText(CameraPageActivity.this, "现在是录像模式！", Toast.LENGTH_SHORT).show();
            }
        });
        //拍照
        photographBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在这里定义按钮被点击时要执行的操作
                //当是双录时过渡过来时要初始化
                if (currentMode == Mode.DOUBLE_RECORDING) {
                    cameraHelper.closeCameras();
                    switchCamera(mLensFacing); // 调用切换摄像头方法
                }
                currentMode = Mode.PHOTOGRAPH;
                // 设置switchCamera按钮可见
                switchCameraBtn.setVisibility(View.VISIBLE);
                mPreviewView.setVisibility(View.VISIBLE);
                //双录的视图不可见
                textureView1.setVisibility(View.GONE);
                textureView2.setVisibility(View.GONE);
                // 例如，显示一个 Toast 消息
                Toast.makeText(CameraPageActivity.this, "现在是拍照模式！", Toast.LENGTH_SHORT).show();
            }
        });
        //双录
        doubleRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在这里定义按钮被点击时要执行的操作
                currentMode = Mode.DOUBLE_RECORDING;
                // 设置switchCamera按钮不可见
                switchCameraBtn.setVisibility(View.GONE);
                mPreviewView.setVisibility(View.GONE);
                //显示双录的视图
                textureView1.setVisibility(View.VISIBLE);
                textureView2.setVisibility(View.VISIBLE);
                //启动前置和后置摄像头预览
                dMap =cameraHelper.openDualCameras(textureView1,textureView2);
                // 例如，显示一个 Toast 消息
                Toast.makeText(CameraPageActivity.this, "现在是双录模式！", Toast.LENGTH_SHORT).show();
            }
        });
        //start or end
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在这里定义按钮被点击时要执行的操作
                String message = "";
                String confirmType = "start";
                //录制或拍照
                if(currentMode == Mode.RECORDING || currentMode == Mode.PHOTOGRAPH ) {
                    String cameraType = ( mLensFacing == CameraCharacteristics.LENS_FACING_BACK) ? "后置摄像头" : "前置摄像头";
                    message = cameraType + (currentMode == Mode.RECORDING ? "录制点击" : "拍照点击");
                    // 检查当前模式
                    if(currentMode == Mode.RECORDING) {
                        Log.d("CameraOpen----", "videoRecorder--录制模式 " + currentMode);
                        if(videoRecorder.isRecording()) {
                            Log.d("CameraOpen----", "结束录制 start " );
                            // 调用 stopRecording 方法停止录制视频
                            videoRecorder.stopRecording();
                            Log.d("CameraOpen----", "videoRecorder--结束录制 " + videoFile);
                            returnCameraData.androidCameraReturn("recorder", videoFile);
                            Log.d("CameraOpen----", "videoRecorder--结束录制01 " + videoFile);
                            // 解绑
                            textureViewCamera.closeCameras();
                            recordingBtn.setVisibility(View.VISIBLE);
                            photographBtn.setVisibility(View.VISIBLE);
                            doubleRecording.setVisibility(View.VISIBLE);
                            //录制结合后信息要传递 关闭原生页面
                            finish();

                        }else{
                            recordingBtn.setVisibility(View.GONE);
                            photographBtn.setVisibility(View.GONE);
                            doubleRecording.setVisibility(View.GONE);
                            switchCameraBtn.setVisibility(View.GONE);
                            // 如果未在录制，则开始录制
                            videoFile = getVideoFile("video"); // 获取用于保存视频的文件对象
                            // 开始录制视频
                            Log.d("CameraOpen----", "videoRecorder--开始录制 " + outputDirectory);
                            String direction = mLensFacing == CameraCharacteristics.LENS_FACING_BACK ? "back" : "front";
                            videoRecorder.startRecording(videoFile,direction);
                            Toast.makeText(CameraPageActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
                        }

                    }else{
                        // 调用拍摄照片方法
                        //cameraHelper.getCurrentCameraDevice(); // 获取当前打开的相机设备
                        // 检查摄像头设备是否可用
                        if (cameraDevice != null) {
                            recordingBtn.setVisibility(View.GONE);
                            photographBtn.setVisibility(View.GONE);
                            doubleRecording.setVisibility(View.GONE);
                            // 调用拍摄照片方法，拍照并获取图片数据
                            photoModule.takePictureAndReturn(cameraDevice, new PhotoModule.ImageAvailableListener() {
                                @Override
                                public void onImageAvailable(ByteBuffer imageData) {
                                    // 在这里处理返回的图片数据，比如将它转换成Bitmap
                                    byte[] bytes = new byte[imageData.remaining()];
                                    imageData.get(bytes);
                                    final Bitmap imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    // 创建一个临时文件来保存JPEG数据
                                    File jpegFile = new File(getExternalFilesDir(null), "tempImage.jpg");
                                    try (FileOutputStream out = new FileOutputStream(jpegFile)) {
                                        // 压缩并写入文件
                                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // 100表示最高质量
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    returnCameraData.androidCameraReturn("image", jpegFile);
                                    finish();
                                    // 更新UI等操作需要在主线程中执行
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 显示Bitmap或者处理图像...
                                            // 例如显示到一个ImageView上
                                            // imageView.setImageBitmap(imageBitmap);
                                            Toast.makeText(CameraPageActivity.this, "以获取到照片！", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                            message = "拍照已触发，请等待...";
                        } else {
                            recordingBtn.setVisibility(View.VISIBLE);
                            photographBtn.setVisibility(View.VISIBLE);
                            doubleRecording.setVisibility(View.VISIBLE);
                            message = "摄像头未准备好，请稍候...";
                        }

                    }
                }
                //双录
                if(currentMode == Mode.DOUBLE_RECORDING) {
                    message = "双录模式点击";
                    if(videoRecorder.isRecording()) {
                        String videoPrefix = "mergedVideo"; // 视频文件名前缀
                        String videoExtension = ".mp4"; // 视频文件扩展名
                        String publicUrl = ReturnCameraData.getPublicStorageUrl(); //获取储存的公共路径
                        String videoFilePath = returnCameraData.generateUniqueFileName(publicUrl, videoPrefix, videoExtension); //生成文件名
                        returnCameraData.createVideoFile(videoFilePath); //创建文件
                        Log.d("CameraOpen----", "合并文件url******" + videoFilePath);
                        videoFile = new File(videoFilePath);
                        //videoFile = getVideoFile("mergedVideo"); // 获取用于保存合成视频的文件对象
                        // 调用 stopRecording 方法停止录制视频
                        videoRecorder.stopDoubleRecording();
                        //String fileUrl = getVideoFilePath("videoFile");
                        //获取合成的视频文件
                        FFmpegModule.mergeVideos(videoFile1.getAbsolutePath(), videoFile2.getAbsolutePath(), videoFile.getAbsolutePath(),CameraPageActivity.this,  new FFmpegModule.MergeListener(){
                            @Override
                            public void onMergeSuccess(String outputVideoPath) {
                                // 合并成功后的处理逻辑
                                System.out.println("合并成功：" + outputVideoPath);
                                Log.d("CameraOpen----", "55555555" + videoFile);
                                returnCameraData.androidCameraReturn("doubleRecording", videoFile);
                                recordingBtn.setVisibility(View.VISIBLE);
                                photographBtn.setVisibility(View.VISIBLE);
                                doubleRecording.setVisibility(View.VISIBLE);
                                finish();
                            }
                            @Override
                            public void onMergeFailure(String errorMessage) {
                                // 合并失败后的处理逻辑
                                System.out.println("合并失败：" + errorMessage);
                                Toast.makeText(CameraPageActivity.this, "视频合成失败"+errorMessage, Toast.LENGTH_SHORT).show();
                                recordingBtn.setVisibility(View.VISIBLE);
                                photographBtn.setVisibility(View.VISIBLE);
                                doubleRecording.setVisibility(View.VISIBLE);
                                finish();
                            }
                        });

                    }else{
                        recordingBtn.setVisibility(View.GONE);
                        photographBtn.setVisibility(View.GONE);
                        doubleRecording.setVisibility(View.GONE);
                        // 如果未在录制，则开始录制
                        videoFile1 = getVideoFile("videoFile1"); // 获取用于保存视频的文件对象
                        videoFile2 = getVideoFile("videoFile2"); // 获取用于保存视频的文件对象
                        // 开始录制视频
                        Log.d("CameraOpen----", "双录--开始录制 " + outputDirectory);
                        videoRecorder.startFrontCameraDeviceRecording(videoFile1,(CameraDevice)dMap.get("device1"),textureView1,(CameraCaptureSession)dMap.get("mPreviewSession1"));
                        videoRecorder.startBackCameraDeviceRecording(videoFile2,(CameraDevice)dMap.get("device2"),textureView2,(CameraCaptureSession)dMap.get("mPreviewSession2"));
                        Log.d("CameraOpen----", "执行录制完成------end" + outputDirectory);
                        Toast.makeText(CameraPageActivity.this, "开始录制---全部执行", Toast.LENGTH_SHORT).show();
                    }
                }
                // 例如，显示一个 Toast 消息
                Toast.makeText(CameraPageActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        //切换前后摄像头
        switchCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在这里定义按钮被点击时要执行的操作
                switchCamera(null); // 调用切换摄像头方法
                String message = ( mLensFacing == CameraCharacteristics.LENS_FACING_BACK) ? "切换前置摄像头" : "切换后置摄像头";
                // 例如，显示一个 Toast 消息
                Toast.makeText(CameraPageActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 检查相机权限
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            );
        }

    }
    private String getVideoFilePath(String suffix) {
        return new File(Environment.getExternalStorageDirectory(), suffix).getAbsolutePath();
    }
    // 初始化相机
    private void startCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        // 使用CameraManager打开相机
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            // 检查权限后打开相机
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                //有摄像头
                if(mLensFacing != -1) {
                    //打开相机并预览
                    textureViewCamera.openCameras();
                    //记录设备
                    //cameraDevice = cameraHelper.getCurrentCameraDevice();
                }
            }else{
                ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                );
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "开启相机失败", e);
        }
    }
    // 切换摄像头方法
    private void switchCamera(@Nullable Integer lensFacing) {
        // 如果当前是后置摄像头，则切换到前置摄像头；如果当前是前置摄像头，则切换到后置摄像头
        if (lensFacing != null) {
            mLensFacing = lensFacing;
        }else{
            mLensFacing = textureViewCamera.switchLensType(mLensFacing);
        }
        if(textureViewCamera != null ){
            // 解绑旧的预览用例
            textureViewCamera.closeCameras();
            //打开相机并预览 设置朝向
            textureViewCamera.switchCamera(mLensFacing);
        }

    }

    // 检查是否授予所需权限
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    // 获取输出目录
//    private File getOutputDirectory() {
//        File mediaDir = new File(getExternalMediaDirs()[0], getResources().getString(R.string.app_name));
//        mediaDir.mkdirs();
//        return mediaDir;
//    }
    // 处理权限请求响应
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // 用户拒绝了权限请求，但是未勾选“不再询问”，可以再次请求权限
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                } else {
                    // 用户拒绝了权限请求，并且勾选了“不再询问”，引导用户手动授权
                    showPermissionDeniedDialog();
                }
//                Toast.makeText(this, "用户未授予权限。", Toast.LENGTH_SHORT).show();
//                // 回到上一个会话
//                finish();
            }
        }
    }

    // 显示权限被拒绝对话框，引导用户手动授权
    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("权限被拒绝");
        builder.setMessage("您已拒绝相机权限，需要手动授权才能继续使用应用。是否前往设置界面授权？");
        builder.setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(CameraPageActivity.this, "某些功能可能无法正常使用，请手动授权相机权限以继续。", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }
    // 获取用于保存视频的文件对象
    private File getVideoFile(String video) {
        // 指定存储目录，您可以根据实际情况更改目录
        File storageDir = new File(getExternalFilesDir(null), "Videos");
        // 如果目录不存在，则创建目录
        if (!storageDir.exists()) {
            if(!storageDir.mkdirs()) {
                Log.d("CameraOpen", "目录创建结果: 失败" );
            };
        }
        // 创建一个唯一的文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = video+"VIDEO_" + timeStamp + ".mp4";
        // 创建文件对象并返回
        return new File(storageDir, videoFileName);
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoRecorder.stopDoubleRecording();
        videoRecorder.stopRecording();
    }

    @Override
    protected void onDestroy() {
        //离开页面释放资源
        super.onDestroy();
        cameraExecutor.shutdown();
    }

}
