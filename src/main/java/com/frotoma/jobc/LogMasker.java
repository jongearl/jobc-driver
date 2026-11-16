package com.frotoma.jobc;

import java.util.regex.Pattern;

/**
 * 로그에 남기기 전에 접속 URL 안의 비밀번호를 가린다.
 */
public final class LogMasker {

    /** 값은 다음 '/'(Virtuoso 구분자) 또는 '&'(URL 속성 구분자)까지. ';' '?' '@' 는 비밀번호에 들어갈 수 있다. */
    private static final Pattern PASSWORD_PARAM = Pattern.compile("(?i)(pwd|password|passwd)=[^/&]*");
    /** scheme://user:비밀번호@host — 비밀번호에 '@' 가 있을 수 있으므로 첫 '/' 앞의 마지막 '@' 까지 */
    private static final Pattern USER_INFO = Pattern.compile("(://[^:/@]+:)[^/]*@");

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
