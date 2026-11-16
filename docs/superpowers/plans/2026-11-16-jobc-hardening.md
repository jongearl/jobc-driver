# JOBC 드라이버 보안·안정성 보강 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 코드 리뷰(2026-11-16)에서 나온 인증정보 노출, 무제한 대기/메모리, 자원 누수, JDBC 규약 위반, 레거시 인젝션 코드를 고친다.

**Architecture:** HTTP 계층은 `APIMangerService` 하나로 모으고(레거시 `rest/API*Service` 삭제), Web/Fuseki 드라이버의 중복 코드는 `AbstractHttpSparqlDriver`로 묶는다. 커넥션 풀은 참조 카운트로 마지막 `Connection.close()` 때 닫는다. 파라미터 바인딩은 `PrepareSparqlBuilder.addParameter(int, Object)` 한 곳에서 타입별로 처리하고, 이스케이프는 계속 Jena `ParameterizedSparqlString`에 맡긴다.

**Tech Stack:** Java 21, Maven 3.6.3 (`mvn -o`), JUnit 4.11, Apache HttpClient 4.5.13, Jena 6.2.0, org.json, SLF4J/Logback, HikariCP 2.7.9 (테스트 전용)

**Spec:** 이 대화에서 나온 리뷰 결과(🔴/🟠/🟡 항목). 별도 문서는 없다. 각 Task 제목 옆에 다루는 리뷰 항목을 적었다.

## Global Constraints

- Java 21. 새 런타임 의존성은 추가하지 않는다. `slf4j-api`만 명시하고, `logback-classic`은 test scope로 옮긴다.
- 기존 공개 클래스 이름(`APIMangerService`, `WebSparqlResultset` 등)의 철자는 바꾸지 않는다. 철자 정리는 이 계획의 범위 밖이다.
- 외부 서버 없이 도는 테스트만 회귀 기준으로 쓴다. 새 테스트 클래스 이름은 모두 `*FixTest`로 짓는다. 회귀 명령은 `mvn -q -o test -Dtest=*FixTest`이고, 기준선은 현재 `JobcDriverFixTest` 11개 통과다.
- 라이브 서버가 필요한 기존 테스트(`FusekiTest`, `VirtuosoTest` 등)는 컴파일만 유지한다(`mvn -q -o test-compile`).
- SQLState 값: 접속 닫힘 `08003`, 통신 오류 `08S01`, 인증 실패 `28000`, 인덱스 범위 오류 `07009`, NULL 불가 `22004`, 형변환 실패 `22018`, 숫자 범위 초과 `22003`.
- HTTP 기본값: connecttimeout `5000`, connectionrequesttimeout `10000`, sockettimeout `60000`, maxresponsebytes `67108864`(64 MiB).
- 로그에는 비밀번호, Base64 인증값, `Authorization` 헤더를 남기지 않는다. SPARQL 본문은 DEBUG 레벨로만 남긴다.

### 확정된 결정 (2026-11-16 사용자 확인)

1. **`setNull()`은 예외를 던진다.** `SQLException("SPARQL has no NULL value (parameter N)", "22004")`. 기존 `""` 바인딩은 의도된 동작이 아니었다.
2. **버전은 `0.0.2`를 유지한다.** `APIMangerService.getPassword()`/`getUser()`, `PrepareSparqlBuilder`의 타입별 오버로드, 레거시 클래스는 지운다.
3. `WebSparqlConnection.getMetaData()`는 `null` 대신 `SQLFeatureNotSupportedException`을 던진다. null이면 호출한 쪽에서 NPE가 나지만, 예외면 Spring/MyBatis가 정상 처리한다.

## Review Focus

1. 비밀번호에 `=`·`%`·`;` 같은 문자가 들어 있는 경우. URL 파라미터로 넘겨도 원래 값 그대로 인증에 쓰이고, 어떤 로그에도 남지 않아야 한다. 테스트는 Task 1, Task 4.
2. OPTIONAL로 바인딩되지 않은 컬럼을 MyBatis가 `getInt`/`getLong`/`getBoolean`으로 읽는 경우. NPE 없이 `0`/`false`를 돌려주고 `wasNull()`이 `true`여야 한다. 테스트는 Task 6.
3. 응답 크기 초과나 HTTP 오류가 연속으로 나는 경우. 그 뒤에도 풀 연결이 반납되어 다음 쿼리가 정상 실행돼야 한다. 테스트는 Task 3.
4. HikariCP 같은 풀이 `isValid`·`setNetworkTimeout`·`getNetworkTimeout`·`close`를 부르는 경우. 예외가 나지 않아야 하고, 풀을 닫은 뒤에는 HTTP 풀도 닫혀야 한다. 테스트는 Task 7.
5. 인젝션 시도 문자열(`x" } ; DROP ALL ; INSERT DATA { ...`)을 `setString`으로 넘기는 경우. Web, Fuseki, Virtuoso 모두에서 리터럴 하나로 남아야 한다. 테스트는 Task 5, Task 8.

---

### Task 0: 작업 기반 정리

**Files:** 없음 (git 작업만)

- [ ] **Step 1: 현재 미커밋 변경을 사용자에게 확인받고 커밋한다.** 대상은 `pom.xml`, `FusekiSparqlDriver`, `LiteralObj`, `APIMangerService` 등 8개 파일과 `src/test/java/com/frotoma/jobc/`이다. 이 변경들은 이 계획 이전에 하던 작업이다.

```bash
git add pom.xml src/main src/test/java/com/frotoma/jobc
git commit -m "fix: per-connection settings, POST body, UTF-8, isValid socket close"
```

