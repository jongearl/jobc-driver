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
        pss.setCommandText(SparqlPlaceholders.normalize(sql) + " ");
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
        try {
            bind(idx - 1, obj);
        } catch (SQLException e) {
            throw e;
        } catch (RuntimeException e) {
            // Jena 가 인젝션 위험 등으로 거부한 값 (ARQException 등)
            throw new SQLException("Invalid value for parameter " + idx + ": " + e.getMessage(), "22018", e);
        }
    }

    private void bind(int i, Object obj) throws SQLException {
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
        }else if( obj instanceof RDFNode ){
            // Literal 포함. Jena 는 언어 태그·데이터타입·변수 이름을 검사하지 않고 그대로 쓰므로 먼저 검사한다
            Node node = ((RDFNode) obj).asNode();
            checkNode(node);
            pss.setParam(i, node);
        }else if( obj instanceof Node ){
            checkNode((Node) obj);
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

    private static final java.util.regex.Pattern LANG_TAG = java.util.regex.Pattern.compile("[A-Za-z]{1,8}(-[A-Za-z0-9]{1,8})*");
    private static final java.util.regex.Pattern IRI_FORBIDDEN = java.util.regex.Pattern.compile("[\\s<>\"{}|^`\\\\]");

    private static final java.util.regex.Pattern VAR_NAME = java.util.regex.Pattern.compile("[\\p{L}\\p{N}_]+");

    /**
     * Jena 는 언어 태그·데이터타입 IRI·변수 이름을 검사하지 않고 쿼리 문자열에 그대로 넣으므로 여기서 막는다 (인젝션 방지).
     * 리터럴의 값(lexical form)은 Jena 가 이스케이프하므로 검사하지 않는다.
     */
    static void checkNode(Node node) throws SQLException{
        if( node.isLiteral() ){
            String lang = node.getLiteralLanguage();
            if( lang != null && !lang.isEmpty() ){
                checkLang(lang);
            }
            String dt = node.getLiteralDatatypeURI();
            if( dt != null ){
                checkIri(dt, "datatype IRI");
            }
        }else if( node.isURI() ){
            checkIri(node.getURI(), "IRI");
        }else if( node.isVariable() ){
            if( !VAR_NAME.matcher(node.getName()).matches() ){
                throw new SQLException("Invalid variable name", "22018");
            }
        }else if( node.isTripleTerm() ){
            org.apache.jena.graph.Triple t = node.getTriple();
            checkNode(t.getSubject());
            checkNode(t.getPredicate());
            checkNode(t.getObject());
        }
        // 블랭크 노드는 Jena 가 라벨을 안전한 형태로 다시 쓴다
    }

    private static void checkLang(String lang) throws SQLException{
        if( !LANG_TAG.matcher(lang).matches() ){
            throw new SQLException("Invalid language tag", "22018");
        }
    }

    private static void checkIri(String iri, String what) throws SQLException{
        if( IRI_FORBIDDEN.matcher(iri).find() || iri.indexOf(':') < 1 ){
            throw new SQLException("Invalid " + what, "22018");
        }
    }

    private static Literal toLiteral(LiteralObj l) throws SQLException{
        String lexical = l.getLexicalForm() != null ? l.getLexicalForm() : String.valueOf(l.getValue());
        if( l.getLang() != null ){
            checkLang(l.getLang());
            return ResourceFactory.createLangLiteral(lexical, l.getLang());
        }
        if( l.getDatatype() != null ){
            String iri = expandXsd(l.getDatatype());
            checkIri(iri, "datatype IRI");
            return ResourceFactory.createTypedLiteral(lexical,
                    org.apache.jena.datatypes.TypeMapper.getInstance().getSafeTypeByName(iri));
        }
        return ResourceFactory.createPlainLiteral(lexical);
    }

    private static String expandXsd(String datatype){
        return datatype.startsWith("xsd:") ? XSDDatatype.XSD + "#" + datatype.substring(4) : datatype;
    }

    public void clearParameters(){
        pss.clearParams();
    }

    /** 값이 들어간 쿼리. 값을 넣지 않은 '?' 가 남아 있으면 SQLState 07001 */
    public String query() throws SQLException{
        String q = pss.toString();
        if( SparqlPlaceholders.count(q) > 0 ){
            throw new SQLException("Not all parameters are bound", "07001");
        }
        return q;
    }

}
