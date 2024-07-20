package com.social.amigos.models;

@SuppressWarnings("unused")
public class ModelFriendlist {

    String id;

    public ModelFriendlist() {
    }

    public ModelFriendlist(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
