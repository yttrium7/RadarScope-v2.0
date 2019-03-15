package com.example.vulpix.maphelper.model;

/**
 * Models for user friends data
 */
public class Friends {

    public String date;

    public Friends(){}

    public Friends (String date){

        this.date = date;

    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