- [ ] **Step 2: 브랜치를 만든다.** `git switch -c fix/hardening`
- [ ] **Step 3: 기준선을 확인한다.** `mvn -q -o test -Dtest=*FixTest`를 실행해 `Tests run: 11, Failures: 0, Errors: 0`이 나오는지 본다.

---

### Task 1: 인증정보 노출 차단 (리뷰 🔴1, 테스트 로그에서 발견된 wire 로그)

**Files:**
- Create: `src/main/java/com/frotoma/jobc/LogMasker.java`
- Modify: `src/main/java/com/frotoma/jobc/virt/VirtuosoSparqlDriver.java:41`
- Modify: `src/main/java/com/frotoma/jobc/virt/VirtuosoStatement.java:33`, `virt/VirtuosoPreparedStatement.java:187` (SQL 로그 `info` → `debug`)
- Modify: `src/main/java/com/frotoma/jobc/rest/APIMangerService.java`
- Modify: `pom.xml` (의존성)
- Create: `src/test/resources/logback-test.xml`
- Create: `src/test/java/com/frotoma/jobc/MockSparqlServer.java` (테스트 도우미)
- Test: `src/test/java/com/frotoma/jobc/CredentialFixTest.java`
- Modify: `src/test/java/com/frotoma/jobc/JobcDriverFixTest.java:143`

**Interfaces:**
- Produces: `public static String LogMasker.maskUrl(String url)`
  - `pwd`, `password`, `passwd` 키(대소문자 무시) 뒤 `=`부터 다음 구분자(`/`, `&`, `;`, `?`) 또는 문자열 끝까지를 `****`로 바꾼다.
  - URL의 user-info 비밀번호(`scheme://user:pw@host`)도 `****`로 바꾼다.
  - `null`이 들어오면 `null`을 돌려준다.
- Produces: `MockSparqlServer` (이후 모든 HTTP 테스트에서 사용)
  - `static MockSparqlServer start()`, `String base()`, `void close()`
  - 응답 설정: `respond(int status, String contentType, String body)`, `delayMillis(long)`, `chunked(boolean)`
  - 요청 기록: `lastMethod()`, `lastPath()`, `lastRawQuery()`, `lastBody()`, `lastHeader(String name)`, `requestCount()`
  - `JobcDriverFixTest`의 내장 `HttpServer` 패턴을 옮겨 오되, 기존 테스트는 고치지 않는다.
- Produces: `APIMangerService.getProperty(String key)`. 키가 `"password"`면 항상 `null`을 돌려준다. `getPassword()`와 `getUser()`는 삭제한다.

- [ ] **Step 1: 실패하는 테스트를 작성한다.** `CredentialFixTest`에 넣을 내용:

```java
@Test public void masksVirtuosoPwd() {
    assertEquals("jdbc:virtuoso://h:1111/UID=dba/PWD=****/charset=UTF-8",
        LogMasker.maskUrl("jdbc:virtuoso://h:1111/UID=dba/PWD=s3cr=t/charset=UTF-8"));
}
@Test public void masksQueryPassword() {
    assertEquals("http://h/ds&user=u&password=****&method=post",
        LogMasker.maskUrl("http://h/ds&user=u&password=a%3Db&method=post"));
}
@Test public void masksUserInfo() { assertEquals("http://u:****@h/ds", LogMasker.maskUrl("http://u:p@h/ds")); }
@Test public void maskNull() { assertNull(LogMasker.maskUrl(null)); }

@Test public void credentialsSentAsBasicAuth() throws Exception  // user=u1,password=p2 로 mock 에 쿼리
    // → lastHeader("Authorization") == "Basic " + base64("u1:p2")
@Test public void passwordNotReadableFromConnection()             // c.getValue("password") == null
@Test public void noFrotomaLogContainsSecret() throws Exception
    // logback ListAppender를 root 에 붙이고 com.frotoma 로거를 DEBUG 로 설정.
    // (1) Fuseki: password=topsecret 으로 mock 서버에 POST 쿼리
    // (2) Virtuoso: "jobc:virtuoso://127.0.0.1:1/UID=dba/PWD=topsecret" connect → SQLException 예상
    // → loggerName 이 "com.frotoma" 로 시작하는 모든 이벤트의 formattedMessage 에
    //   "topsecret" 과 base64("u:topsecret") 이 없어야 한다
@Test public void warnsOnBasicAuthOverPlainHttp()                // WARN 이벤트 1건, 메시지에 "plain HTTP" 포함
```

`JobcDriverFixTest.differentCredentialsGetDifferentSettings`의 `getPassword()` 단언(143행)은 삭제한다. `assertNotSame` 단언은 남긴다.

