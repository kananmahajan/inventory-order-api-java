package com.kanan.inventory.security;
public record Identity(String username,Role role) {
    public enum Role { ADMIN, CUSTOMER }
    public boolean isAdmin(){return role==Role.ADMIN;}
}
