package it.unisannio.ingsw24.unpacked.persistance;

import it.unisannio.ingsw24.entities.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.*;

public class UnPackedDAOMySQL implements UnPackedDAO{

    private static String host = System.getenv("MYSQL_ADDRESS");
    private static String port = System.getenv("MYSQL_PORT");
    private Connection connection;


    private static final String GET_FoodDAO = "SELECT * FROM " + TABLE + " WHERE " + ELEMENT_NAME + " = ?";
    private static final String GET_ALL_FoodDAO_FOR_NAME = "SELECT " + ELEMENT_NAME + " FROM " + TABLE;
    private static final String POST_FoodDAO = "INSERT INTO " + TABLE + "(" + ELEMENT_UNIQUE_ID + ", " + ELEMENT_NAME + ", " + ELEMENT_AVERAGE_EXPIRY_DAYS + ", " + ELEMENT_CATEGORY + ")" 
                                + " VALUES ( ?, ?, ?, ?)";
    private static final String DELETE_FoodDAO = "DELETE FROM " + TABLE + " WHERE " + ELEMENT_UNIQUE_ID + " = ?";
    private static final String UPDATE_AVGEXPDAYS_FoodDAO = "UPDATE " + TABLE + " SET " + ELEMENT_AVERAGE_EXPIRY_DAYS + " = ?" + " WHERE " + ELEMENT_UNIQUE_ID + " = ?";

    public UnPackedDAOMySQL(){
        if (host == null) {
            host = "172.17.0.4";
        }
        if (port == null) {
            port = "3306";
        }
        String URI = "jdbc:mysql://" + host + ":" + port + "/" + DATABASE_NAME;

        try {
            this.connection = DriverManager.getConnection(URI, "root", "cusas-mysql");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Parser che crea l'oggetto FoodDAO
    public static FoodDAO FoodDAOFromResultSet(ResultSet resultSet) throws SQLException{

        //String cat = resultSet.getString(ELEMENT_CATEGORY);

        return new FoodDAO(resultSet.getString(ELEMENT_UNIQUE_ID),
                resultSet.getString(ELEMENT_NAME),
                resultSet.getInt(ELEMENT_AVERAGE_EXPIRY_DAYS),
                Category.valueOf(resultSet.getString(ELEMENT_CATEGORY).toUpperCase()));
    }


    @Override
    public FoodDAO getFoodDAO(String name) {
        try {
            PreparedStatement preparedStmt = this.connection.prepareStatement(GET_FoodDAO);
            preparedStmt.setString(1, name);
            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                FoodDAO uf = FoodDAOFromResultSet(rs);
                return uf;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // FACTORY
    }

    @Override
    public boolean createFoodDAO(String ID, String name, int averageExpiryDays, String category) {
        try {
            PreparedStatement preparedStmt = this.connection.prepareStatement(POST_FoodDAO);
            preparedStmt.setString(1, ID);
            preparedStmt.setString(2, name);
            preparedStmt.setInt(3, averageExpiryDays);
            preparedStmt.setString(4, category);
            
            int affectedRows = preparedStmt.executeUpdate();

            return affectedRows == 1;
    
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public boolean deleteFoodDAO(String ID) {
        try {
            PreparedStatement preparedStmt = this.connection.prepareStatement(DELETE_FoodDAO);
            preparedStmt.setString(1, ID);
            int affectedRows = preparedStmt.executeUpdate();

            return affectedRows == 1;
           
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } 
    }


    private List<String> getAllFoodDAONames(){

        List<String> names = new ArrayList<>();

        try {
            PreparedStatement preparedStmt = this.connection.prepareStatement(GET_ALL_FoodDAO_FOR_NAME);
            ResultSet rs = preparedStmt.executeQuery();
            while (rs.next()) {
                names.add(rs.getString(UnPackedDAO.ELEMENT_NAME));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return names;
    }
    

    @Override
    public Map<String, FoodDAO> getAllFoodDAO() {
        
        Map<String, FoodDAO> FoodDAOs = new HashMap<>();
        List<String> names = this.getAllFoodDAONames();
        for( String name : names){
            FoodDAO upf = this.getFoodDAO(name);
            FoodDAOs.put(upf.getID(), upf);
        }

        return FoodDAOs;
    }

    
    // Il tipo di ritorno dovrebbe essere boolean, come parametri si dovrebbero passare 
    // ID dell'oggetto da modificare e la scadenza media, magari aggiornando la mappa se contiene l'oggetto e non è vuota
    
    /* @Override
    public FoodDAO updateFoodDAO(int averageExpiryDate) {
        return null;
    } */

    @Override
    public boolean updateFoodDAO(String ID, int averageExpiryDays) {
        try{
            PreparedStatement preparedStatement = this.connection.prepareStatement(UPDATE_AVGEXPDAYS_FoodDAO);
            preparedStatement.setString(2, ID);
            preparedStatement.setInt(1, averageExpiryDays);
            int affectedRows = preparedStatement.executeUpdate();
            
            return affectedRows == 1;
        }
        catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public boolean dropDB() {
        return false;
    }

    @Override
    public boolean closeConnection() {
        return false;
    }
}