- [ ] **Step 2: 테스트가 실패하는지 확인한다.** `mvn -q -o test -Dtest=CredentialFixTest`를 실행하면 `LogMasker` 미정의로 컴파일 에러가 나야 한다.
- [ ] **Step 3: 구현한다.**
  - `LogMasker`: 정규식 두 개로 구현한다. `(?i)(pwd|password|passwd)=[^/&;?]*`는 `$1=****`로, `(://[^:/@]+:)[^@/]+@`는 `$1****@`로 치환한다.
  - `VirtuosoSparqlDriver` 41행: `logger.debug(..., LogMasker.maskUrl(convertedUrl))`로 바꾼다.
  - `APIMangerService`:
    - POST 요청 헤더를 덤프하는 루프(202–206행)를 삭제한다.
    - `"AUTH : " + username` 로그를 삭제한다.
    - 생성자에서 `url`이 `http://`로 시작하고 `user`가 있으면 `logger.warn("Basic auth over plain HTTP: credentials are sent unencrypted to {}", LogMasker.maskUrl(url))`을 남긴다.
  - `pom.xml`:
    - `logback-classic`에 `<scope>test</scope>`를 추가한다.
    - `org.slf4j:slf4j-api:2.0.17`을 compile 의존성으로 추가한다.
    - 버전 선택: logback 1.6.3이 쓰는 slf4j 2.0.x 라인에 맞춘다. 해당 버전이 로컬 저장소에 있는지 `mvn -o dependency:tree`로 확인하고, 없으면 tree에 나온 slf4j 버전을 그대로 쓴다.
  - `logback-test.xml`: root는 INFO, `org.apache.http`는 WARN, `com.frotoma`는 DEBUG로 둔다.
- [ ] **Step 4: 통과하는지 확인한다.** `mvn -q -o test -Dtest=*FixTest`를 실행해 전부 PASS인지 본다.
- [ ] **Step 5: 커밋한다.** `git commit -m "fix: stop leaking credentials via logs and public getters"`

---

### Task 2: 레거시 코드 삭제 (리뷰 🔴4)

**Files:**
- Delete:
  - `builder/PrepareQueryBuilder.java`
  - `builder/VirtuosoPreparedSparqlBuilder.java`
  - `virt/VirtuosoConnection4pstmt.java`
  - `virt/VirtuosoPreparedStatement4psmt.java`
  - `rest/APISelectGetService.java`
  - `rest/APISelectPostService.java`
  - `rest/APIUpdatePostFusekiService.java`
  - `rest/AbstractAPIService.java`
  - (모두 `src/main/java/com/frotoma/jobc/` 아래)
- Modify: `rest/APIMangerService.java` (`isReachable` 추가)
- Modify: `web/WebSparqlConnection.java:308-313` (`isValid`)
- Modify: `src/test/java/com/frotoma/onto/sparql/PareparedUpdateSparqlTest.java:4` (사용하지 않는 import 삭제)

**Interfaces:**
- Produces: `public boolean APIMangerService.isReachable(int timeoutMillis) throws SQLException`
  - `AbstractAPIService.isValid`의 본문을 옮긴다. URL은 `this.url`을 쓴다.
  - 열었던 연결은 반드시 닫는다.

- [ ] **Step 1: 파일을 삭제하고 `isReachable`을 옮긴 뒤, `WebSparqlConnection.isValid`가 `apiManager.isReachable(millis)`를 호출하도록 바꾼다.**
- [ ] **Step 2: 컴파일을 확인한다.** `mvn -q -o test-compile`이 에러 없이 끝나야 한다.
- [ ] **Step 3: 회귀를 확인한다.** `mvn -q -o test -Dtest=*FixTest`를 실행한다. 특히 `isValidClosesItsSocket`이 PASS여야 한다.
- [ ] **Step 4: 커밋한다.** `git commit -m "refactor: remove unused legacy builders and HTTP services"`

---

### Task 3: HTTP 클라이언트 강화 (리뷰 🔴2, 🟡3, 🟡4)

**Files:**
- Modify: `src/main/java/com/frotoma/jobc/rest/APIMangerService.java`
- Modify: `src/main/java/com/frotoma/jobc/JobcDriverException.java`
- Modify: `web/WebSparqlStatement.java` (catch 블록 2곳, `executeUpdate`)
- Modify: `fuseki/FusekiSparqlStatement.java` (예외 변환)
- Test: `src/test/java/com/frotoma/jobc/rest/ApiManagerFixTest.java`

**Interfaces:**
- Produces:
  - `String get(String url, Map<String,String> param, int timeoutMillis) throws JobcDriverException`
  - `String post(String url, Map<String,String> param, int timeoutMillis) throws JobcDriverException`
  - `timeoutMillis <= 0`이면 접속 기본 socketTimeout을 쓴다. 기존 2-인자 메서드는 `0`을 넘겨 위 메서드를 호출한다.
  - 2xx이면서 본문이 비어 있으면(204 포함) `null`을 돌려준다.
- Produces: `RequestConfig defaultRequestConfig()` (package-private, 테스트용)
- Produces: `public void close()`. `APIMangerService implements AutoCloseable`로 바꾸고, `close()`에서 `cm.close()`를 호출한다.
- Produces: `public SQLException JobcDriverException.toSQLException()`. cause가 `SQLException`이면 그것을, 아니면 `new SQLException(getMessage(), "08S01", this)`를 돌려준다.
  - `WebSparqlStatement`의 모든 `catch (JobcDriverException ex)`는 `throw ex.toSQLException()`으로 바꾼다.
  - `executeUpdate`도 `JobcDriverException`을 잡아야 한다. 지금은 Fuseki 경로에서 RuntimeException이 그대로 새어 나간다.

- [ ] **Step 1: 실패하는 테스트를 작성한다.** `MockSparqlServer`를 쓴다.

