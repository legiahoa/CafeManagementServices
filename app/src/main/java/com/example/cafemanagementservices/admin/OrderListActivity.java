package com.example.cafemanagementservices.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.OrderAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.DonHang;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OrderListActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private OrderAdapter adapter;
    private final List<DonHang> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        rvOrders = findViewById(R.id.rvOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OrderAdapter(orderList, order -> {
            // mở chi tiết đơn để xác nhận đã thu tiền
            Intent i = new Intent(this, OrderDetailActivity.class);
            i.putExtra("orderId", order.id);
            startActivity(i);
        });
        rvOrders.setAdapter(adapter);

        loadPendingCashOrders();
    }


    private void loadPendingCashOrders() {
        FirebaseService.getDonHangRef()
                .orderByChild("phuongThucThanhToan")
                .equalTo(DonHang.PT_TIEN_MAT)   // chỉ lấy đơn tiền mặt
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            DonHang d = child.getValue(DonHang.class);
                            if (d != null) {
                                d.id = child.getKey();

                                // chỉ add những đơn đang chờ xác nhận
                                if (DonHang.TRANG_THAI_CHO_XAC_NHAN.equals(d.trangThai)) {
                                    orderList.add(d);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OrderListActivity.this,
                                error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}