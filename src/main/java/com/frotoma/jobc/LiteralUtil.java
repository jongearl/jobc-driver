package com.frotoma.jobc;

public final class LiteralUtil {

    private LiteralUtil() {
    }


    public static boolean isLiteral(String value){
        return parseLiteral(value) != null;
    }

    public static LiteralParts parseLiteral(String valueString) {
        if( valueString == null || valueString.length() < 2 ){
            return null;
        }

        char quote = valueString.charAt(0);
        if( quote != '"' && quote != '\'' ){
            return null;
        }

        boolean escaped = false;
        int closingQuote = -1;
        for( int index = 1; index < valueString.length(); index++ ){
            char current = valueString.charAt(index);
            if( current == quote && !escaped ){
                closingQuote = index;
                break;
            }
            if( current == '\\' ){
                escaped = !escaped;
            }else{
                escaped = false;
            }
        }
        if( closingQuote < 0 ){
            return null;
        }

        String suffix = valueString.substring(closingQuote + 1);
        String lang = null;
        String datatype = null;
        if( suffix.startsWith("@") ){
            lang = suffix.substring(1);
            if( lang.isEmpty() || !isLanguageTag(lang) ){
                return null;
            }
        }else if( suffix.startsWith("^^") ){
            datatype = suffix.substring(2);
            if( datatype.isEmpty() ){
                return null;
            }
            if( datatype.startsWith("<") ){
                if( !datatype.endsWith(">") ){
                    return null;
                }
                datatype = datatype.substring(1, datatype.length() - 1);
                if( datatype.isEmpty() ){
                    return null;
                }
            }
        }else if( !suffix.isEmpty() ){
            return null;
        }
        return new LiteralParts(valueString.substring(1, closingQuote), lang, datatype);
    }

    private static boolean isLanguageTag(String value) {
        for( int index = 0; index < value.length(); index++ ){
            char current = value.charAt(index);
            if( current == '-' ){
                if( index == 0 || index == value.length() - 1 ){
                    return false;
                }
            }else if( !Character.isLetterOrDigit(current) ){
                return false;
            }
        }
        return true;
    }

    public static final class LiteralParts {
        private final String value;
        private final String lang;
        private final String datatype;

        private LiteralParts(String value, String lang, String datatype) {
            this.value = value;
            this.lang = lang;
            this.datatype = datatype;
        }

        public String getValue() {
            return value;
        }

        public String getLang() {
            return lang;
        }

        public String getDatatype() {
            return datatype;
        }
    }

}