```java
@Test public void defaultsApplied()        // defaultRequestConfig(): socketTimeout 60000, connectTimeout 5000, connectionRequestTimeout 10000
@Test public void invalidNumberFallsBack() // sockettimeout="abc" → socketTimeout 60000, 예외 없음
@Test public void perRequestTimeout()      // delayMillis(3000); get(url, q, 300) → JobcDriverException, 경과 < 2500ms
@Test public void acceptHeader()           // lastHeader("Accept") == "application/sparql-results+json, application/json;q=0.9"
                                           // GET 요청은 lastHeader("Content-Type") == null
@Test public void status401()              // respond(401,..) → toSQLException().getSQLState()=="28000", 메시지 "HTTP 401" 으로 시작
@Test public void status500BodyTruncated() // body 5000자 → 메시지 길이 <= "HTTP 500: ".length() + 1000, SQLState "08S01"
@Test public void status204ReturnsNull()   // respond(204, null, "") → get(...) == null
@Test public void oversizedRejected()      // maxresponsebytes=1000, body 5000B → 메시지에 "maxresponsebytes" 포함
@Test public void oversizedChunkedRejected() // chunked(true) 로 같은 조건
@Test public void errorsDoNotLeakPool()    // connectionrequesttimeout=2000, maxresponsebytes=1000;
                                           // 초과 응답 25회(경로당 최대 20개보다 많게) 후 정상 응답 1회 성공
@Test public void fusekiUpdateErrorIsSQLException() // Fuseki connection executeUpdate, mock 500 → SQLException(RuntimeException 아님)
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다.** `mvn -q -o test -Dtest=ApiManagerFixTest`를 실행하면 컴파일 에러나 FAIL이 나야 한다.
- [ ] **Step 3: 구현한다.**
  - `private static int intProp(Properties p, String key, int def)`: 값이 형식에 맞지 않으면 warn을 남기고 `def`를 돌려준다.
  - 요청별 타임아웃은 `RequestConfig.copy(defaultConfig).setSocketTimeout(ms).build()`로 만들어 `request.setConfig`에 넣는다.
  - `private static String readBounded(HttpRequestBase req, HttpEntity e, long max) throws IOException`:
    - `Content-Length`가 `max`보다 크면 바로 `req.abort()`하고 예외를 던진다.
    - 그렇지 않으면 최대 `max + 1` 바이트까지 읽는다. 초과하면 `req.abort()` 후 `IOException("Response exceeds maxresponsebytes=" + max)`를 던진다.
    - 읽은 바이트는 UTF-8로 디코딩한다.
  - 상태코드별 SQLState: 401/403은 `28000`, 404는 `08001`, 5xx는 `08S01`, 그 외는 `HY000`이다. 메시지는 `"HTTP " + code + ": " + 본문 앞 1000자`로 만든다.
  - `ACCEPT` 헤더를 두 번 `setHeader`하던 부분은 위 값으로 한 번만 설정한다. GET 요청에서는 `Content-Type`을 빼고, POST 요청에만 둔다.
- [ ] **Step 4: 통과하는지 확인한다.** `mvn -q -o test -Dtest=*FixTest`를 실행해 전부 PASS인지 본다.
- [ ] **Step 5: 커밋한다.** `git commit -m "fix: default HTTP timeouts, response size limit, status-aware SQLState"`

---

### Task 4: 드라이버 통합과 수명 관리 (리뷰 🔴1 캐시 키, 🔴3, 🟡1, 🟡2)

**Files:**
- Create: `src/main/java/com/frotoma/jobc/web/AbstractHttpSparqlDriver.java`
- Modify: `web/WebSparqlDriver.java`, `fuseki/FusekiSparqlDriver.java` (중복 코드 제거, 상속)
- Modify: `JobcDriver.java`, `virt/VirtuosoSparqlDriver.java` (등록)
- Modify: `rest/APIMangerService.java` (참조 카운트)
- Modify: `web/WebSparqlConnection.java` (`close`/`isClosed`/`checkOpen`)
- Modify: `web/WebSparqlStatement.java` (`executeQuery`/`executeUpdate` 앞에서 `checkOpen`)
- Test: `src/test/java/com/frotoma/jobc/DriverLifecycleFixTest.java`

**Interfaces:**
- Produces: `public abstract class AbstractHttpSparqlDriver implements java.sql.Driver`
  - `protected abstract String urlPrefix()`
  - `protected abstract WebSparqlConnection newConnection(String serverUrl, APIMangerService api)`
  - `public final Connection connect(String url, Properties info)`:
    1. `acceptsURL`를 확인한다(null-safe).
    2. 가장 먼저 `apiServices.values().removeIf(APIMangerService::isClosed)`를 실행한다.
    3. `apiServices.compute(key, ...)`에서 기존 값이 없거나 닫혀 있으면 새로 만든다.
    4. 그 값에 `retain()`을 호출한 뒤 `newConnection`을 호출한다.
  - `static Properties urlToInfo(String endpoint, Properties info)`:
    - 키는 소문자로 바꾼다. `&` 뒤의 각 항목을 `split("=", 2)`로 나누고, 값은 `URLDecoder.decode(v, UTF_8)`로 디코딩한다.
    - URL 쪽 값이 `info`보다 우선한다.
  - `static String stripParams(String endpoint)`
  - `static String cacheKey(String serverUrl, Properties props)`: `serverUrl + "#" + SHA-256 hex(정렬된 key=value를 \n으로 연결)`
  - `getPropertyInfo`는 `new DriverPropertyInfo[0]`을, `getParentLogger`는 `java.util.logging.Logger.getLogger("com.frotoma.jobc")`를 돌려준다.
- Produces: `APIMangerService.retain()`, `release()`, `isClosed()`. 모두 `synchronized`다. `release()`로 카운트가 0이 되면 `close()`를 호출한다.
- Produces: `WebSparqlConnection.close()`(멱등, `apiManager.release()` 호출), `isClosed()`, package-private `void checkOpen() throws SQLException`(SQLState `08003`)
- Produces: `public static void JobcDriver.ensureRegistered()`. 본문은 비어 있다. 호출만으로 `JobcDriver`의 클래스 초기화(=등록 1회)를 강제한다.
  - Fuseki/Web/Virtuoso 하위 드라이버의 static 블록은 `DriverManager.registerDriver(...)` 대신 `JobcDriver.ensureRegistered()`를 호출한다.
  - 기존 테스트가 `Class.forName("...FusekiSparqlDriver")`를 쓰므로 이 경로도 계속 동작해야 한다.

- [ ] **Step 1: 실패하는 테스트를 작성한다.**

```java
@Test public void urlPasswordWithEqualsAndPercentIsDecoded()
    // "jobc:fuseki:"+base+"/ds/sparql&user=u&password=a%3Db=c" 로 쿼리
    // → Authorization == "Basic " + base64("u:a=b=c")
