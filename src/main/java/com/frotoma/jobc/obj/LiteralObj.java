/*
MIT License

Copyright (c) 2026 jongearl

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.frotoma.jobc.obj;

import java.math.BigDecimal;

import com.frotoma.jobc.LiteralUtil;
import com.frotoma.jobc.LiteralUtil.LiteralParts;

public class LiteralObj {
    
    private String datatype = null;

    private Object value = null;

    private String lang = null;

    // private String valueString = null;
    

    public LiteralObj (String valueString){
        LiteralParts parts = LiteralUtil.parseLiteral(valueString);
        //LiteralParts parts = parseLiteral(valueString);
        if( parts != null ){
            String value = parts.getValue();
            String lang = parts.getLang();
            String datatype = parts.getDatatype();

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
