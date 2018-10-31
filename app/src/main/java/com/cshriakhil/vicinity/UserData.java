package com.cshriakhil.vicinity;

public class UserData {
    String name;
    String email;

    public UserData(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public UserData() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
