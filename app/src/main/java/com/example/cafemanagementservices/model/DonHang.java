package com.example.cafemanagementservices.model;

import java.io.Serializable;
import java.util.Map;

public class DonHang implements Serializable {
    public static final String PT_TIEN_MAT = "TIEN_MAT";
    public static final String PT_MOMO     = "MOMO";
    public static final String PT_ZALOPAY  = "ZALOPAY";

    // Trạng thái đơn
    public static final String TRANG_THAI_CHO_THANH_TOAN   = "CHO_THANH_TOAN";
    public static final String TRANG_THAI_CHO_XAC_NHAN = "CHO_XAC_NHAN"; // chờ thanh toán / chờ xác nhận
    public static final String TRANG_THAI_THANH_CONG   = "THANH_CONG";
    public static final String TRANG_THAI_THAT_BAI     = "THAT_BAI";
    public static final String TRANG_THAI_DA_HUY       = "DA_HUY";
    public String id;
    public String userId;
    public String tenKhachHang;
    public String banId;
    public String tenBan;
    public String thoiGian;   // "yyyy-MM-dd HH:mm"
    public String trangThai;  // ChoXuLy / HoanTat / DaHuy ...
    public long tongTien;
    public Map<String, OrderItem> danhSachMon;
    public String phuongThucThanhToan;
    public Map<String, ChiTietMon> items;

    // --- MoMo fields (optional, to track payment) ---
    public String momoRequestId;
    public String momoPayUrl;
    public String momoDeeplink;
    public Long momoTransId;
    public Integer momoResultCode;
    public String momoMessage;
    public String momoPayType;
    public Long momoResponseTime;

    public DonHang() {}
}