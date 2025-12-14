package com.example.cafemanagementservices.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.HistoryAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.DonHang;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private final List<DonHang> orderList = new ArrayList<>();

    private String userId;
    private String hoTen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        userId = getIntent().getStringExtra("user_id");
        if (userId == null) userId = getIntent().getStringExtra("userId");

        if (userId == null || userId.trim().isEmpty()) {
            FirebaseUser u = FirebaseService.getAuth().getCurrentUser();
            if (u != null) userId = u.getUid();
        }

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không xác định được userId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        adapter = new HistoryAdapter(orderList, order -> {
            if (order == null || order.id == null || order.id.trim().isEmpty()) return;

            Intent i = new Intent();
            i.setClassName(this, "com.example.cafemanagementservices.customer.CustomerOrderDetailActivity");
            i.putExtra("orderId", order.id);
            i.putExtra("order_id", order.id);
            startActivity(i);
        });

        rvHistory.setAdapter(adapter);

        fetchHoTenThenLoad();
    }

    private void fetchHoTenThenLoad() {
        FirebaseService.getTaiKhoanRef()
                .child(userId)
                .child("hoTen")
                .get()
                .addOnSuccessListener(snap -> {
                    hoTen = snap.getValue(String.class);
                    loadHistory();
                })
                .addOnFailureListener(e -> loadHistory());
    }

    private void loadHistory() {
        FirebaseService.getDonHangRef()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            DonHang d = child.getValue(DonHang.class);
                            if (d == null) continue;

                            if (d.id == null || d.id.trim().isEmpty()) d.id = child.getKey();

                            boolean match = false;

                            if (d.userId != null && d.userId.equals(userId)) {
                                match = true;
                            } else if ((d.userId == null || d.userId.trim().isEmpty())
                                    && hoTen != null && !hoTen.trim().isEmpty()
                                    && d.tenKhachHang != null
                                    && d.tenKhachHang.trim().equalsIgnoreCase(hoTen.trim())) {
                                match = true;
                            }

                            if (match) orderList.add(d);
                        }

                        Collections.sort(orderList, (a, b) -> {
                            String ta = (a != null && a.thoiGian != null) ? a.thoiGian : "";
                            String tb = (b != null && b.thoiGian != null) ? b.thoiGian : "";
                            return tb.compareTo(ta);
                        });

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HistoryActivity.this, "Lỗi tải lịch sử: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
