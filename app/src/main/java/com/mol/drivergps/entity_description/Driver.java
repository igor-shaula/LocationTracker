package com.mol.drivergps.entity_description;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class Driver {

    @DatabaseField (id = true)
    private int id = 1;

    @DatabaseField
    private String qr;

    @DatabaseField
    private String coordinates;

    @DatabaseField
    private String time;

    public Driver() {
    }

    public Driver(String qr, String coordinates, String time) {
        this.qr = qr;
        this.coordinates = coordinates;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public String getQr() {
        return qr;
    }

    public void setQr(String qr) {
        this.qr = qr;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Driver{" +
                "qr='" + qr + '\'' +
                ", coordinates='" + coordinates + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}