package com.example.cafemanagementservices.model;

import java.io.Serializable;

public class CartItem implements Serializable {
    public MonAn monAn;
    public int soLuong;

    public CartItem() {}

    public CartItem(MonAn monAn, int soLuong) {
        this.monAn = monAn;
        this.soLuong = soLuong;
    }

    public long getThanhTien() {
        if (monAn == null) return 0;
        return monAn.gia * soLuong;
    }
}