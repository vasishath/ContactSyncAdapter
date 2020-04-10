package com.democontactsyncadapter;

/**
 * Created by richa on 3/7/18.
 */

public class MyContact {
    private String name;
    private String number;
    private int id;

    public String getNumber() {
        return number;
    }

    public MyContact(String name, String number, int id) {
        this.name = name;
        this.number = number;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {return id;}
}
