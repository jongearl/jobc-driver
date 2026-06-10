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

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            DataSource dataSource = crateDataSource();
            connection = dataSource.getConnection();            
            stmt = connection.prepareStatement(preparedSql);

            stmt.setObject(1, ParamBuilder.createIRI("http://dbpedia.org/resource/BTS"));
            stmt.setObject(2, ParamBuilder.createIRI("http://dbpedia.org/ontology/bandMember") );
            //stmt.setString(2, "South Korea");
            
            rs = stmt.executeQuery();            
            
            ResultSetMetaData rsm = stmt.getMetaData();
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

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
```

### Contact
* Email : jongearl@frotoma.com
* Company : [프로토마](http://www.frotoma.com)

