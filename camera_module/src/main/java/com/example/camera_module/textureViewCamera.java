package com.example.camera_module;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Toast;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class textureViewCamera extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private CameraHelper CameraHelper1;
    private CameraHelper CameraHelper2;

    private TextureView textureView1;
    private TextureView textureView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview); // 假设您的布局文件名为 activity_texture_view_camera.xml

        textureView1 = findViewById(R.id.texture1); // 假设您在布局文件中定义了名为 textureView1 的 TextureView 控件
        textureView2 = findViewById(R.id.texture2); // 假设您在布局文件中定义了名为 textureView2 的 TextureView 控件

        // 检查并请求相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            // 如果已经有相机权限，设置摄像头
            setupCamera();
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 如果权限请求被授予，设置摄像头
                setupCamera();
            } else {
                // 如果权限被拒绝，显示相应提示
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // 设置摄像头
    private void setupCamera() {
        // 创建前置摄像头辅助类实例，并将其与 TextureView 关联
        CameraHelper1 = new CameraHelper(this, CameraHelper.CAMERA_FRONT, textureView1);
        // 创建后置摄像头辅助类实例，并将其与 TextureView 关联
        CameraHelper2 = new CameraHelper(this, CameraHelper.CAMERA_BACK, textureView2);
    }

    // 恢复活动时启动摄像头预览
    @Override
    protected void onResume() {
        super.onResume();
        CameraHelper1.openCameras();
        CameraHelper2.openCameras();
    }

    // 暂停活动时停止摄像头预览
    @Override
    protected void onPause() {
        super.onPause();
        CameraHelper1.closeCameras();
        CameraHelper2.closeCameras();
    }

}

