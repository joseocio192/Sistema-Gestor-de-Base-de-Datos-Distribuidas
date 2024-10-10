package two_phase_commit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mysql.cj.protocol.Resultset;

import base_de_datos.DatabaseModelMysql;
import base_de_datos.DatabaseModelPostgres;
import base_de_datos.DatabaseModelSQLServer;
import errors.ErrorHandler;

public class SQLparser {

    private static final String ZONA_NORTE = "Baja California";
    private static final String ZONA_CENTRO = "Jalisco";
    private static final String ZONA_SUR = "Chiapas";

    private Connection connessioneATablaCheHaFrammenti;
    private Connection conexionNorte;
    private Connection conexionCentro;
    private Connection conexionSur;

    private List<Map<String, Object>> resultadosNorte;
    private List<Map<String, Object>> resultadoCentro;
    private List<Map<String, Object>> resultadosSur;

    public SQLparser(Connection connessioneATablaCheHaFrammenti) {
        this.connessioneATablaCheHaFrammenti = connessioneATablaCheHaFrammenti;

    }

    public List<String> parseQuery(String query) {
        List<String> targetFragments = new ArrayList<>();

        // Check if the query contains a WHERE clause
        Pattern wherePattern = Pattern.compile("(?i)\\bWHERE\\b");
        Matcher whereMatcher = wherePattern.matcher(query);

        // Check if the query contains a insert clause
        Pattern insertPattern = Pattern.compile("(?i)\\bINSERT\\b");
        Matcher insertMatcher = insertPattern.matcher(query);

        if (insertMatcher.find()) {
            // get the zone from the insert clause
            if (query.toLowerCase().contains(ZONA_NORTE.toLowerCase())) {
                targetFragments.add(ZONA_NORTE);
            }
            if (query.toLowerCase().contains(ZONA_CENTRO.toLowerCase())) {
                targetFragments.add(ZONA_CENTRO);
            }
            if (query.toLowerCase().contains(ZONA_SUR.toLowerCase())) {
                targetFragments.add(ZONA_SUR);
            }
            return targetFragments;
        } else {
            if (!whereMatcher.find()) {
                // No WHERE clause, send to all fragments
                targetFragments.add(ZONA_NORTE);
                targetFragments.add(ZONA_CENTRO);
                targetFragments.add(ZONA_SUR);
            } else {
                // Check if the WHERE clause contains 'Estado'
                Pattern estadoPattern = Pattern.compile("(?i)\\bWHERE\\b.*\\bEstado\\b");
                Matcher estadoMatcher = estadoPattern.matcher(query);

                if (estadoMatcher.find()) {
                    // Check for specific zone conditions
                    if (query.toLowerCase().contains(ZONA_NORTE.toLowerCase())) {
                        targetFragments.add(ZONA_NORTE);
                    }
                    if (query.toLowerCase().contains(ZONA_CENTRO.toLowerCase())) {
                        targetFragments.add(ZONA_CENTRO);
                    }
                    if (query.toLowerCase().contains(ZONA_SUR.toLowerCase())) {
                        targetFragments.add(ZONA_SUR);
                    }
                }

                // If no specific zone condition is found, send to all fragments
                if (targetFragments.isEmpty()) {
                    targetFragments.add(ZONA_NORTE);
                    targetFragments.add(ZONA_CENTRO);
                    targetFragments.add(ZONA_SUR);
                }
            }
            return targetFragments;
        }
    }

