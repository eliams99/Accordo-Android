package com.example.accordo;

import androidx.annotation.NonNull;
import androidx.room.PrimaryKey;

public abstract class Post {
    public static final String TEXT = "t", IMAGE = "i", LOCATION = "l";
    private String uid, name, pversion, type;

    @PrimaryKey
    @NonNull
    private String pid;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPversion() {
        return pversion;
    }

    public void setPversion(String pversion) {
        this.pversion = pversion;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