@Test public void cacheKeyHidesPassword()
    // cacheKey("http://h/ds", {password=topsecret}) 에 "topsecret" 이 없다
@Test public void poolClosedOnLastClose()
    // c1, c2 같은 URL → 같은 api. c1.close() 후 !api.isClosed(); c2.close() 후 api.isClosed()
    // 다시 connect → 다른 api 인스턴스
@Test public void closeIsIdempotent()
    // close() 2번 → 예외 없음, 다른 열린 연결의 api 는 열린 상태 유지
@Test public void closedConnectionRejectsWork()
    // close 후 createStatement() → SQLException state "08003"
    // close 전에 만든 statement 의 executeQuery → "08003"
@Test public void driverRegisteredOnce() throws Exception
    // Class.forName("com.frotoma.jobc.fuseki.FusekiSparqlDriver");
    // DriverManager.drivers() 중 acceptsURL("jobc:fuseki:http://x/ds") 가 true 인 것 == 1개
@Test public void propertyInfoNotNull()
    // new JobcDriver().getPropertyInfo("jobc:fuseki:http://x", new Properties()).length == 0
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다.** `mvn -q -o test -Dtest=DriverLifecycleFixTest`
- [ ] **Step 3: 구현한다.** 위 Interfaces대로 구현한다. `WebSparqlDriver`와 `FusekiSparqlDriver`에는 `urlPrefix()`와 `newConnection()`만 남긴다. `JobcDriver`의 버전 상수는 하위 드라이버가 참조할 수 있도록 `public static final`로 바꾼다.
- [ ] **Step 4: 통과하는지 확인한다.** `mvn -q -o test -Dtest=*FixTest`를 실행한다. 기존 `connectionsWithDifferentUrlsDoNotShareSettings`(열린 a, a2가 같은 api)도 PASS여야 한다.
- [ ] **Step 5: 커밋한다.** `git commit -m "fix: close HTTP pool on last connection close, single driver registration"`

---

### Task 5: PreparedStatement 파라미터 바인딩 (리뷰 🟠 표 1–3행)

**Files:**
- Modify: `src/main/java/com/frotoma/jobc/builder/PrepareSparqlBuilder.java`
- Modify: `web/WebSparqlPreparedStatement.java`, `fuseki/FusekiSparqlPreparedStatement.java`. 수정 대상은 `setNull` 2개와 `clearParameters`다.
- Test: `src/test/java/com/frotoma/jobc/builder/PrepareSparqlBuilderFixTest.java`

**Interfaces:**
- Produces: `public void PrepareSparqlBuilder.addParameter(int idx, Object obj) throws SQLException`가 유일한 진입점이 된다. 타입별 오버로드는 삭제한다. 타입별 처리 순서는 다음과 같다.

| 입력 | 바인딩 |
|---|---|
| `idx < 1` | `SQLException("Parameter index out of range: "+idx, "07009")` |
| `null` | `SQLException("SPARQL has no NULL value (parameter "+idx+")", "22004")` |
| `String` | plain literal (`pss.setLiteral(i, String)`) |
| `Boolean, Byte, Short, Integer, Long, Float, Double` | `ResourceFactory.createTypedLiteral(obj)` |
| `BigInteger` | `xsd:integer`, `toString()` |
| `BigDecimal` | `xsd:decimal`, `toPlainString()` |
| `java.sql.Date` | `xsd:date`, `toLocalDate().toString()` (`java.util.Date`보다 먼저 검사) |
| `java.sql.Time` | `xsd:time`, `toLocalTime().toString()` |
| `java.sql.Timestamp` | `xsd:dateTime`, `toLocalDateTime().toString()` |
| `java.util.Date` | `xsd:dateTime`, `toInstant().toString()` |
| `LocalDate` / `LocalDateTime` / `OffsetDateTime` | `xsd:date` / `xsd:dateTime` / `xsd:dateTime`, `toString()` |
| `Calendar` | `pss.setLiteral(i, Calendar)` |
| `URL` | `xsd:anyURI` literal (기존 동작 유지) |
| `IRIx` | `pss.setIri` |
| `RDFNode` / `Node` | `pss.setParam` |
| `LiteralObj` | `lang`이 있으면 lang literal, `datatype`이 있으면 typed literal, 둘 다 없으면 plain. 모두 `getLexicalForm()`을 쓴다. |
| 그 외 (`byte[]`, 스트림, `Reader`, `Blob` 등) | `SQLFeatureNotSupportedException("Unsupported parameter type: "+cls.getName())` |

- Produces: `public void PrepareSparqlBuilder.clearParameters()` (`pss.clearParams()`)
- `setNull(...)` 두 오버로드는 `addParameter(idx, null)`을 호출해 위 22004 예외를 그대로 받는다. `clearParameters()`는 `builder.clearParameters()`를 호출한다.

