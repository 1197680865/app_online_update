package top.itser.updateapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    protected ApiService apiService = null;
    public static final String SERVER_URL = "http://111.198.24.33:6119/";//53服务器
    private TextView tvVersion,tvProgress;
    private boolean firstOpen = true;
    private AppVersionBean appVersion = null;
    private boolean appDownloadCompelte = false;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 1001:
                    Toast.makeText(getApplicationContext(), "开始下载", Toast.LENGTH_SHORT).show();
                    break;
                case 1002:
                    int progress = message.getData().getInt("progress");
                    tvProgress.setText(String.format("下载进度：%d",progress));
                    Toast.makeText(getApplicationContext(), "下载进度:" + progress, Toast.LENGTH_SHORT).show();
                    break;
                case 1003:
                    Toast.makeText(getApplicationContext(), "下载完成", Toast.LENGTH_SHORT).show();
                    appDownloadCompelte = true;
                    String path = message.getData().getString("path");
                    if (path != null) {
                        File tempFile = new File(path);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Uri uri = FileProvider.getUriForFile(getApplicationContext(), "top.itser.updateapp.fileprovider", tempFile);
                            Log.i(TAG, "uri: " + uri.toString());
                            intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        } else {
                            intent.setDataAndType(Uri.fromFile(tempFile), "application/vnd.android.package-archive");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        if (getApplication().getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                            getApplication().startActivity(intent);
                        }

                    }
                    break;
                case 1004:
                    Toast.makeText(getApplicationContext(), "下载失败", Toast.LENGTH_SHORT).show();
                    appDownloadCompelte = true;
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvVersion = findViewById(R.id.tv1);
        tvProgress = findViewById(R.id.tv2);
        String localVersionName = AppVersionUtils.getLocalVersionName(this);
        tvVersion.setText(localVersionName);
        Retrofit retrofit =
                new Retrofit.Builder().baseUrl(SERVER_URL)
                        .addConverterFactory(GsonConverterFactory.create()).build();
        apiService = retrofit.create(ApiService.class);
        if (firstOpen) {
            getLatestVersionInfo();

            firstOpen = false;
        }
    }

    private void checkUpdate() {
        if (appVersion == null) return;
        final String versionName = appVersion.getVersionName();
        final String apkUrl = appVersion.getApkUrl();
        String localVersionName = AppVersionUtils.getLocalVersionName(getApplicationContext());
        //1. 比较版本
        if (!localVersionName.equals(versionName)) {
            //2.弹窗确认
            AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainActivity.this);
            normalDialog.setTitle("版本更新");
            normalDialog.setMessage("发现新版本：" + versionName + ",是否更新？");
            normalDialog.setPositiveButton("更新",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //3.根据apkUrl下载
                            downloadApk(apkUrl, versionName);
                        }
                    });
            normalDialog.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            // 显示
            normalDialog.show();

        }


    }

    private void getLatestVersionInfo() {

        Call<AppVersionBean> call = apiService.getLatestVersion();
        if (call != null) {
            call.enqueue(new Callback<AppVersionBean>() {
                @Override
                public void onResponse(Call<AppVersionBean> call, Response<AppVersionBean> response) {
                    if (response.code() == 200) {
                        AppVersionBean appVersionBean = response.body();
                        if (appVersionBean != null) {
                            appVersion = appVersionBean;
                            checkUpdate();
                        } else {
                            Log.e(TAG, "获取最新版本失败");
                        }
                    } else {
                        Log.e(TAG, "获取最新版本失败" + response.errorBody().toString());
                    }
                }

                @Override
                public void onFailure(Call<AppVersionBean> call, Throwable t) {
                    Log.e(TAG, "获取最新版本失败，检查网络连接");
                }
            });
        }

    }

    /**
     * 使用Retrofit下载apk
     *
     * @param url
     */
    private void downloadApk(String url, String versionName) {

        new MaterialDialog.Builder(this)
                .title("更新")
                .content("正在下载安装包...")
                .contentGravity(GravityEnum.CENTER)
                .progress(false, 100, false)
                .cancelListener(dialog -> {
                    //取消
                    if (!appDownloadCompelte) {
                        Toast.makeText(getApplicationContext(), "安装包后台下载中...", Toast.LENGTH_LONG).show();
                    }
                }).showListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                final MaterialDialog dialog = (MaterialDialog) dialogInterface;
                DownloadUtil.myCacheDirPath = MainActivity.this.getCacheDir().getAbsolutePath() + "/apks/";
                DownloadUtil.download(url, MainActivity.this.getCacheDir().getAbsolutePath() + "/apks/maas-app-" + versionName + ".apk", new DownloadListener() {
                    @Override
                    public void onStart() {

                        Message message = new Message();
                        message.what = 1001;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onProgress(int progress) {
                    Message message = new Message();
                    message.what = 1002;
                    Bundle bundle = new Bundle();
                    bundle.putInt("progress", progress);
                    message.setData(bundle);
                    handler.sendMessage(message);
                        dialog.setProgress(progress);

                    }

                    @Override
                    public void onFinish(String path) {

                        Message message = new Message();
                        message.what = 1003;
                        Bundle bundle = new Bundle();
                        bundle.putString("path", path);
                        message.setData(bundle);
                        handler.sendMessage(message);
                        runOnUiThread(() -> {
                            dialog.setContent("下载完成");
                        });
                    }

                    @Override
                    public void onFail(String errorInfo) {

                        Message message = new Message();
                        message.what = 1004;
                        handler.sendMessage(message);
                        runOnUiThread(() -> {
                            dialog.setContent("下载失败，请重试");
                        });
                    }
                });
            }
        }).show();


    }
}
