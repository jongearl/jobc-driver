package com.frotoma.jobc.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.Test;

import com.frotoma.jobc.obj.LiteralObj;

/**
 * PreparedStatement 바인딩 경로별 SPARQL 인젝션 시도.
 * 안전 = 바인딩 단계에서 SQLException, 또는 결과가 INSERT DATA 1개·트리플 1개이고 값이 입력 그대로.
 */
public class InjectionFixTest {

    private static final String TEMPLATE = "INSERT DATA { GRAPH <urn:g> { <urn:s> <urn:p> ? } }";
    private static final String BREAK = " } } ; DROP ALL ; INSERT DATA { GRAPH <urn:g> { <urn:a> <urn:b> <urn:c> } } #";

    private static Node boundObject(Object value) throws SQLException {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder(TEMPLATE);
        b.addParameter(1, value);
        UpdateRequest req = UpdateFactory.create(b.query());
        assertEquals("연산 수", 1, req.getOperations().size());
        List<Quad> quads = ((UpdateDataInsert) req.getOperations().get(0)).getQuads();
        assertEquals("트리플 수", 1, quads.size());
        return quads.get(0).getObject();
    }

    private static void rejected(Object value) {
        try {
            boundObject(value);
            fail("바인딩 단계에서 거부되어야 한다");
        } catch (SQLException expected) {
            // 거부됨
        }
    }

    // ---- 문자열: 리터럴 하나로 남아야 한다 ----

    @Test
    public void stringQuoteBreakStaysLiteral() throws Exception {
        String v = "x\"" + BREAK;
        assertEquals(v, boundObject(v).getLiteralLexicalForm());
    }

    @Test
    public void stringCodepointEscapeStaysLiteral() throws Exception {
        String v = "\\u0022" + BREAK;
        assertEquals(v, boundObject(v).getLiteralLexicalForm());
        String w = "\\U00000022" + BREAK;
        assertEquals(w, boundObject(w).getLiteralLexicalForm());
    }

    @Test
    public void stringControlCharsStayLiteral() throws Exception {
        String v = "a\n\r\t\u0000 " + BREAK;
        assertEquals(v, boundObject(v).getLiteralLexicalForm());
    }

    @Test
    public void literalObjLexicalStaysLiteral() throws Exception {
        String v = "x\"" + BREAK;
        assertEquals(v, boundObject(new LiteralObj(v, null, "xsd:string")).getLiteralLexicalForm());
    }

    @Test
    public void urlStaysLiteral() throws Exception {
        java.net.URL u = java.net.URI.create("http://x/a%3E%20%7D").toURL();
        assertTrue(boundObject(u).isLiteral());
    }

    // ---- Jena 객체: 검사 없이 문자열에 들어가던 경로 ----

    @Test
    public void jenaLiteralBadLangRejected() {
        rejected(ResourceFactory.createLangLiteral("1", "en" + BREAK));
    }

    @Test
    public void jenaLiteralBadDatatypeRejected() {
        rejected(ResourceFactory.createTypedLiteral("1", TypeMapper.getInstance().getSafeTypeByName("http://x>" + BREAK)));
    }

    @Test
    public void jenaNodeLiteralBadLangRejected() {
        rejected(NodeFactory.createLiteralLang("1", "en" + BREAK));
    }

    @Test
    public void jenaNodeVariableBadNameRejected() {
        rejected(NodeFactory.createVariable("v" + BREAK));
    }

    @Test
    public void jenaUriWithBracketRejected() {
        rejected(NodeFactory.createURI("http://x>" + BREAK));
        rejected(ResourceFactory.createResource("http://x>" + BREAK));
    }

    @Test
    public void validJenaValuesStillBind() throws Exception {
        assertEquals("ko", boundObject(ResourceFactory.createLangLiteral("화성", "ko")).getLiteralLanguage());
        assertEquals("http://ex/a", boundObject(NodeFactory.createURI("http://ex/a")).getURI());
        assertTrue(boundObject(ResourceFactory.createTypedLiteral("5",
                TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#int"))).isLiteral());
    }
}
