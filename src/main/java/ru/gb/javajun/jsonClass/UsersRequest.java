package ru.gb.javajun.jsonClass;

public class UsersRequest extends QueryType{

    public static final String TYPE = "Users";


    private String message;

    public UsersRequest() {
        setType(TYPE);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