    public List<Map<String, Object>> ejecutarSelect(String sentencia) {
        List<String> targetFragments = parseQuery(sentencia);
        if (targetFragments.size() == 3) {
            creareConnessioni(true, null);
            resultadosNorte = prepararSentencia(sentencia, conexionNorte, targetFragments);
            resultadoCentro = prepararSentencia(sentencia, conexionCentro, targetFragments);
            resultadosSur = prepararSentencia(sentencia, conexionSur, targetFragments);
        }

        if (targetFragments.size() == 2) {
            if (targetFragments.contains(ZONA_NORTE) && targetFragments.contains(ZONA_CENTRO)) {
                creareConnessioni(true, "norte");
                creareConnessioni(true, "centro");
                resultadosNorte = prepararSentencia(sentencia, conexionNorte, targetFragments);
                resultadoCentro = prepararSentencia(sentencia, conexionCentro, targetFragments);
            }
            if (targetFragments.contains(ZONA_NORTE) && targetFragments.contains(ZONA_SUR)) {
                creareConnessioni(true, "norte");
                creareConnessioni(true, "sur");
                resultadosNorte = prepararSentencia(sentencia, conexionNorte, targetFragments);
                resultadosSur = prepararSentencia(sentencia, conexionSur, targetFragments);
            }
            if (targetFragments.contains(ZONA_CENTRO) && targetFragments.contains(ZONA_SUR)) {
                creareConnessioni(true, "centro");
                creareConnessioni(true, "sur");
                resultadoCentro = prepararSentencia(sentencia, conexionCentro, targetFragments);
                resultadosSur = prepararSentencia(sentencia, conexionSur, targetFragments);
            }
        }

        if (targetFragments.size() == 1) {
            if (targetFragments.contains(ZONA_NORTE)) {
                creareConnessioni(false, "norte");
                resultadosNorte = prepararSentencia(sentencia, conexionNorte, targetFragments);
            }
            if (targetFragments.contains(ZONA_CENTRO)) {
                creareConnessioni(false, "centro");
                resultadoCentro = prepararSentencia(sentencia, conexionCentro, targetFragments);
            }
            if (targetFragments.contains(ZONA_SUR)) {
                creareConnessioni(false, "sur");
                resultadosSur = prepararSentencia(sentencia, conexionSur, targetFragments);
            }
        }

        List<Map<String, Object>> resultados = new ArrayList<>();
        if (resultadosNorte != null) {
            resultados.addAll(resultadosNorte);
        }
        if (resultadoCentro != null) {
            resultados.addAll(resultadoCentro);
        }
        if (resultadosSur != null) {
            resultados.addAll(resultadosSur);
        }
        return resultados;

    }

    private void creareConnessioni(Boolean x, String y) {
        try {
            Statement statement = connessioneATablaCheHaFrammenti.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM fragmentos");
            while (resultSet.next()) {
                String fragmento = resultSet.getString("Fragmento");
                String criterioFrag = resultSet.getString("CriterioFrag");
                String servidor = resultSet.getString("IP");
                String gestor = resultSet.getString("gestor");
                String basededatos = resultSet.getString("basededatos");
                String usuario = resultSet.getString("usuario");
                String password = resultSet.getString("Contraseña");
                if (x) {
                    if (fragmento.equalsIgnoreCase(ZONA_NORTE)) {
                        conexionNorte = AsignandoConectionYgestor(servidor, gestor, basededatos, usuario, password);
                    } else if (fragmento.equalsIgnoreCase(ZONA_CENTRO)) {
                        conexionCentro = AsignandoConectionYgestor(servidor, gestor, basededatos, usuario, password);
                    } else if (fragmento.equalsIgnoreCase(ZONA_SUR)) {
                        conexionSur = AsignandoConectionYgestor(servidor, gestor, basededatos, usuario, password);
                    }    
                }else{
                    if (fragmento.equalsIgnoreCase(ZONA_NORTE) && y.equals("norte")) {
                        conexionNorte = AsignandoConectionYgestor(servidor, gestor, basededatos, usuario, password);
                    } else if (fragmento.equalsIgnoreCase(ZONA_CENTRO) && y.equals("centro")) {
                        conexionCentro = AsignandoConectionYgestor(servidor, gestor, basededatos, usuario, password);
                    } else if (fragmento.equalsIgnoreCase(ZONA_SUR) && y.equals("sur")) {
                        conexionSur = AsignandoConectionYgestor(servidor, gestor, basededatos, usuario, password);
                    }  
                }
                  
            }
        } catch (Exception e) {
            System.out.println("Error al crear las conexiones alv");
        }
    }

    private Connection AsignandoConectionYgestor(String servidor, String gestor, String basededatos, String usuario,
            String password) {
        if (gestor.equalsIgnoreCase("SQLServer")) {
            return new DatabaseModelSQLServer(servidor, basededatos, usuario, password)
                    .getConexion();
        }
        if (gestor.equalsIgnoreCase("MySQL")) {
            return new DatabaseModelMysql(servidor, basededatos, usuario, password).getConexion();
        }
        if (gestor.equalsIgnoreCase("Postgres")) {
            return new DatabaseModelPostgres(servidor, basededatos, usuario, password).getConexion();
        }
        System.err.println("Error al crear la conexión todo mal");
        return null;
    }

    public void ejecutarTransacion(String sentencia) throws SQLException {
        if (fasePreparacion(sentencia)) {
            faseCommit();
            System.out.println("Transacción completada con éxito");
        } else {
            faseAbort();
            System.err.println("Transacción abortada. Los cambios han sido revertidos.");
        }
    }

