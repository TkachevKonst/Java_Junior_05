package ru.gb.javajun.jsonClass;

import ru.gb.javajun.User;

import java.util.List;

public class ListResponse {

    private List<User> users;

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
