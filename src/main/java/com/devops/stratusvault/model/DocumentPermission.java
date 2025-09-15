package com.devops.stratusvault.model;

import jakarta.persistence.*;

@Entity
public class DocumentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    private User sharedWithUser;

    @Enumerated(EnumType.STRING)
    private PermissionLevel permissionLevel;

    public DocumentPermission(Long id, Document document, User sharedWithUser, PermissionLevel permissionLevel) {
        this.id = id;
        this.document = document;
        this.sharedWithUser = sharedWithUser;
        this.permissionLevel = permissionLevel;
    }

    public DocumentPermission() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public User getSharedWithUser() {
        return sharedWithUser;
    }

    public void setSharedWithUser(User sharedWithUser) {
        this.sharedWithUser = sharedWithUser;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
