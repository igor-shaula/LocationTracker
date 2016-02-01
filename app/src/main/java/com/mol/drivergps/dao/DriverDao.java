package com.mol.drivergps.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.mol.drivergps.entity_description.Driver;

import java.sql.SQLException;

public class DriverDao extends BaseDaoImpl<Driver, Integer> {

    public DriverDao(ConnectionSource connectionSource, Class<Driver> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public void add(Driver driver){
        try {
            this.createOrUpdate(driver);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Driver getDriver(){
        try {
            return this.queryForId(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}