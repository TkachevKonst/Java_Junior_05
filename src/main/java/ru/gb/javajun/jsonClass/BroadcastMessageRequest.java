package ru.gb.javajun.jsonClass;

public class BroadcastMessageRequest extends QueryType {


    public static final String TYPE = "BroadcastMessage";

    private String message;

    public BroadcastMessageRequest() {
        setType(TYPE);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
