# app_online_update
app 在线下载并安装
1.下载文件：使用  retrofit2 @Streaming
2.安装apk
    android6.0以上：需要使用FileProvider(https://blog.csdn.net/chen_white/article/details/72819814) ，并加入权限
        <!--安卓8.0及以上调起apk安装程序，需要REQUEST_INSTALL_PACKAGES权限，否则会闪退，而且不会报错-->
   		 <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
