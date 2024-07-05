package ru.gb.javajun.jsonClass;

public class DisconnectRequest extends QueryType {

    public static final String TYPE = "Disconnect";

    private String message;

    public DisconnectRequest() {
        setType(TYPE);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
