package dev.local.qingtiao;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;

/** Public GitHub Release metadata and verification of the matching, signed APK. */
final class UpdateRelease {
    static final class Problem extends IOException {
        Problem(String description){super(description);}
    }
    final String version;
    private final URL download;
    private final long size;
    private final String digest;

    private UpdateRelease(String version,URL download,long size,String digest) {
        this.version=version;this.download=download;this.size=size;this.digest=digest;
    }

    static UpdateRelease findNewer(String installed) throws Exception {
        ByteArrayOutputStream metadata=new ByteArrayOutputStream();
        transfer(new URL(UpdateReleasePolicy.LATEST),metadata,512*1024,false);
        JSONObject release=new JSONObject(new String(metadata.toByteArray(),StandardCharsets.UTF_8));
        String version=UpdateReleasePolicy.versionOfTag(release.getString("tag_name"));
        if(version==null||release.optBoolean("draft")||release.optBoolean("prerelease"))
            throw new Problem("发布版本格式有误");
        if(!UpdateReleasePolicy.newer(version,installed))return null;
        JSONArray assets=release.getJSONArray("assets");
        for(int i=0;i<assets.length();i++){
            JSONObject asset=assets.getJSONObject(i);
            String name=asset.optString("name"),url=asset.optString("browser_download_url");
            long size=asset.optLong("size");
            if("uploaded".equals(asset.optString("state"))&&UpdateReleasePolicy.matchesAsset(version,name,url,size)){
                String digest=asset.isNull("digest")?"":asset.optString("digest");
                if(!digest.isEmpty()&&!digest.matches("sha256:[0-9a-f]{64}"))throw new Problem("安装包校验值格式有误");
                return new UpdateRelease(version,new URL(url),size,digest);
            }
        }
        throw new Problem("新版 APK 暂未发布");
    }

    void downloadVerified(Context context) throws Exception {
        File partial=File.createTempFile("qingtiao-update-",".apk",context.getCacheDir());
        try {
            MessageDigest sha=MessageDigest.getInstance("SHA-256");
            try(DigestOutputStream out=new DigestOutputStream(new FileOutputStream(partial),sha)){
                if(transfer(download,out,size,true)!=size)throw new Problem("安装包下载不完整");
            }
            if(!digest.isEmpty()&&!digest.equals("sha256:"+hex(sha.digest())))throw new Problem("安装包校验值不符");
            verifyApk(context,partial);
            Files.move(partial.toPath(),UpdateFileProvider.apk(context).toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } finally {
            partial.delete();
        }
    }

    private void verifyApk(Context context,File apk)throws PackageManager.NameNotFoundException,IOException {
        PackageManager packages=context.getPackageManager();
        int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo installed=packages.getPackageInfo(context.getPackageName(),flags);
        PackageInfo offered=packages.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
        if(offered==null||!installed.packageName.equals(offered.packageName)||!version.equals(offered.versionName)
            ||versionCode(offered)<=versionCode(installed))throw new Problem("安装包的应用或版本不符");
        Signature[] actual=signers(offered),expected=signers(installed);
        if(actual==null||expected==null||actual.length==0||actual.length!=expected.length
            ||!new HashSet<>(Arrays.asList(actual)).equals(new HashSet<>(Arrays.asList(expected))))
            throw new Problem("安装包签名不符");
    }

    private static long versionCode(PackageInfo info) {
        return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
    }

    private static Signature[] signers(PackageInfo info) {
        if(Build.VERSION.SDK_INT<28)return info.signatures;
        return info.signingInfo!=null?info.signingInfo.getApkContentsSigners():null;
    }

    private static long transfer(URL url,OutputStream out,long limit,boolean binary)throws IOException {
        long deadline=SystemClock.elapsedRealtime()+120000;
        for(int hop=0;hop<5;hop++){
            if(!trusted(url,binary))throw new IOException("下载地址无效");
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept",binary?"application/octet-stream":"application/vnd.github+json");
            connection.setRequestProperty("User-Agent","QingTiao-Android");
            try {
                int status=connection.getResponseCode();
                if(status==301||status==302||status==303||status==307||status==308){
                    String next=connection.getHeaderField("Location");
                    if(!binary||next==null||SystemClock.elapsedRealtime()>deadline)throw new IOException("下载重定向无效");
                    url=new URL(url,next);continue;
                }
                if(status!=200)throw new Problem(status==403||status==429?"GitHub 暂时限制访问":"GitHub 返回错误 "+status);
                try(InputStream in=connection.getInputStream()){
                    byte[] buffer=new byte[16384];
                    long total=0;
                    for(int count;(count=in.read(buffer))!=-1;){
                        total+=count;
                        if(total>limit)throw new IOException("下载文件大小不符");
                        if(SystemClock.elapsedRealtime()>deadline)throw new IOException("下载超时");
                        out.write(buffer,0,count);
                    }
                    return total;
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("下载重定向次数过多");
    }

    private static boolean trusted(URL url,boolean binary) {
        if(!"https".equals(url.getProtocol())||url.getUserInfo()!=null||(url.getPort()!=-1&&url.getPort()!=443))return false;
        String host=url.getHost();
        return binary?host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com")
            :host.equals("api.github.com");
    }

    private static String hex(byte[] bytes) {
        StringBuilder result=new StringBuilder(bytes.length*2);
        for(byte value:bytes)result.append(Character.forDigit((value&255)>>>4,16)).append(Character.forDigit(value&15,16));
        return result.toString();
    }
}
