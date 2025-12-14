package com.example.cafemanagementservices.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private ProgressBar progress;
    private OrderAdapter adapter;
    private final List<DonHang> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        View root = findViewById(R.id.rootOrderList);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        rvOrders = findViewById(R.id.rvOrders);
        progress = findViewById(R.id.progressOrders);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> onBackPressed());

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList, order -> {
            // mở chi tiết đơn
            Intent i = new Intent(this, OrderDetailActivity.class);
            i.putExtra("orderId", order.id);
            startActivity(i);
        });
        rvOrders.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        progress.setVisibility(View.VISIBLE);

        FirebaseService.getDonHangRef()
                .orderByChild("thoiGian")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            DonHang d = child.getValue(DonHang.class);
                            if (d != null) {
                                d.id = child.getKey();
                                orderList.add(0, d);
                            }
                        }
                        progress.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(OrderListActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
