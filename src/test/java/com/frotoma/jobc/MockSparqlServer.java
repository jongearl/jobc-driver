package com.frotoma.jobc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

/**
 * 테스트용 SPARQL 엔드포인트. 마지막 요청을 기록하고, 정해 둔 응답을 돌려준다.
 */
public class MockSparqlServer implements AutoCloseable {

    public static final String DEFAULT_JSON = "{\"head\":{\"vars\":[\"n\"]},\"results\":{\"bindings\":["
            + "{\"n\":{\"type\":\"literal\",\"value\":\"007\",\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\"}}]}}";

    private final HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();

    private volatile int status = 200;
    private volatile String contentType = "application/sparql-results+json";
    private volatile String body = DEFAULT_JSON;
    private volatile long delayMillis = 0;
    private volatile boolean chunked = false;

    private volatile String lastMethod;
    private volatile String lastPath;
    private volatile String lastRawQuery;
    private volatile String lastBody;
    private volatile Headers lastHeaders;

    private MockSparqlServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            lastMethod = exchange.getRequestMethod();
            lastPath = exchange.getRequestURI().getPath();
            lastRawQuery = exchange.getRequestURI().getRawQuery();
            lastHeaders = exchange.getRequestHeaders();
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] out = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }
            try {
                if (status == 204 || out.length == 0) {
                    exchange.sendResponseHeaders(status, -1);
                } else {
                    exchange.sendResponseHeaders(status, chunked ? 0 : out.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(out);
                    }
                }
            } catch (IOException ignored) {
                // 클라이언트가 먼저 끊은 경우
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    public static MockSparqlServer start() throws IOException {
        return new MockSparqlServer();
    }

    public String base() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public MockSparqlServer respond(int status, String contentType, String body) {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
        return this;
    }

    public MockSparqlServer delayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
        return this;
    }

    public MockSparqlServer chunked(boolean chunked) {
        this.chunked = chunked;
        return this;
    }

    public String lastMethod() { return lastMethod; }
    public String lastPath() { return lastPath; }
    public String lastRawQuery() { return lastRawQuery; }
    public String lastBody() { return lastBody; }
    public int requestCount() { return requestCount.get(); }

    public String lastHeader(String name) {
        Headers h = lastHeaders;
        return h == null ? null : h.getFirst(name);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
