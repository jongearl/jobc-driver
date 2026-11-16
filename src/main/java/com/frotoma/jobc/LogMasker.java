package com.frotoma.jobc;

import java.util.regex.Pattern;

/**
 * 로그에 남기기 전에 접속 URL 안의 비밀번호를 가린다.
 */
public final class LogMasker {

    private static final Pattern PASSWORD_PARAM = Pattern.compile("(?i)(pwd|password|passwd)=[^/&;?]*");
    private static final Pattern USER_INFO = Pattern.compile("(://[^:/@]+:)[^@/]+@");

    private LogMasker() {
    }

    /** "PWD=xxx", "password=xxx", "scheme://user:xxx@host" 의 xxx 를 **** 로 바꾼다. null 이면 null. */
    public static String maskUrl(String url) {
        if (url == null) {
            return null;
        }
        String masked = PASSWORD_PARAM.matcher(url).replaceAll("$1=****");
        return USER_INFO.matcher(masked).replaceAll("$1****@");
    }
}
