package dev.local.qingtiao;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pure Java matching policy; no UI action is performed here. */
public final class MatchPolicy {
    /** Ad screenshots and pasted diagnostics inside a conversation are content, not live controls. */
    public static boolean diagnosticHost(String pkg){return "com.openai.chatgpt".equals(pkg);}
    private static final Pattern SKIP = Pattern.compile(
        "^(?:(?:跳过|跳過)(?:广告|廣告)?(?:[0-9]{1,2}[sS秒]?)?|[0-9]{1,2}[sS秒]?(?:跳过|跳過)(?:广告|廣告)?|(?i:skip(?:ad)?(?:[0-9]{1,2}s?)?|[0-9]{1,2}s?skip(?:ad)?))$");
    private static final String[] SDK_SKIP = {
        "tt_splash_skip_btn", "tt_splash_skip_btn_container", "gdt_skip_button",
        "ksad_splash_skip_view", "ksad_splash_skip_right_view", "mbridge_splash_tv_skip",
        "anythink_splash_tv_skip", "baidu_splash_skip", "splash_ad_skip"
    };
    private static final Pattern COUNTDOWN=Pattern.compile("[0-9]{1,2}[sS秒]?");
    private static final Pattern AD_JUMP=Pattern.compile("点击跳转详情(?:页面)?[>›»→]?");
    // Reuse the exact platform regex so Android's own whitespace semantics stay unchanged.
    private static final Pattern NORMALIZE=Pattern.compile("[\\s\\u00a0（）()｜|·:：]");
    public static String normalize(String text) {
        if(text==null)return "";
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(c<=' '||c==':'||c=='('||c==')'||c=='|'||c=='·'||c=='：'||c=='（'||c=='）'||c=='｜'
                ||c=='\u0085'||c=='\u180e'||Character.isWhitespace(c)||Character.isSpaceChar(c)){
                // Keep the platform matcher authoritative for separators and trimming.
                return NORMALIZE.matcher(text).replaceAll("").trim();
            }
        }
        return text;
    }
    public static boolean isSkipText(String text) {
        return SKIP.matcher(normalize(text)).matches();
    }
    public static boolean explicitAdSkip(String text) {
        String s=normalize(text);
        return isSkipText(s)&&(s.contains("广告")||s.contains("廣告")||s.toLowerCase(Locale.ROOT).contains("ad"));
    }
    public static boolean isSdkSkip(String id) {
        if (id == null) return false;
        String shortId = id.substring(id.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        for (String item : SDK_SKIP) if (shortId.equals(item)) return true;
        return false;
    }
    public static boolean isCountdownText(String text) {
        return COUNTDOWN.matcher(normalize(text)).matches();
    }
    public static boolean isCountdownId(String id) {
        if(id==null)return false;
        String s=id.substring(id.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
        return s.contains("count_down")||s.contains("countdown");
    }
    public static boolean adSpecificId(String id) {
        if(id==null)return false;
        String s=id.substring(id.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
        return s.contains("splash")||s.startsWith("ad_")||s.contains("_ad_")||s.endsWith("_ad");
    }
    public static boolean preferGesture(String pkg,String text,String desc,String id,boolean selfTest,boolean gestureAllowed) {
        if(selfTest||!gestureAllowed)return false;
        if(!"com.tencent.qqmusic".equals(pkg)&&!"com.autonavi.minimap".equals(pkg)&&!"air.tv.douyu.android".equals(pkg))return false;
        return isSkipText(text)||isSkipText(desc)||isSdkSkip(id)
            ||(isCountdownId(id)&&(isCountdownText(text)||isCountdownText(desc)));
    }
    /** Native actions for this app are unsafe; only its verified screenshot path may act. */
    public static boolean requiresVisualSkip(String pkg){return "com.weilaishanhai.oopz".equals(pkg);}
    public static boolean isAdEvidence(String text, String id) {
        return isAdLabel(text)||isAdId(id);
    }
    static boolean isAdEvidence(String text,String description,String id){
        return isAdLabel(text)||isAdLabel(description)||isAdId(id);
    }
    private static boolean isAdLabel(String text){
        String value = normalize(text);
        return value.equals("广告") || value.equals("廣告") || value.equalsIgnoreCase("ad") || value.equalsIgnoreCase("advertisement")
                || value.equals("互动广告") || value.equals("互動廣告")
                || value.startsWith("点击跳转详情")&&AD_JUMP.matcher(value).matches()
                || value.startsWith("广告剩余") || value.equals("广告详情")
                || value.equals("广告由第三方提供") || value.equals("广告由第三方提供，请注意甄别");
    }
    private static boolean isAdId(String id){
        String low = id == null ? "" : id.toLowerCase(Locale.ROOT);
        return low.contains("tt_splash_ad_logo") || low.contains("gdt_ad_logo")
            || low.contains("ksad_splash_logo") || low.contains("splash_ad_logo")
            || low.contains("mbridge_splash_adchoice") || low.contains("ad_source_logo");
    }
    public static boolean plausibleBounds(int left, int top, int right, int bottom,
                                          int width, int height) {
        if (width <= 0 || height <= 0 || left < 0 || top < 0 || right > width || bottom > height) return false;
        int w = right-left, h = bottom-top;
        return w > 0 && h > 0 && w <= width * .48 && h <= height * .12
            && left >= width * .45 && (bottom <= height * .35 || top >= height * .72);
    }
    public static boolean eligible(String text, String id, boolean adEvidence, boolean custom,
                                   int left, int top, int right, int bottom, int width, int height) {
        return plausibleBounds(left,top,right,bottom,width,height)
            && (isSdkSkip(id) || (adEvidence && (custom || isSkipText(text))));
    }
    private MatchPolicy() {}
}
