package com.example.vulpix.maphelper.model;

/**
 * Models for message fragment
 */
public class ChatSelectItem {

    private String id;
    private String LastMessage;
    private String LastMessageTime;
    private String readStatus;


    public ChatSelectItem() {

    }

    public ChatSelectItem(String LastMessage, String LastMessageTime, String readStatus) {
        this.LastMessage = LastMessage;
        this.LastMessageTime = LastMessageTime;
        this.readStatus = readStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastMessage() {
        return LastMessage;
    }

    public void setLastMessage(String LastMessage) {
        this.LastMessage = LastMessage;
    }

    public String getLastMessageTime() {
        return LastMessageTime;
    }

    public void setLastMessageTime(String LastMessageTime) {
        this.LastMessageTime = LastMessageTime;
    }

    public String getReadStatus(){ return readStatus; }

    public void setReadStatus(String readStatus){ this.readStatus = readStatus; }

}
