# jobc-driver

LOD, Apache Jena Fuseki, Virtuoso를 Java에서 JDBC처럼 사용 할 수 있는 라이브러리입니다.

### 특징
- 일반적인 Database Connection처럼 코딩을 해서 온톨로지 저장소에 Sparql를 수행합니다.
- PoolManager를 사용할 수 있습니다.
    - LOD와 Fuseki는 실제 Pool관리는 하지 않습니다.
- Mybatis처럼 사용할 수 있습니다.
-  동일한 Sparql을 LOD, Jena fuseki, Virtuoso에서 사용 가능합니다.
    -  Preparedstatement를 사용할때 Sql과 동일하게 Sparql의 변수를 ? 로 사용합니다.
    -  Virtuoso에서 preparedstatement를 사용할 때 Sparql은 다른 방법을 사용.
    -  예) 파라메터를 ?? 로 사용하거나 INSERT Data를 INSERT INTO GRAPH 사용

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
        * ex) http://localhost:8080/sparql
    * Fuseki : Web address
        * ex) http://localhost:3030/jobc/sparql
    * Virutoso : address
        * ex) localhost:1111/CHARSET=UTF-8
* HTTP Verbs : API HTTP Method
    * ex) get, post etc...
* Full Example
    * LOD : jobc:web:http://localhost:3030/jobc/sparql&method=post
    * Fuseki : jobc:fuseki:http://localhost:3030/jobc/sparql&method=post
    * Virtuoso : jobc:virtuoso://localhost:1111/CHARSET=UTF-8

### Preparedstatement
코드는 JDBC를 사용할때와 동일
Sparql의 변수는 '?' 를 사용


### Preparedstatement Example
```java
    String DRIVER = "com.frotoma.jobc.JobcDriver";
        String URL = "jobc:web:http://dbpedia.org/sparql/";
        String user = null;
        String password = null;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException ex) {
            System.getLogger(DBPediaTest.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        String preparedSql = "SELECT ?person ?value\n"
            + " WHERE {     \n"
            + "  Filter( ?person = ? ) .     \n"
            + "  ?person <http://dbpedia.org/property/origin> ?value .\n"
            + "  FILTER ( CONTAINS( STR( ?value)  , ? ) )\n"
            + "}  ";

        try {
            conn = DriverManager.getConnection(URL, user, password);
            stmt = conn.prepareStatement(preparedSql);

            stmt.setObject(1, ParamBuilder.createIRI("http://dbpedia.org/resource/BTS"));
            stmt.setString(2, "South Korea");

            rs = stmt.executeQuery();

            System.out.println("================ Result ====================");

            ResultSetMetaData rsmd = rs.getMetaData();

            while (rs.next()) {
                System.out.print(rsmd.getColumnLabel(1) + " : " + rs.getString(1));
                System.out.println("\t");
                System.out.print(rsmd.getColumnLabel(2) + " : " + rs.getString(2));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
```

### TODO
Virutoso PreparedStatement : Use PreparedStatement of Virtuoso Driver 


