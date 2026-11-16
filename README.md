# jobc-driver

Jobc (java ontology base connection)

LOD, Apache Jena Fuseki, Virtuoso can be used in Java just like JDBC.

### Features
- Execute Sparql on ontology repositories by coding like a general Database Connection.
- PoolManager is available. (LOD and Fuseki do not manage the actual pool).
- Can be used like Mybatis.
- The same Sparql can be used in LOD, Jena fuseki, and Virtuoso. When using Preparedstatement, '? ' is used as a variable in Sparql just like in Sql.
- When using preparedstatement in Virtuoso, Sparql uses a different method.
    - e.g.) Use parameters as '??' or use 'INSERT INTO GRAPH' for 'INSERT Data'.
- Generally, LOD does not support updates.

###  JobcDriver Attrebute Value
Divier Name : com.frotoma.jobc.JobcDriver
URL : Ontology Storage Address
user : User ID
password : User password

### URL Attrebute Value
jobc:[Ontology Storage Type]:[Ontology Storage Address]&method=[HTTP Verbs]
* Ontology Storage Type
    * LOD : web
    * Fuseki : fuseki
    * Virtuoso : virtuoso
* Ontology Storage Address
    * LOD : web address 
        * ex) 
        ```
        http://localhost:8080/sparql
        ```        
    * Fuseki : Web address
        * ex) 
        ```
        http://localhost:3030/jobc/sparql
        ```
    * Virutoso : address
        * ex) 
        ```
        localhost:1111/CHARSET=UTF-8
        ```
* HTTP Verbs : API HTTP Method
    * ex) get, post etc...
* Full Example
    * LOD : jobc:web:http://localhost:3030/jobc/sparql&method=post
    * Fuseki : jobc:fuseki:http://localhost:3030/jobc/sparql&method=post
    * Virtuoso : jobc:virtuoso://localhost:1111/CHARSET=UTF-8

### HTTP Connection Properties (web, fuseki)
Set as JDBC properties or appended to the URL as `&key=value` (URL values win; values are URL-decoded).

| Property | Default | Meaning |
|---|---|---|
| connecttimeout | 5000 | TCP connect timeout (ms) |
| connectionrequesttimeout | 10000 | Wait for a pooled connection (ms) |
| sockettimeout | 60000 | Wait for response data (ms). `Statement.setQueryTimeout` / `Connection.setNetworkTimeout` override it per request |
| maxresponsebytes | 67108864 | Responses larger than this (64 MiB) fail instead of exhausting memory |

### Changes (security & stability hardening)
- `PreparedStatement.setNull()` now throws `SQLException` (SQLState 22004). SPARQL has no NULL; it used to bind an empty string silently.
- Unsupported parameter types (`byte[]`, streams, `Blob` …) throw `SQLFeatureNotSupportedException`. `Date`, `Timestamp`, `BigDecimal`, `java.time` types are bound as typed xsd literals.
- Removed `APIMangerService.getPassword()/getUser()`; `getProperty("password")` returns null. Passwords are masked in logs.
- Removed unused legacy classes: `PrepareQueryBuilder`, `VirtuosoPreparedSparqlBuilder`, `VirtuosoConnection4pstmt`, `VirtuosoPreparedStatement4psmt`, `APISelectGetService`, `APISelectPostService`, `APIUpdatePostFusekiService`, `AbstractAPIService`.
- `logback-classic` is no longer a runtime dependency. Applications must provide their own SLF4J binding.
- `Connection.close()` now releases resources: the HTTP pool closes when the last connection using it is closed.
- Default HTTP timeouts and `maxresponsebytes` (see table above). HTTP errors map to SQLStates (401/403 → 28000, 5xx → 08S01).
- `Connection.getMetaData()` throws `SQLFeatureNotSupportedException` instead of returning null.
- `xsd:gYearMonth` → `java.time.YearMonth`, `xsd:gMonthDay` → `java.time.MonthDay`, `xsd:gMonth`/`gDay`/`gYear` → `Integer`.
- `ResultSet` numeric getters return 0 and `wasNull()` is true for unbound variables; out-of-range values throw (SQLState 22003).

### Preparedstatement
- The code is identical to using JDBC.
- '?' is used as a variable in Sparql.


### Preparedstatement Example
```java
        String preparedSql = 
            "SELECT *  WHERE {  " +
            "  Filter( ?person = ? ) . " +
            "  ?person ? ?member . " +
            "  ?member <http://dbpedia.org/property/name> ?membername ." +
            "}  ";

        String DRIVER = "com.frotoma.jobc.JobcDriver";
        String URL = "jobc:web:http://dbpedia.org/sparql/";
        String user = null;
        String password = null;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            DataSource dataSource = crateDataSource();
            conn = dataSource.getConnection();            
            pstmt = conn.prepareStatement(preparedSql);

            pstmt.setObject(1, ParamBuilder.createIRI("http://dbpedia.org/resource/BTS"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://dbpedia.org/ontology/bandMember") );
            //stmt.setString(2, "South Korea");
            
            rs = pstmt.executeQuery();            
            
            ResultSetMetaData rsm = pstmt.getMetaData();
            int columnCount = rsm.getColumnCount();
            
            
            System.out.println("============================================== ");
            
            System.out.print("NUM");
            for( int i = 0; i< columnCount; i++ ){
                System.out.print( "\t" );
                System.out.print( rsm.getColumnLabel(i+1) );                    
            }
            System.out.println("");            
            System.out.println("---------------------------------------------- ");

            int num = 0;
            while (rs.next()) {
                System.out.print( num );
                for( int i = 0; i< columnCount; i++ ){
                    System.out.print( "\t" );
                    System.out.print( rs.getString(i+1) );
                }
                System.out.println("");
                num++;
            }

            rs.close();
            pstmt.close();
            conn.close();

        } catch (Exception e) {
            try {
                rs.close();
                pstmt.close();
                conn.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
```

### Contact
* Email : jongearl@frotoma.com
* Company : [프로토마](http://www.frotoma.com)