- [ ] **Step 1: 실패하는 테스트를 작성한다.**

```java
@Test public void injectionStaysOneLiteral() throws Exception {
    PrepareSparqlBuilder b = new PrepareSparqlBuilder("INSERT DATA { <http://ex/s> <http://ex/p> ? }");
    b.addParameter(1, "x\" } ; DROP ALL ; INSERT DATA { <http://a> <http://b> \"y");
    UpdateRequest req = UpdateFactory.create(b.query());
    assertEquals(1, req.getOperations().size());
}
@Test public void byteBindsTyped()       // addParameter(1,(byte)5): query 에 "\"5\"" 와 "XMLSchema#byte" 포함
@Test public void shortBindsTyped()      // (short)7 → "XMLSchema#short"
@Test public void bigDecimalBinds()      // new BigDecimal("1.50") → "\"1.50\"" 와 "XMLSchema#decimal"
@Test public void sqlDateBinds()         // Date.valueOf("2026-11-16") → "\"2026-11-16\"" 와 "XMLSchema#date"
@Test public void timestampBinds()       // Timestamp.valueOf("2026-11-16 10:20:30") → "\"2026-11-16T10:20:30\"" 와 "XMLSchema#dateTime"
@Test public void localDateBinds()       // LocalDate.of(2026,11,16) → "XMLSchema#date"
@Test public void nullRejected()         // SQLException, getSQLState()=="22004"
@Test public void indexZeroRejected()    // SQLException, "07009"
@Test public void bytesUnsupported()     // new byte[]{1} → SQLFeatureNotSupportedException
@Test public void clearParametersRemovesValue() // "abc" 바인딩 → clearParameters() → query 에 "abc" 없음
@Test public void webPreparedSetByteEndToEnd()  // WebSparqlPreparedStatement + MockSparqlServer: setByte 후 executeQuery 성공
@Test public void preparedSetNullThrows()       // Fuseki prepareStatement(..).setNull(1, Types.VARCHAR) → SQLException "22004"
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다.** `mvn -q -o test -Dtest=PrepareSparqlBuilderFixTest`
- [ ] **Step 3: 위 표대로 구현한다.**
- [ ] **Step 4: 통과하는지 확인한다.** `mvn -q -o test -Dtest=*FixTest`를 실행하고, `mvn -q -o test-compile`도 성공해야 한다. 라이브 테스트가 타입별 오버로드를 직접 호출한다면 `Object` 시그니처로 그대로 컴파일된다.
- [ ] **Step 5: 커밋한다.** `git commit -m "fix: type-safe SPARQL parameter binding, reject NULL, clearParameters"`

---

### Task 6: ResultSet 값 변환과 LiteralObj 직렬화 (리뷰 🟠 표 5–6행, 🟡6)

**Files:**
- Modify: `web/WebSparqlResultset.java` (`wasNull`, 숫자/불리언 getter)
- Modify: `web/WebSparqlStatement.java:134-142` (updateCount)
- Modify: `obj/LiteralObj.java:146-155` (`toString`)
- Test: `src/test/java/com/frotoma/jobc/ResultSetFixTest.java`

**Interfaces:**
- `WebSparqlResultset`:
  - `getObjectValue`가 `lastWasNull = (obj == null)`을 기록하고, `wasNull()`이 이 값을 돌려준다.
  - `getInt`, `getLong`, `getShort`, `getByte`, `getFloat`, `getDouble`, `getBigDecimal`, `getBoolean`의 규칙:
    - 값이 `null`이면 `0`/`false`/`null`을 돌려준다.
    - 정수형은 `toBigDecimal(obj).setScale(0, RoundingMode.DOWN).xxxValueExact()`로 변환한다. 범위를 넘으면 `SQLException(.., "22003")`을 던진다.
    - 실수형은 `Number.doubleValue()`/`floatValue()`를 쓴다.
    - 파싱에 실패하면 `SQLException(.., "22018")`을 던진다.
    - `getBoolean`은 `Boolean`이면 그대로 쓰고, 문자열이면 `"true"`/`"1"`만 true로 본다.
- `WebSparqlStatement.executeUpdate`: 값이 `Number`면 `intValue()`를, 아니면 `Integer.parseInt(toString().trim())`을 쓴다.
- `LiteralObj.toString()`:
  - 값 부분은 `lexicalForm`을 쓴다. `\`, `"`, `\n`, `\r`을 차례로 `\\`, `\"`, `\n`, `\r`로 이스케이프한다(`replace` 사용, `replaceAll` 사용 금지).
  - `datatype`에 `"://"`가 있으면 `<...>`로 감싸고, 아니면 그대로 쓴다.
  - 그 뒤에 `@lang`을 붙인다.

- [ ] **Step 1: 실패하는 테스트를 작성한다.** `MockSparqlServer` 응답 JSON 기준이다. 변수는 `n`(바인딩됨, `"007"^^xsd:integer`), `m`(바인딩 없음), `b`(`"true"^^xsd:boolean`), `d`(`"1.5"^^xsd:decimal`), `big`(`"12345678901234567890"^^xsd:integer`)이다.

