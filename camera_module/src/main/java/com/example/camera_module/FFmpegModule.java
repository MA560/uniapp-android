package com.example.camera_module;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import java.io.File;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

public class FFmpegModule {

    public interface MergeListener {
        void onMergeSuccess(String outputVideoPath);
        void onMergeFailure(String errorMessage);
    }
    public static void mergeVideos(String inputVideo1, String inputVideo2, String outputVideo, Context context,MergeListener listener) {
        // 创建临时文件路径
        String tempOutputVideo = context.getExternalCacheDir() + File.separator + "temp_video.mp4";
        Toast.makeText(context, "视频合成中,请等待...", Toast.LENGTH_SHORT).show();
        // 构建 FFmpeg 命令
        //String[] cmd = new String[]{"-y","-i", inputVideo1, "-i", inputVideo2, "-filter_complex", "[0:v][1:v]vstack=inputs=2[v]", "-map", "[v]", tempOutputVideo};
        //String[] cmd = new String[]{"-y", "-i", inputVideo1, "-i", inputVideo2, "-filter_complex", "[0:v][1:v]vstack=inputs=2[v]", "-map", "[v]", "-r", "30", "-time_base", "1/90000", tempOutputVideo};
        String[] cmd = new String[]{"-y", "-i", inputVideo1, "-i", inputVideo2, "-filter_complex", "[0:v][1:v]vstack=inputs=2[v]", "-map", "[v]", "-r", "29.97", "-vsync", "vfr", tempOutputVideo};
        //命令
        int rc = FFmpeg.execute(cmd);
        //日志
        String outputLog = Config.getLastCommandOutput();
        try {
            Log.d("CameraOpen----", "开始合成" );
            // 执行 FFmpeg 命令
            if (rc == 0) {
                // 如果合成成功，将临时输出视频复制到最终输出视频路径
                File tempFile = new File(tempOutputVideo);
                File outputFile = new File(outputVideo);
                if (tempFile.exists() && tempFile.isFile()) {
                    tempFile.renameTo(outputFile);
                }
                if (listener != null) {
                    listener.onMergeSuccess(tempOutputVideo);
                }
            }else {
                if (listener != null) {
                    listener.onMergeFailure("Merge failed with error code: " + outputLog);
                }
            };
        } catch (Exception e) {
            // 捕获其他异常
            e.printStackTrace();
            Log.d("CameraOpen----", "合成失败" +e.getMessage());
            // 执行过程中出现异常也调用失败回调方法
            if (listener != null) {
                listener.onMergeFailure(e.getMessage());
            }
    }
    }

}
