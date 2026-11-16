package com.frotoma.jobc.builder;

/**
 * SPARQL 텍스트에서 PreparedStatement 자리표시자 '?' 를 찾는다.
 * 문자열 리터럴, IRI(&lt;...&gt;), 주석(#...), 변수(?name) 안의 '?' 는 자리표시자가 아니다.
 *
 * Jena ParameterizedSparqlString 은 '?' 바로 뒤가 ')' '}' 같은 문자면 자리표시자로 보지 않아
 * 값이 다음 '?' 로 밀린다. normalize() 는 자리표시자 뒤에 공백을 넣어 이를 막는다.
 */
final class SparqlPlaceholders {

    private SparqlPlaceholders() {
    }

    interface Visitor {
        /** @param pos 자리표시자 '?' 의 위치 */
        void placeholder(int pos);
    }

    /** 자리표시자 '?' 뒤에 공백이 없으면 공백을 넣는다 */
    static String normalize(String sparql) {
        if (sparql == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(sparql.length() + 8);
        int[] last = { 0 };
        scan(sparql, pos -> {
            out.append(sparql, last[0], pos + 1);
            last[0] = pos + 1;
            if (pos + 1 < sparql.length() && !Character.isWhitespace(sparql.charAt(pos + 1))) {
                out.append(' ');
            }
        });
        out.append(sparql, last[0], sparql.length());
        return out.toString();
    }

    /** 자리표시자 '?' 의 개수 */
    static int count(String sparql) {
        int[] n = { 0 };
        scan(sparql, pos -> n[0]++);
        return n[0];
    }

    static void scan(String s, Visitor visitor) {
        int len = s.length();
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipString(s, i);
            } else if (c == '#') {
                while (i < len && s.charAt(i) != '\n' && s.charAt(i) != '\r') i++;
            } else if (c == '<' && isIriStart(s, i)) {
                i = s.indexOf('>', i) + 1;
            } else if (c == '?' || c == '$') {
                if (i + 1 < len && isVarChar(s.charAt(i + 1))) {
                    i++;
                    while (i < len && isVarChar(s.charAt(i))) i++;
                } else {
                    if (c == '?') visitor.placeholder(i);
                    i++;
                }
            } else {
                i++;
            }
        }
    }

    /** 따옴표로 시작하는 문자열(긴 문자열 """...""" 포함)의 끝 다음 위치 */
    private static int skipString(String s, int start) {
        char q = s.charAt(start);
        int len = s.length();
        boolean longString = start + 2 < len && s.charAt(start + 1) == q && s.charAt(start + 2) == q;
        int i = start + (longString ? 3 : 1);
        while (i < len) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == q) {
                if (!longString) return i + 1;
                if (i + 2 < len && s.charAt(i + 1) == q && s.charAt(i + 2) == q) return i + 3;
            }
            i++;
        }
        return len;
    }

    /** '&lt;' 가 IRI 시작인지 (비교 연산자 &lt; 가 아닌지): 공백 없이 '&gt;' 로 닫혀야 한다 */
    private static boolean isIriStart(String s, int i) {
        for (int j = i + 1; j < s.length(); j++) {
            char c = s.charAt(j);
            if (c == '>') return true;
            if (Character.isWhitespace(c) || c == '<' || c == '"' || c == '{' || c == '}') return false;
        }
        return false;
    }

    private static boolean isVarChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '·';
    }
}
