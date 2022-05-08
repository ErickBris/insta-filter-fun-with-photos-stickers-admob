package com.stickercamera.app.model;

/**
 *
 Local simple to use, the actual items associated with the sticker attributes can be added to this class
 */
public class Addon  {
    private int    id;

    //JSON used
    public Addon() {

    }

    public Addon(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
