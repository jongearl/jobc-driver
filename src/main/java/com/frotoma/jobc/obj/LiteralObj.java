package com.frotoma.jobc.obj;

import java.math.BigDecimal;

import com.frotoma.jobc.LiteralUtil;
import com.frotoma.jobc.LiteralUtil.LiteralParts;

public class LiteralObj {
    
    private String datatype = null;

    private Object value = null;

    private String lang = null;

    // private String valueString = null;
    

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

        String localType = localName(datatype);

        if( localType == null ){
            this.value = value;
        }else if( localType.equalsIgnoreCase("string") ){
            this.value = value;
        }else if( localType.equalsIgnoreCase("integer")){
            this.value = Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("double")){
            this.value = Double.parseDouble(value);
        }else if( localType.equalsIgnoreCase("float")){
            this.value = Float.parseFloat(value);
        }else if( localType.equalsIgnoreCase("long")){
            this.value = Long.parseLong(value);
        }else if( localType.equalsIgnoreCase("boolean")){
            this.value = Boolean.parseBoolean(value);
        }else if( localType.equalsIgnoreCase("short")){
            this.value = Short.parseShort(value);
        }else if( localType.equalsIgnoreCase("byte")){
            this.value = Byte.parseByte(value);
        }else if( localType.equalsIgnoreCase("decimal")){
            this.value = new BigDecimal(value);
        }else if( localType.equalsIgnoreCase("gDay")){
            this.value = Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("gMonth")){
            this.value = Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("gMonthDay")){
            this.value = Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("gYear")){
            this.value = Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("gYearMonth")){
            this.value = Integer.parseInt(value);
        }else if( localType.equalsIgnoreCase("QName")){
            this.value = value;
        }else if( localType.equalsIgnoreCase("anyURI")){
            this.value = value;
        }else{
            this.value = value;
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
