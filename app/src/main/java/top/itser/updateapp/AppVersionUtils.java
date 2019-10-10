package top.itser.updateapp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * app版本控制类
 */
public class AppVersionUtils {
    private static final String TAG = "AppVersionUtils";



    /**
     * 获取软件版本号 VersionCode
     * @param ctx
     * @return
     */
    public static int getLocalVersionCode(Context ctx) {
        int localVersionCode = 0;
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersionCode = packageInfo.versionCode;
            Log.i("AppVersionUtils", "getLocalVersionCode: " + localVersionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersionCode;
    }
    /**
     * 获取本地软件版本号名称 VersionName
     */
    public static String getLocalVersionName(Context ctx) {
        String localVersionName = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersionName = packageInfo.versionName;
            Log.i("AppVersionUtils", "getLocalVersionName: " + localVersionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersionName;
    }



}
