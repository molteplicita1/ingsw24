package it.unisannio.ingsw24.unpacked.logic;

import it.unisannio.ingsw24.unpacked.persistance.UnPackedMySQL;

import java.util.Map;


public interface UnPackedLogic {

    int createUnPackedMySQL(String name, int averageExpiryDays, String category);
    UnPackedMySQL getUnPackedMySQL(String name);
    Map<String, UnPackedMySQL> getAllUnPackedMySQL();
    boolean updateUnPackedMySQL(int ID, int averageExpiryDays);
    boolean deleteUnPackedMySQL(int ID);

}