    private boolean fasePreparacion(String sentencia) {
        List<String> targetFragments = parseQuery(sentencia);
        try {
            if (targetFragments.contains(ZONA_NORTE)) {
                creareConnessioni(false, "norte");
                conexionNorte.setAutoCommit(false);
                resultadosNorte = prepararSentenciaNada(sentencia, conexionNorte, targetFragments);
            }
            if (targetFragments.contains(ZONA_CENTRO)) {
                creareConnessioni(false, "centro");
                conexionCentro.setAutoCommit(false);
                resultadoCentro = prepararSentenciaNada(sentencia, conexionCentro, targetFragments);
            }
            if (targetFragments.contains(ZONA_SUR)) {
                creareConnessioni(false, "sur");
                conexionSur.setAutoCommit(false);
                resultadosSur = prepararSentenciaNada(sentencia, conexionSur, targetFragments);
            }
            return true;
        } catch (SQLException e) {
            ErrorHandler.showMessage("Error en la transacción: " + e.getMessage(), "Error de conexión",
                    ErrorHandler.ERROR_MESSAGE);
            return false;
        }
    }

    private void faseCommit() {
        try {
            if (conexionNorte != null) {
                conexionNorte.commit();
            }
            if (conexionCentro != null) {
                conexionCentro.commit();
            }
            if (conexionSur != null) {
                conexionSur.commit();
            }
        } catch (Exception e) {
            if (conexionNorte != null) {
                try {
                    conexionNorte.rollback();
                } catch (SQLException e1) {
                    ErrorHandler.showMessage("Error al realizar el rollback: " + e1.getMessage(), "Error de conexión",
                            ErrorHandler.ERROR_MESSAGE);
                }
            }
            if (conexionCentro != null) {
                try {
                    conexionCentro.rollback();
                } catch (SQLException e1) {
                    ErrorHandler.showMessage("Error al realizar el rollback: " + e1.getMessage(), "Error de conexión",
                            ErrorHandler.ERROR_MESSAGE);
                }
            }
            if (conexionSur != null) {
                try {
                    conexionSur.rollback();
                } catch (SQLException e1) {
                    ErrorHandler.showMessage("Error al realizar el rollback: " + e1.getMessage(), "Error de conexión",
                            ErrorHandler.ERROR_MESSAGE);
                }
            }
            ErrorHandler.showMessage("Error al realizar el commit: " + e.getMessage(), "Error de conexión",
                    ErrorHandler.ERROR_MESSAGE);
        }
    }

    private void faseAbort() {
        try {
            if (conexionNorte != null) {
                conexionNorte.rollback();
            }
            if (conexionCentro != null) {
                conexionCentro.rollback();
            }
            if (conexionSur != null) {
                conexionSur.rollback();
            }
        } catch (SQLException e) {
            ErrorHandler.showMessage("Error al realizar el rollback: " + e.getMessage(), "Error de conexión",
                    ErrorHandler.ERROR_MESSAGE);
        } finally {
            try {
                if (conexionNorte != null) {
                    conexionNorte.setAutoCommit(true);
                }
                if (conexionCentro != null) {
                    conexionCentro.setAutoCommit(true);
                }
                if (conexionSur != null) {
                    conexionSur.setAutoCommit(true);
                }
            } catch (SQLException e) {
                ErrorHandler.showMessage("Error al realizar el rollback: " + e.getMessage(), "Error de conexión",
                        ErrorHandler.ERROR_MESSAGE);
            }
        }
    }

    private List<Map<String, Object>> prepararSentencia(String sentencia, Connection ConexionSql,
            List<String> targetFragments) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        try {
            Statement statement = ConexionSql.createStatement();
            ResultSet resultSet = statement.executeQuery(sentencia);
            while (resultSet.next()) {
                Map<String, Object> fila = new HashMap<>();
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    fila.put(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i));
                }
                resultados.add(fila);
            }
        } catch (SQLException e) {
            ErrorHandler.showMessage("Error al ejecutar la sentencia: " + e.getMessage(), "Error de conexión",
                    ErrorHandler.ERROR_MESSAGE);
        }
        return resultados;
    }

    
    private List<Map<String, Object>> prepararSentenciaNada(String sentencia, Connection ConexionSql,
            List<String> targetFragments) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        try {
            Statement statement = ConexionSql.createStatement();
            int resultSet = statement.executeUpdate(sentencia);
            System.out.println("Filas afectadas: " + resultSet);
        } catch (SQLException e) {
            ErrorHandler.showMessage("Error al ejecutar la sentencia: " + e.getMessage(), "Error de conexión",
                    ErrorHandler.ERROR_MESSAGE);
        }
        return resultados;
    }

    public Connection getConexionNorte() {
        return conexionNorte;
    }

    public void setConexionNorte(Connection conexionNorte) {
        this.conexionNorte = conexionNorte;
    }

    public Connection getConexionCentro() {
        return conexionCentro;
    }

    public void setConexionCentro(Connection conexionCentro) {
        this.conexionCentro = conexionCentro;
    }

    public Connection getConexionSur() {
        return conexionSur;
    }

    public void setConexionSur(Connection conexionSur) {
        this.conexionSur = conexionSur;
    }
}