package top.itser.updateapp;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface ApiService {
    /**
     * 查询最新的版本号
     */
    @GET("app/latest_version")
    Call<AppVersionBean> getLatestVersion();

    @Streaming  //使用@Streaming的主要作用就是把实时下载的字节就立马写入磁盘，而不用把整个文件读入内存
    @GET
    Call<ResponseBody> download(@Url String url);
}
