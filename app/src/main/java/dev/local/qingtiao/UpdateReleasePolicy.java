package dev.local.qingtiao;

import java.util.regex.Pattern;

final class UpdateReleasePolicy {
    static final String REPOSITORY="Cherishia/QingTiao";
    static final String LATEST="https://api.github.com/repos/"+REPOSITORY+"/releases/latest";
    static final long MAX_APK_BYTES=32*1024*1024;
    private static final Pattern VERSION=Pattern.compile("(?:0|[1-9][0-9]{0,5})(?:\\.(?:0|[1-9][0-9]{0,5})){2}");

    static String versionOfTag(String tag) {
        return tag!=null&&tag.startsWith("v")&&VERSION.matcher(tag.substring(1)).matches()?tag.substring(1):null;
    }

    static boolean newer(String remote,String current) {
        if(remote==null||current==null||!VERSION.matcher(remote).matches()||!VERSION.matcher(current).matches())return false;
        String[] offered=remote.split("\\."),installed=current.split("\\.");
        for(int i=0;i<offered.length;i++){
            int order=Integer.compare(Integer.parseInt(offered[i]),Integer.parseInt(installed[i]));
            if(order!=0)return order>0;
        }
        return false;
    }

    static String apkName(String version){return "QingTiao-"+version+".apk";}

    static boolean matchesAsset(String version,String name,String url,long size) {
        if(version==null||!VERSION.matcher(version).matches())return false;
        return size>0&&size<=MAX_APK_BYTES&&apkName(version).equals(name)
            &&("https://github.com/"+REPOSITORY+"/releases/download/v"+version+"/"+name).equals(url);
    }

    private UpdateReleasePolicy(){}
}
