package com.frotoma.jobc.obj;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiteralObj {


    public static final Pattern literalPattern = Pattern.compile("^[\"|\']((.|\n|\r)*?)[\"|\']((@(.{2}))$|(\\^\\^<(.+?)>)$|(\\^\\^(.+?))$)");
    
    private String datatype = null;

    private Object value = null;

    private String lang = null;

    // private String valueString = null;
    

    public LiteralObj (String valueString){
        Matcher m = literalPattern.matcher(valueString);
        if( m.find() ){
            String value = m.group(1);
            String lang = m.group(5);
            String datatype = m.group(9);

            this.lang = lang;
            this.datatype = datatype;

            if( datatype == null ){
                this.value = value;
            }else if( datatype.equals("xsd:string") ){
                this.value = value;
            }else if( datatype.equalsIgnoreCase("xsd:integer")){
                this.value = Integer.parseInt(value);            
            }else if( datatype.equalsIgnoreCase("xsd:double")){
                this.value = Double.parseDouble(value);
            }else if( datatype.equalsIgnoreCase("xsd:float")){
                this.value = Float.parseFloat(value);
            }else if( datatype.equalsIgnoreCase("xsd:long")){
                this.value = Long.parseLong(value);    
            }else if( datatype.equalsIgnoreCase("xsd:boolean")){
                this.value = Boolean.parseBoolean(value);    
            }else if( datatype.equalsIgnoreCase("xsd:short")){
                this.value = Short.parseShort(value);        
            }else if( datatype.equalsIgnoreCase("xsd:byte")){
                this.value = Byte.parseByte(value);            
            }else if( datatype.equalsIgnoreCase("xsd:decimal")){
                this.value = new BigDecimal(value);
            }else if( datatype.equalsIgnoreCase("xsd:gDay")){
                this.value = Integer.parseInt(value);    
            }else if( datatype.equalsIgnoreCase("xsd:gMonth")){
                this.value = Integer.parseInt(value);    
            }else if( datatype.equalsIgnoreCase("xsd:gMonthDay")){
                this.value = Integer.parseInt(value);    
            }else if( datatype.equalsIgnoreCase("xsd:gYear")){
                this.value = Integer.parseInt(value);                
            }else if( datatype.equalsIgnoreCase("xsd:gYearMonth")){
                this.value = Integer.parseInt(value);                    
            }else if( datatype.equalsIgnoreCase("xsd:QName")){
                this.value = value;
            }else if( datatype.equalsIgnoreCase("xsd:anyURI")){
                this.value = value;    
            }else{
                this.value = value;
            }
        }else{
            this.value = valueString;
        }
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
