package com.example.cafemanagementservices.customer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.CartAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.Ban;
import com.example.cafemanagementservices.model.DonHang;
import com.example.cafemanagementservices.model.OrderItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private RecyclerView rvCart;
    private CartAdapter adapter;
    private TextView tvTotal, tvSelectedTable;
    private MaterialButton btnSelectTable, btnPlaceOrder;

    private ArrayList<OrderItem> cartItems = new ArrayList<>();
    private long tongTien = 0;

    private String currentUserId;
    private String currentUserName;
    private String selectedBanId;
    private String selectedBanName;

    private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        currentUserId = getIntent().getStringExtra("userId");
        currentUserName = getIntent().getStringExtra("userName");
        ArrayList<OrderItem> listIntent = (ArrayList<OrderItem>) getIntent().getSerializableExtra("cartItems");
        if (listIntent != null) cartItems = listIntent;

        initViews();
        setupRecyclerView();
        calcTotal();

        btnSelectTable.setOnClickListener(v -> selectTable());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void initViews() {
        rvCart = findViewById(R.id.rvCart);
        tvTotal = findViewById(R.id.tvTotal);
        tvSelectedTable = findViewById(R.id.tvSelectedTable);
        btnSelectTable = findViewById(R.id.btnSelectTable);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
    }

    private void setupRecyclerView() {
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(cartItems);
        rvCart.setAdapter(adapter);
    }

    private void calcTotal() {
        tongTien = 0;
        for (OrderItem it : cartItems) {
            tongTien += it.thanhTien;
        }
        tvTotal.setText("Tổng: " + moneyFmt.format(tongTien));
    }

    private void selectTable() {
        FirebaseService.getBanRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<Ban> banList = new ArrayList<>();
                        ArrayList<String> tenBanList = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Ban b = child.getValue(Ban.class);
                            if (b != null) {
                                b.id = child.getKey();
                                banList.add(b);
                                tenBanList.add(b.tenBan + " (" + b.khuVuc + ")");
                            }
                        }

                        if (banList.isEmpty()) {
                            Toast.makeText(CartActivity.this, "Không có bàn trống", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        new AlertDialog.Builder(CartActivity.this)
                                .setTitle("Chọn bàn")
                                .setItems(tenBanList.toArray(new String[0]), (dialog, which) -> {
                                    Ban b = banList.get(which);
                                    selectedBanId = b.id;
                                    selectedBanName = b.tenBan;
                                    tvSelectedTable.setText("Bàn: " + selectedBanName);
                                })
                                .show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CartActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Map<String, OrderItem> buildOrderItemsMap() {
        Map<String, OrderItem> map = new HashMap<>();
        for (int i = 0; i < cartItems.size(); i++) {
            map.put("item_" + i, cartItems.get(i));
        }
        return map;
    }

    private void placeOrder() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedBanId == null) {
            Toast.makeText(this, "Vui lòng chọn bàn", Toast.LENGTH_SHORT).show();
            return;
        }

        String orderId = FirebaseService.getDonHangRef().push().getKey();
        if (orderId == null) return;

        DonHang order = new DonHang();
        order.id = orderId;
        order.userId = currentUserId;
        order.tenKhachHang = currentUserName;
        order.banId = selectedBanId;
        order.tenBan = selectedBanName;
        order.thoiGian = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());
        order.trangThai = "ChoXuLy";
        order.tongTien = tongTien;
        order.danhSachMon = buildOrderItemsMap();

        btnPlaceOrder.setEnabled(false);

        FirebaseService.getDonHangRef().child(orderId)
                .setValue(order)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(CartActivity.this, "Đặt món thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPlaceOrder.setEnabled(true);
                    Toast.makeText(CartActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}