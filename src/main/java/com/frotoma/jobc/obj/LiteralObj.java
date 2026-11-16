package com.frotoma.jobc.obj;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.frotoma.jobc.LiteralUtil;
import com.frotoma.jobc.LiteralUtil.LiteralParts;

public class LiteralObj {
    
    private String datatype = null;

    private Object value = null;

    private String lang = null;

    // 저장소가 돌려준 값 그대로 (변환 전 어휘 형식)
    private String lexicalForm = null;


    /**
     * Turtle 스타일로 직렬화된 문자열( "값"@lang / "값"^^datatype )을 파싱해서 생성한다.
     * @param valueString Turtle 스타일 리터럴 문자열
     */
    public LiteralObj (String valueString){
        LiteralParts parts = LiteralUtil.parseLiteral(valueString);
        if( parts != null ){
            init(parts.getValue(), parts.getLang(), parts.getDatatype());
        }else{
            this.value = valueString;
            this.lexicalForm = valueString;
        }
    }

    /**
     * SPARQL 1.1 JSON Results 바인딩("value", "xml:lang", "datatype")으로부터 직접 생성한다.
     * @param value 리터럴 값
     * @param lang 언어 태그 (없으면 null)
     * @param datatype 데이터타입 URI 또는 xsd: 접두 표기 (없으면 null)
     */
    public LiteralObj (String value, String lang, String datatype){
        init(value, lang, datatype);
    }

    private void init(String value, String lang, String datatype){
        this.lang = lang;
        this.datatype = datatype;
        this.lexicalForm = value;
        try {
            this.value = convert(value, localName(datatype));
        } catch (NumberFormatException e) {
            // Java 값으로 바꿀 수 없는 형식("2026-05"^^xsd:gYearMonth 등)은 문자열 그대로 둔다
            this.value = value;
        }
    }

    private static Object convert(String value, String localType){
        if( localType == null ){
            return value;
        }else if( localType.equalsIgnoreCase("string") ){
            return value;
        }else if( localType.equalsIgnoreCase("integer")){
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return new BigInteger(value);   // int 범위를 넘는 정수
            }
        }else if( localType.equalsIgnoreCase("double")){
            return Double.parseDouble(value);
        }else if( localType.equalsIgnoreCase("float")){
            return Float.parseFloat(value);
        }else if( localType.equalsIgnoreCase("long")){
            return Long.parseLong(value);
        }else if( localType.equalsIgnoreCase("boolean")){
            return Boolean.parseBoolean(value);
        }else if( localType.equalsIgnoreCase("short")){
            return Short.parseShort(value);
        }else if( localType.equalsIgnoreCase("byte")){
            return Byte.parseByte(value);
        }else if( localType.equalsIgnoreCase("decimal")){
            return new BigDecimal(value);
        }else if( localType.equalsIgnoreCase("gDay")){
            return Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("gMonth")){
            return Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("gMonthDay")){
            return Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("gYear")){
            return Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("gYearMonth")){
            return Integer.parseInt(value);
        }else{
            return value;
        }
    }

    /**
     * "xsd:integer" 같은 접두 표기와 전체 URI("http://www.w3.org/2001/XMLSchema#integer") 표기
     * 모두에서 로컬 타입 이름("integer")만 뽑아낸다.
     * @param datatype xsd 접두 표기 또는 전체 URI (null 가능)
     * @return 로컬 타입 이름, datatype이 null이면 null
     */
    private static String localName(String datatype){
        if( datatype == null ){
            return null;
        }
        int hashIdx = datatype.lastIndexOf('#');
        if( hashIdx >= 0 ){
            return datatype.substring(hashIdx + 1);
        }
        int colonIdx = datatype.lastIndexOf(':');
        if( colonIdx >= 0 ){
            return datatype.substring(colonIdx + 1);
        }
        return datatype;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /** 저장소가 돌려준 값 그대로 ("007"^^xsd:integer 이면 "007"). getValue() 는 Java 값으로 바꾼 값(7) */
    public String getLexicalForm() {
        return lexicalForm;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("\"").append( value.toString().replaceAll("\"", "\\\"") ).append("\"");
        if( datatype != null ){
            sb.append("^^").append(datatype);
        }else if( lang != null) {
            sb.append("@").append(lang);
        }
        return sb.toString();
    }

    // public static void main(String[] args){
    //     Literal l = new Literal("\"test\"^^\"dsadf\"");
    //     System.out.println( "DTYPE : " + l.getDatatype() );
    //     System.out.println( "LANG : " + l.getLang() );
    //     System.out.println( "VALUE : " + l.getValue() );
    //     System.out.println( "toString  : " + l.toString());
        
    // }

}
