package dev.local.qingtiao;
import java.util.Random;
import java.util.regex.Pattern;
/** Frozen 0.5.14 normalization semantics, including default-Java whitespace and trim behavior. */
public final class TextPolicyParityTest {
    private static int checks;
    private static final Pattern OLD=Pattern.compile("[\\s\\u00a0（）()｜|·:：]");
    private static String old(String s){return s==null?"":OLD.matcher(s).replaceAll("").trim();}
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args) {
        check(MatchPolicy.normalize(null).equals("")&&MatchPolicy.normalize("").equals(""),"null and empty");
        for(int code=0;code<=65535;code++) {
            char c=(char)code;String inside="A"+c+"B",ends=c+"跳过"+c;
            if(!old(inside).equals(MatchPolicy.normalize(inside))||!old(ends).equals(MatchPolicy.normalize(ends)))throw new AssertionError("UTF-16 mismatch at "+code);
        }
        check(true,"all UTF-16 code units inside and at trimmed ends match old semantics");
        Random random=new Random(515);for(int n=0;n<2048;n++) {
            StringBuilder s=new StringBuilder();for(int i=0;i<n%129;i++)s.append((char)random.nextInt(65536));String value=s.toString();
            if(!old(value).equals(MatchPolicy.normalize(value)))throw new AssertionError("mixed string mismatch "+n);
        }
        check(true,"mixed-length and surrogate strings match");
        check(!MatchPolicy.isSkipText("跳过\u3000")&&!MatchPolicy.isSkipText("跳过\u2028"),"non-legacy Unicode separators cannot broaden skip matching");
        check(MatchPolicy.isSkipText("3S | 跳过")&&MatchPolicy.isAdEvidence("点击跳转详情页面›",""),"skip and ad evidence retain exact supported forms");
        System.out.println("PASS: "+checks+" text parity groups (131072 single-character strings and 2048 mixed strings)");
    }
}
