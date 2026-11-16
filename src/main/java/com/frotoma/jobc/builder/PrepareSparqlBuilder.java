package com.frotoma.jobc.builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Calendar;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.irix.IRIx;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;

import com.frotoma.jobc.obj.LiteralObj;

/**
 * PreparedStatement 의 '?' 자리에 값을 넣는다. 이스케이프는 Jena ParameterizedSparqlString 이 한다.
 * @author jongearl
 */
public class PrepareSparqlBuilder {

    private final ParameterizedSparqlString pss;

    public PrepareSparqlBuilder(String sql){
        pss = new ParameterizedSparqlString();
        pss.setCommandText(sql + " ");
    }

    /**
     * @param idx 1부터 시작하는 파라미터 위치
     * @param obj 넣을 값. SPARQL 에는 NULL 이 없으므로 null 은 받지 않는다.
     */
    public void addParameter(int idx, Object obj) throws SQLException {
        if( idx < 1 ){
            throw new SQLException("Parameter index out of range: " + idx, "07009");
        }
        if( obj == null ){
            throw new SQLException("SPARQL has no NULL value (parameter " + idx + ")", "22004");
        }
        int i = idx - 1;
        if( obj instanceof String ){
            pss.setLiteral(i, (String) obj);
        }else if( obj instanceof Boolean || obj instanceof Byte || obj instanceof Short || obj instanceof Integer
                || obj instanceof Long || obj instanceof Float || obj instanceof Double ){
            pss.setLiteral(i, ResourceFactory.createTypedLiteral(obj));
        }else if( obj instanceof BigInteger ){
            pss.setLiteral(i, typed(obj.toString(), XSDDatatype.XSDinteger));
        }else if( obj instanceof BigDecimal ){
            pss.setLiteral(i, typed(((BigDecimal) obj).toPlainString(), XSDDatatype.XSDdecimal));
        }else if( obj instanceof java.sql.Date ){
            pss.setLiteral(i, typed(((java.sql.Date) obj).toLocalDate().toString(), XSDDatatype.XSDdate));
        }else if( obj instanceof java.sql.Time ){
            pss.setLiteral(i, typed(((java.sql.Time) obj).toLocalTime().toString(), XSDDatatype.XSDtime));
        }else if( obj instanceof java.sql.Timestamp ){
            pss.setLiteral(i, typed(((java.sql.Timestamp) obj).toLocalDateTime().toString(), XSDDatatype.XSDdateTime));
        }else if( obj instanceof java.util.Date ){
            pss.setLiteral(i, typed(((java.util.Date) obj).toInstant().toString(), XSDDatatype.XSDdateTime));
        }else if( obj instanceof LocalDate ){
            pss.setLiteral(i, typed(obj.toString(), XSDDatatype.XSDdate));
        }else if( obj instanceof LocalDateTime || obj instanceof OffsetDateTime ){
            pss.setLiteral(i, typed(obj.toString(), XSDDatatype.XSDdateTime));
        }else if( obj instanceof Calendar ){
            pss.setLiteral(i, (Calendar) obj);
        }else if( obj instanceof URL ){
            pss.setLiteral(i, obj.toString(), XSDDatatype.XSDanyURI);
        }else if( obj instanceof IRIx ){
            pss.setIri(i, (IRIx) obj);
        }else if( obj instanceof Literal ){
            pss.setLiteral(i, (Literal) obj);
        }else if( obj instanceof RDFNode ){
            pss.setParam(i, (RDFNode) obj);
        }else if( obj instanceof Node ){
            pss.setParam(i, (Node) obj);
        }else if( obj instanceof LiteralObj ){
            pss.setLiteral(i, toLiteral((LiteralObj) obj));
        }else{
            throw new SQLFeatureNotSupportedException("Unsupported parameter type: " + obj.getClass().getName());
        }
    }

    private static Literal typed(String lexical, XSDDatatype type){
        return ResourceFactory.createTypedLiteral(lexical, type);
    }

    private static Literal toLiteral(LiteralObj l){
        if( l.getLang() != null ){
            return ResourceFactory.createLangLiteral(l.getLexicalForm(), l.getLang());
        }
        if( l.getDatatype() != null ){
            return ResourceFactory.createTypedLiteral(l.getLexicalForm(),
                    org.apache.jena.datatypes.TypeMapper.getInstance().getSafeTypeByName(expandXsd(l.getDatatype())));
        }
        return ResourceFactory.createPlainLiteral(l.getLexicalForm());
    }

    private static String expandXsd(String datatype){
        return datatype.startsWith("xsd:") ? XSDDatatype.XSD + "#" + datatype.substring(4) : datatype;
    }

    public void clearParameters(){
        pss.clearParams();
    }

    public String query(){
        return pss.toString();
    }

}
