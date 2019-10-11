package top.itser.updateapp;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class DownloadUtil {
    private static final String TAG = "DownloadUtil";
    public static final String SERVER_URL = "http://111.198.24.33:6119/";//53服务器
    private static String realPath;
    private static String tempPath;
    public static String myCacheDirPath; //存放安装包的缓存路径，getCacheDir().getAbsolutePath() + "/apks"

    public static void download(String url, final String path, final DownloadListener downloadListener) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_URL)
                //通过线程池获取一个线程，指定callback在子线程中运行。
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .build();

        ApiService service = retrofit.create(ApiService.class);

        Call<ResponseBody> call = service.download(url);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull final Response<ResponseBody> response) {
                //将Response写入到从磁盘中，详见下面分析
                //注意，这个方法是运行在子线程中的
                writeResponseToDisk(path, response, downloadListener);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                downloadListener.onFail("网络错误～");
            }
        });
    }

    private static void writeResponseToDisk(String path, Response<ResponseBody> response, DownloadListener downloadListener) {
        //从response获取输入流以及总大小
        realPath =path;
        tempPath =path;
        writeFileFromIS(new File(path), response.body().byteStream(), response.body().contentLength(), downloadListener);
    }

    private static int sBufferSize = 8192;

    //将输入流写入文件
    private static void writeFileFromIS(File file, InputStream is, long totalLength, DownloadListener downloadListener) {
        //开始下载
        downloadListener.onStart();

        //创建文件
        if (!file.exists()) {
            //需要下载安装包
            //清楚缓存
            boolean delFlag = FileUtils.deleteFiles(myCacheDirPath);
            if (delFlag==false)
            {
                Log.e(TAG, "writeFileFromIS: 清除缓存失败" );
            }

            if (!file.getParentFile().exists())
                file.getParentFile().mkdir();
            try {
                //如果没有已下载好的安装包，则需要临时下载，临时的包名加入后缀suffix，下载完成后重命名为真实名称
                String suffix ="_"+UUID.randomUUID().toString().substring(0,4);
                tempPath = tempPath +suffix;
                file = new File(tempPath);
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                downloadListener.onFail("createNewFile IOException");
            }
        }else {
            //已经有下载完成的
            downloadListener.onFinish(file.getAbsolutePath());
            Log.i("DownloadUtil", "writeFileFromIS: 安装包已存在");
            return;
        }

        OutputStream os = null;
        long currentLength = 0;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            byte data[] = new byte[sBufferSize];
            int len;
            int lastProgress = 0;
            while ((len = is.read(data, 0, sBufferSize)) != -1) {
                os.write(data, 0, len);
                currentLength += len;
                //计算当前下载进度
                int progress =(int) (100 * currentLength / totalLength);
                if (progress -lastProgress>=1)
                {
                    downloadListener.onProgress(progress);
                    lastProgress =progress;
                }

            }
            //下载完成，重命名为实际文件名称
            boolean b = FileUtils.renameFile(file.getAbsolutePath(), realPath);
            if (b){
                // 并返回保存的文件路径
                downloadListener.onFinish(realPath);
            }else {
                Log.e(TAG, "writeFileFromIS: 文件重命名失败" );
                downloadListener.onFail("文件重命名失败");
            }

        } catch (IOException e) {
            e.printStackTrace();
            downloadListener.onFail("IOException");
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




}