```java
@Test public void unboundIntIsZeroAndWasNull()   // getInt("m")==0 && wasNull()
@Test public void boundIntClearsWasNull()        // getInt("m") 후 getInt("n")==7 && !wasNull()
@Test public void unboundBooleanFalse()          // getBoolean("m")==false && wasNull()
@Test public void booleanTyped()                 // getBoolean("b")==true
@Test public void decimalAsDouble()              // getDouble("d")==1.5
@Test public void longFromInteger()              // getLong("n")==7L
@Test public void intOverflowIs22003()           // getInt("big") → SQLException "22003"
@Test public void updateCountFromLong()          // executeUpdate 응답 count="5"^^xsd:long → 5 (예전에는 ClassCastException)
@Test public void literalToStringEscapes() {
    assertEquals("\"a\\\"b\\\\c\"^^<http://www.w3.org/2001/XMLSchema#string>",
        new LiteralObj("a\"b\\c", null, XSD + "string").toString());
}
@Test public void literalToStringKeepsLexical()  // new LiteralObj("007",null,XSD+"integer").toString() == "\"007\"^^<"+XSD+"integer>"
@Test public void literalToStringLang()          // new LiteralObj("화성","ko",null).toString() == "\"화성\"@ko"
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다.** `mvn -q -o test -Dtest=ResultSetFixTest`
- [ ] **Step 3: 구현한다.**
- [ ] **Step 4: 통과하는지 확인한다.** `mvn -q -o test -Dtest=*FixTest`
- [ ] **Step 5: 커밋한다.** `git commit -m "fix: JDBC null semantics and exact numeric conversion in ResultSet"`

---

### Task 7: Connection/Statement JDBC 규약 (리뷰 🟠 표 4, 7–10행)

**Files:**
- Modify: `web/WebSparqlConnection.java`
- Modify: `web/WebSparqlStatement.java`
- Modify: `fuseki/FusekiSparqlStatement.java` (타임아웃 전달)
- Modify: `web/WebSparqlPreparedStatement.java`, `fuseki/FusekiSparqlPreparedStatement.java` (batch 위임)
- Test: `src/test/java/com/frotoma/jobc/JdbcConformanceFixTest.java`

**Interfaces:**
- Consumes: Task 3의 `get/post(url, param, int timeoutMillis)`, Task 4의 `checkOpen()`
- `WebSparqlConnection`:
  - 오버로드 위임:
    - `createStatement(int,int)`와 `createStatement(int,int,int)`는 `this.createStatement()`를 호출한다.
    - `prepareStatement(sql, …)` 5개 오버로드는 모두 `this.prepareStatement(sql)`을 호출한다.
    - 이렇게 하면 `FusekiSparqlConnection`은 따로 고칠 필요 없이 Fuseki용 타입을 돌려주게 된다.
  - 다음은 `SQLFeatureNotSupportedException`을 던진다: `prepareCall`×3, `createClob`, `createBlob`, `createNClob`, `createSQLXML`, `createArrayOf`, `createStruct`, `setSavepoint`×2, `rollback(Savepoint)`, `releaseSavepoint`, `getMetaData`.
  - `nativeSQL(sql)`은 `sql`을 그대로 돌려준다.
  - `unwrap`/`isWrapperFor`는 `iface.isInstance(this)` 기준으로 동작하고, 해당하지 않으면 `unwrap`이 `SQLException`을 던진다.
  - `getClientInfo()`는 `new Properties()`를 돌려준다. `getTransactionIsolation()`은 `TRANSACTION_NONE`을 돌려준다. `abort(executor)`는 `close()`를 호출한다.
  - 네트워크 타임아웃:
    - `setNetworkTimeout(executor, ms)`: `ms < 0`이면 `SQLException`을 던지고, 아니면 `private int networkTimeoutMillis`에 저장한다.
    - `getNetworkTimeout()`: 저장된 값을 돌려준다.
- `WebSparqlStatement`:
  - 쿼리 타임아웃: `int queryTimeoutSeconds` 필드를 둔다. `setQueryTimeout`은 음수면 `SQLException`을 던진다.
  - `protected int effectiveTimeoutMillis()`: `queryTimeoutSeconds > 0`이면 `queryTimeoutSeconds * 1000`, 아니면 `connection.getNetworkTimeout()`을 돌려준다. 이 값을 Task 3의 `get/post`에 넘긴다(Fuseki 포함).
  - 배치:
    - `private final List<String> batch`를 둔다. `addBatch`는 큐에 쌓기만 하고 즉시 실행하지 않는다. `clearBatch`는 큐를 비운다.
    - `executeBatch`는 큐를 차례로 `executeUpdate`하고 각 결과를 `int[]`에 담는다.
    - i번째에서 `SQLException e`가 나면 `BatchUpdateException(e.getMessage(), e.getSQLState(), Arrays.copyOf(counts, i), e)`를 던진다.
    - 성공하든 실패하든 끝나면 큐를 비운다.
  - `close()`는 closed 플래그를 세우고, `isClosed()`가 이 플래그를 돌려준다. 닫힌 Statement로 실행하면 `SQLException`을 던진다.
  - 결과셋 속성: `getResultSetType`은 `TYPE_FORWARD_ONLY`, `getResultSetConcurrency`는 `CONCUR_READ_ONLY`, `getResultSetHoldability`는 `CLOSE_CURSORS_AT_COMMIT`를 돌려준다.
  - 나머지 `UnsupportedOperationException`은 모두 `SQLFeatureNotSupportedException`으로 바꾼다.
- PreparedStatement(Web/Fuseki): `addBatch()`는 `statement.addBatch(builder.query())`를, `executeBatch()`는 `statement.executeBatch()`를 호출한다. Fuseki의 자체 `batchQuery` 리스트는 삭제한다.

- [ ] **Step 1: 실패하는 테스트를 작성한다.**

```java
@Test public void fusekiTypedOverloadUsesUpdateEndpoint()
    // conn.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY).executeUpdate("INSERT DATA { <http://a> <http://b> 1 }")
    // → lastPath()=="/ds/update", lastBody() 가 "update=" 로 시작
