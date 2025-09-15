package com.devops.stratusvault.dto;

public class ShareRequestDTO {
    private String email;

    public ShareRequestDTO(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
