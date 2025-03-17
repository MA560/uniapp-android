package com.example.camera_module;

import android.hardware.camera2.CameraAccessException;
import android.os.Build;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.io.File;

import io.dcloud.feature.uniapp.AbsSDKInstance;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.common.UniModule;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Base64;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.FileOutputStream;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class ReturnCameraData extends UniModule {
    String TAG = "ReturnCameraData";
    public static int REQUEST_CODE = 1000;
    private final static AbsSDKInstance mUniSDKInstanceNew = FunModule.getUniSDKInstance();

    public final Context context = mUniSDKInstanceNew.getContext();
    public static String folderName = "CameraModuleDownload";

    public Build build;
    public ActivityCompat activityCompat;

    public static final int REQUEST_PERMISSION_CODE = 123; // 你可以使用任何整数值作为请求码

    // 定义媒体类型枚举
    //结束业务回传camera数据 //将数据传递给uniapp
    @UniJSMethod (uiThread = true)
    public void androidCameraReturn(String type, File file) {
        if(file != null) {
            Log.d("CameraOpen----", "发送开始 "+file );
                //发送信息

            JSONObject data = new JSONObject();
            data.put("type", type);
            data.put("file", "file:/" + file);
            data.put("msg", "AndroidCameraReturn"); // 将你想发送的消息放入data中
    //        mUniSDKInstance.fireGlobalEvent("myEvent", data);
                if (mUniSDKInstanceNew != null) {
                    Log.d("CameraOpen----", "调用了 " + data);
                    mUniSDKInstanceNew.fireGlobalEventCallback("androidCameraReturn", data);
                    //存储附件到本地
                    //saveFile(file);
                    //String base64 = type.equals("image") ? imageToBlob(file) : videoToBlob(file);
                    //saveFileFromBase64(context,base64,file.getName());
                    //Log.d("CameraOpen----", "调用2 " + base64);
                }else{
                    Log.d("CameraOpen----", "mUniSDKInstance没有正确初始化或未获取到 " + mUniSDKInstanceNew);
                }
        }else{
            Log.d("CameraOpen----", "附件值为null ");
        }
    }

    //获取旋转方向
    // 将媒体数据转换为 Blob
    // 将图片数据转换为 Blob
    public static String imageToBlob(File imageFile) {
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] blob = outputStream.toByteArray();
        return Base64.encodeToString(blob, Base64.DEFAULT);
    }

    // 将视频数据转换为 Blob
    public static String videoToBlob(File videoFile) {
        if (videoFile == null) {
            Log.e("VideoUtils", "视频文件为空");
            return null;
        }
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
        if (thumbnail == null) {
            Log.e("VideoUtils", "无法为视频生成缩略图");
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if(!thumbnail.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
            Log.e("VideoUtils", "无法压缩缩略图");
            return null;
        }
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] blob = outputStream.toByteArray();
        return Base64.encodeToString(blob, Base64.DEFAULT);
    }

    //获取公共储存url
    @Nullable
    public static String getPublicStorageUrl(){
        // 检查外部存储是否可用
        if (!isExternalStorageWritable()) {
            Log.e("CameraOpen5", "外部存储不可用");
            return null;
        }
        final String[] fileUrl = {null};
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File folder = new File(storageDir, folderName);
        // 如果文件夹不存在，则创建文件夹
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                // 文件夹创建失败
                Log.e("getPublicStorageUrl", "文件夹创建失败: " + folder.getAbsolutePath());
                // 请求权限
                requestStoragePermission(new PermissionCallback(){
                    // 在权限被授予后再次调用 getPublicStorageUrl 方法
                    public void onPermissionGranted() {
                         createFolderIfPermissionGranted(folder);
                    }
                });
                return null;
            }
        }
        // 创建文件夹
        return folder.getAbsolutePath();
    }

    private static String createFolderIfPermissionGranted(File folder) {
        // 如果权限已经授予，则创建文件夹
        if (checkStoragePermission()) {
            if (folder.mkdirs()) {
                return folder.getAbsolutePath();
            } else {
                Log.e("getPublicStorageUrl", "文件夹创建失败: " + folder.getAbsolutePath());
                return null;
            }
        }
        return null;
    }
    //检查应用是否被授予了写外部存储的权限
    private static boolean checkStoragePermission() {
        Context context = mUniSDKInstanceNew.getContext();
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

//    public static void requestStoragePermission() {
//        Context context = mUniSDKInstanceNew.getContext();
//        // 检查是否需要请求权限
//        if (context instanceof Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            Activity activity = (Activity) context;
//            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                // 请求权限
//                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
//            }
//        } else {
//            Log.e("YourActivity", "Context is not an instance of Activity");
//        }
//
//    }

    public interface PermissionCallback {
        void onPermissionGranted();
    }
    public static void requestStoragePermission(PermissionCallback callback) {
        Context context = mUniSDKInstanceNew.getContext();

        if (context instanceof Activity ) {
            Activity activity = (Activity) context;
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 请求权限
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
            } else {
                // 已经授予了权限
                if (callback != null) {
                    callback.onPermissionGranted();
                }
            }
        } else {
            // 不需要请求权限
            Log.e("YourActivity", "Permission is granted on pre-M devices");
        }
    }

    // 将Base64编码的数据保存到文件中
    public static File saveFileFromBase64(Context context, String base64Data, String fileName) {
        try {
            String folder = getPublicStorageUrl();
            // 创建文件对象
            File file = new File(folder, fileName);
            // 创建文件输出流
            FileOutputStream fos = new FileOutputStream(file);
            // 将Base64字符串解码成字节数组并写入文件
            fos.write(Base64.decode(base64Data, Base64.DEFAULT));
            fos.close();
            Log.d("CameraOpen5", "附件保存成功: " + file.getAbsolutePath());
            Toast.makeText(context, "文件已保存到:" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return file;
        } catch (IOException e) {
            Log.e("CameraOpen5", "Failed to save file", e);
            return null;
        }
    }
    // 将文件保存到指定地址
//    private void saveFile(File file) {
//        // 获取下载文件夹路径
//        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        // 创建 folderName 文件夹
//        File folder = new File(storageDir, folderName);
//        if (!folder.exists()) {
//            if (!folder.mkdirs()) {
//                Log.e("CameraOpen5", "Failed to create directory: " + folder.getAbsolutePath());
//            }
//        }
//        // 目标保存路径
//        // 将文件保存到 CameraModule Download 文件夹中
//        String targetPath = folder.getAbsolutePath() + File.separator + file.getName();
//        File targetFile = new File(targetPath);
//        try {
//            // 复制文件
//            Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            Log.d("CameraOpen5", "文件保存成功: " + targetFile.getAbsolutePath());
//            Toast.makeText(mUniSDKInstanceNew.getContext(), "文件已保存到:" + targetFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//        } catch (IOException e) {
//            Log.e("CameraOpen5", "Failed to save file", e);
//        }
//    }
    // 检查外部存储是否可用
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    //创建文件
    public void createVideoFile(String videoFilePath) {
        File videoFile = new File(videoFilePath);
        if (!videoFile.exists()) {
            try {
                boolean created = videoFile.createNewFile();
                if (created) {
                    Log.d("CameraOpen", "视频文件创建成功: " + videoFile.getAbsolutePath());
                } else {
                    Log.e("CameraOpen", "视频文件创建失败");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("CameraOpen", "视频文件创建失败: " + e.getMessage());
            }
        } else {
            Log.d("CameraOpen", "视频文件已存在: " + videoFile.getAbsolutePath());
        }
    }
    //生成一个唯一附件名
    public String generateUniqueFileName(String videoDirectory, String videoPrefix, String videoExtension) {
        // 创建一个唯一的文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String videoFileName = videoPrefix + "_" + timeStamp + videoExtension;
        // 返回视频文件的完整路径
        return new File(videoDirectory, videoFileName).getAbsolutePath();
    }
}
