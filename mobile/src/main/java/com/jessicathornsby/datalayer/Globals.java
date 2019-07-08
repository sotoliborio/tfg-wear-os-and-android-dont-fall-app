package com.jessicathornsby.datalayer;

public class Globals{
    private static Globals instance;

    // Global variable
    private int STATE = 1;

    // Restrict the constructor from being instantiated
    private Globals(){}

    public void setState(int d){
        this.STATE=d;
    }
    public int getState(){
        return this.STATE;
    }

    public static synchronized Globals getInstance(){
        if(instance==null){
            instance=new Globals();
        }
        return instance;
    }
}