@Test public void threeArgPrepareNotNull()       // prepareStatement(sql, 1003, 1007, 1) != null
@Test public void prepareCallUnsupported()       // SQLFeatureNotSupportedException
@Test public void networkTimeoutRoundTrip()      // setNetworkTimeout(null, 1234); getNetworkTimeout()==1234
@Test public void queryTimeoutApplied()          // delayMillis(3000); setQueryTimeout(1) → SQLException, 경과 < 2500ms
@Test public void batchQueuesUntilExecute()      // addBatch×2 → requestCount()==0; executeBatch() 길이 2, requestCount()==2
@Test public void batchFailure()                 // respond(500,...) → BatchUpdateException, 이후 executeBatch() 길이 0
@Test public void unwrapSelf()                   // conn.unwrap(WebSparqlConnection.class) == conn
@Test public void hikariPoolSmoke() throws Exception
    // HikariConfig: jdbcUrl "jobc:fuseki:"+base+"/ds/sparql", driverClassName "com.frotoma.jobc.JobcDriver", maximumPoolSize 2
    // try (HikariDataSource ds = ...; Connection c = ds.getConnection()) { c.createStatement().executeQuery("ASK {}"); }
    // 예외 없이 끝나야 한다
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다.** `mvn -q -o test -Dtest=JdbcConformanceFixTest`
- [ ] **Step 3: 구현한다.**
- [ ] **Step 4: 통과하는지 확인한다.** `mvn -q -o test -Dtest=*FixTest`
- [ ] **Step 5: 커밋한다.** `git commit -m "fix: JDBC conformance for overloads, timeouts, batch, unsupported features"`

---

### Task 8: Virtuoso 래퍼 정리 (리뷰 🟠 표 마지막 행)

**Files:**
- Modify: `virt/VirtuosoConnection.java:180,190` (`createStatement(int,int[,int])`)
- Modify: `virt/VirtuosoPreparedStatement.java` (`setNull`×2, `clearParameters`)
- Test: `src/test/java/com/frotoma/jobc/virt/VirtuosoWrapperFixTest.java`

**Interfaces:**
- Consumes: Task 5의 `addParameter(int, Object) throws SQLException`, `clearParameters()`
- `createStatement(t, c)`와 `createStatement(t, c, h)`는 `new VirtuosoStatement(delegate.createStatement(...))`를 돌려준다.
- `setNull`은 `queryBuilder.addParameter(idx, null)`을 호출해 22004 예외를 받는다. `clearParameters`는 `queryBuilder.clearParameters()`를 호출한다.

- [ ] **Step 1: 실패하는 테스트를 작성한다.** 가짜 `Connection`/`Statement`는 `java.lang.reflect.Proxy`로 만든다. `createStatement`는 가짜 Statement를 돌려주고, `execute*`는 넘어온 SQL 문자열을 `List<String> executed`에 기록한다.

```java
@Test public void typedCreateStatementAddsSparqlPrefix()
    // new VirtuosoConnection(fake).createStatement(1003,1007).executeQuery("SELECT * WHERE {?s ?p ?o}")
    // → executed.get(0) 가 "SPARQL SELECT" 로 시작
@Test public void preparedStringEscaped()
    // prepareStatement("SELECT ?s WHERE { ?s <http://ex/p> ? }").setString(1, "x\" } DROP ALL #") 후 executeQuery
    // → executed.get(0) 에 "x\\\" } DROP ALL #" 포함 (따옴표가 이스케이프됨)
@Test public void clearParametersWorks()         // setString(1,"abc"); clearParameters(); executeQuery → executed 에 "abc" 없음
@Test public void setNullThrows()                // SQLException "22004"
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다.** `mvn -q -o test -Dtest=VirtuosoWrapperFixTest`
- [ ] **Step 3: 구현한다.**
- [ ] **Step 4: 통과하는지 확인한다.** `mvn -q -o test -Dtest=*FixTest`
- [ ] **Step 5: 커밋한다.** `git commit -m "fix: Virtuoso wrapper prefixes all statements, clears params"`

---

### Task 9: 마무리

**Files:**
- Modify: `README.md` (호환성 변경 섹션 추가). 버전은 `0.0.2` 유지.

- [ ] **Step 1: `README.md`에 "변경사항(보안·안정성 보강)" 섹션을 추가한다.** 아래 내용을 각 한 줄로 적는다.
  - `setNull` 예외
  - 레거시 클래스 7개 삭제와 `getPassword()`/`getUser()` 제거
  - HTTP 기본 타임아웃 값
  - `maxresponsebytes` 속성
  - `logback-classic` test scope 전환(사용하는 앱은 자체 SLF4J 바인딩이 필요함)
  - `Connection.close()`가 이제 실제로 자원을 해제한다는 점
- [ ] **Step 3: 전체 검증을 한다.**
  - `mvn -q -o test-compile`이 성공해야 한다.
  - `mvn -q -o test -Dtest=*FixTest`가 실패 0으로 끝나야 한다(기존 11개 + 신규 테스트).
  - `mvn -q -o dependency:tree -Dscope=runtime`에 `logback-classic`이 나오지 않아야 한다.
- [ ] **Step 4: 커밋한다.** `git commit -m "docs: document hardening changes"`
