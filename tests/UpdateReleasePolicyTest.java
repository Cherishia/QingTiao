package dev.local.qingtiao;

public final class UpdateReleasePolicyTest {
    private static int checks;
    private static void check(boolean passed,String message){
        checks++;
        if(!passed)throw new AssertionError(message);
    }
    public static void main(String[] args){
        check("0.5.40".equals(UpdateReleasePolicy.versionOfTag("v0.5.40")),"parse a stable release tag");
        for(String unsupported:new String[]{null,"","v0.5.40-beta.1","v0.5.040","v0.5.40/other","v0.5.1234567","0.5.40"})
            check(UpdateReleasePolicy.versionOfTag(unsupported)==null,"reject unsupported tag "+unsupported);
        check(UpdateReleasePolicy.newer("0.5.40","0.5.39"),"offer a newer application");
        check(UpdateReleasePolicy.newer("0.5.100","0.5.99"),"compare version components numerically");
        check(UpdateReleasePolicy.newer("1.0.0","0.50.60"),"major version wins");
        check(!UpdateReleasePolicy.newer("0.5.39","0.5.40")&&!UpdateReleasePolicy.newer("0.5.40","0.5.40"),"no downgrade or same-version install");
        check(!UpdateReleasePolicy.newer(null,"0.5.40")&&!UpdateReleasePolicy.newer("1.0.0","0.5.40-test"),"invalid versions cannot trigger download");
        String name="QingTiao-0.5.40.apk",url="https://github.com/Cherishia/QingTiao/releases/download/v0.5.40/"+name;
        check(UpdateReleasePolicy.matchesAsset("0.5.40",name,url,1352586),"matching APK asset on the official release");
        check(!UpdateReleasePolicy.matchesAsset("0.5.41",name,url,1352586),"old asset cannot masquerade as the new release");
        check(!UpdateReleasePolicy.matchesAsset("0.5.40","QingTiao-0.5.40-source.zip",url,1352586),"source archive never sent for installation");
        for(String invalid:new String[]{url.replace("https:","http:"),url.replace("github.com","github.com.other.test"),
            url.replace("Cherishia/","someone-else/"),url+"?download=1"})
            check(!UpdateReleasePolicy.matchesAsset("0.5.40",name,invalid,1352586),"only the exact release asset is accepted");
        check(!UpdateReleasePolicy.matchesAsset("0.5.40",name,url,0)
            &&!UpdateReleasePolicy.matchesAsset("0.5.40",name,url,UpdateReleasePolicy.MAX_APK_BYTES+1),"bound download size");
        System.out.println("PASS: "+checks+" release update policy checks");
    }
}
