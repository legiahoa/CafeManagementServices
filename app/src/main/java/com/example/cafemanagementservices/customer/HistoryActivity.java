package com.example.cafemanagementservices.customer;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<DonHang> orderList = new ArrayList<>();
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        userId = getIntent().getStringExtra("user_id");

        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(orderList);
        rvHistory.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        DatabaseReference ref = FirebaseService.getDonHangRef();
        ref.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            DonHang d = child.getValue(DonHang.class);
                            if (d != null) {
                                orderList.add(d);
                            }
                        }
                        // đơn mới nhất lên đầu
                        Collections.reverse(orderList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HistoryActivity.this,
                                "Lỗi tải lịch sử: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}