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
package com.frotoma.jobc.builder;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.irix.IRIx;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author jongearl
 */
public class ParamBuilder {
    
    public static URL createURL( String urlString ){
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {}
        return url;
    }
    
    public static IRIx createIRI( String iriString){
        return IRIx.create(iriString);
    }
    
    public static Literal createLiteral(String str) {
        return ResourceFactory.createPlainLiteral(str);
    }
    
    public static Literal createLiteral(String str, String en) {
        return ResourceFactory.createLangLiteral(str, en);
    }
    
    public static Literal createLiteral(Integer obj) {
        return ResourceFactory.createTypedLiteral(obj);
    }
    
    public static Literal createLiteral(Double obj) {
        return ResourceFactory.createTypedLiteral(obj);
    }
    
    public static Literal createLiteral(Float obj) {
        return ResourceFactory.createTypedLiteral(obj);
    }
    
    public static Literal createLiteral(Boolean obj) {
        return ResourceFactory.createTypedLiteral(obj);
    }
    
    public static Literal createLiteral(URL uri) {
        return ResourceFactory.createTypedLiteral(uri);
    }
    
    public static Literal createLiteral(Date date) {
        return ResourceFactory.createTypedLiteral(date);
    }
    
    public static Literal createTypedLiteral(String str, XSDDatatype type) {
        return ResourceFactory.createTypedLiteral(str, type);
    }
}
