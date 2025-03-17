package com.example.camera_module;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import io.dcloud.feature.uniapp.AbsSDKInstance;

public class FunModule extends UniModule {

    String TAG = "FunModule";
    public static int REQUEST_CODE = 1000;
    // 全局变量
    private static AbsSDKInstance mUniSDKInstanceNew;
    // 方法来设置mUniSDKInstance
    public static void setUniSDKInstance(AbsSDKInstance sdkInstance) {
        mUniSDKInstanceNew = sdkInstance;
    }
    public static AbsSDKInstance getUniSDKInstance() {
        return mUniSDKInstanceNew;
    }
    //run ui thread
    @UniJSMethod(uiThread = true)
    public void testAsyncFunc(JSONObject options, UniJSCallback callback) {
        Log.e(TAG, "testAsyncFunc--"+options);
        if(callback != null) {
            JSONObject data = new JSONObject();
            data.put("code", "success");
            callback.invoke(data);
            //callback.invokeAndKeepAlive(data);
        }
    }

    //run JS thread
    @UniJSMethod (uiThread = false)
    public JSONObject testSyncFunc(){
        JSONObject data = new JSONObject();
        data.put("code", "success");
        return data;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE && data.hasExtra("respond")) {
            Log.e("TestModule", "原生页面返回----"+data.getStringExtra("respond"));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
        // 跳转到 CameraPageActivity
        @UniJSMethod(uiThread = true)
        public void gotoCameraPage() {
//            if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
//                Activity contextActivity = (Activity) mUniSDKInstance.getContext();
//                Intent intent = new Intent(contextActivity, CameraPageActivity.class);
//                contextActivity.startActivity(intent);
//             }
            FunModule.setUniSDKInstance(mUniSDKInstance);
            if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
                // 设置AbsSDKInstance
                Intent intent = new Intent(mUniSDKInstance.getContext(), CameraPageActivity.class);
                ((Activity)mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE);
            }
        }

}
