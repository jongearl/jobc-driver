# Jobc-driver

Jobc(Java Ontology Base Connection) Driver
LOD, Apache Jena Fuseki, Virtuoso can be used in Java just like JDBC.

### Features
- Execute Sparql on ontology repositories by coding like a general Database Connection.
- PoolManager is available. (LOD and Fuseki do not manage the actual pool).
- Can be used like Mybatis.
- The same Sparql can be used in LOD, Jena fuseki, and Virtuoso. When using Preparedstatement, '? ' is used as a variable in Sparql just like in Sql.
- When using preparedstatement in Virtuoso, Sparql uses a different method.
    - e.g.) Use parameters as '??' or use 'INSERT INTO GRAPH' for 'INSERT Data'.

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

### Preparedstatement
- The code is identical to using JDBC.
- '?' is used as a variable in Sparql.


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


### Contact
* Email : jongearl@frotoma.com
* Company : [프로토마](http://www.frotoma.com)

