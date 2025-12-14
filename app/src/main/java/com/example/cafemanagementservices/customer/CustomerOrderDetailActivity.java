package com.example.cafemanagementservices.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.OrderItemAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.ChiTietMon;
import com.example.cafemanagementservices.model.DonHang;
import com.example.cafemanagementservices.model.OrderItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomerOrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";

    private TextView tvOrderInfo, tvCustomerInfo, tvTableInfo, tvPaymentInfo, tvStatus, tvTotalAmount;
    private RecyclerView rvOrderItems;
    private Button btnConfirmCash;

    private final List<ChiTietMon> itemList = new ArrayList<>();
    private OrderItemAdapter itemAdapter;

    private final DecimalFormat fmt = new DecimalFormat("#,### đ");
    private DatabaseReference orderRef;
    private ValueEventListener orderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        String orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (orderId == null || orderId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu order_id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        btnConfirmCash.setVisibility(View.GONE);

        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new OrderItemAdapter(itemList);
        rvOrderItems.setAdapter(itemAdapter);

        orderRef = FirebaseService.getDonHangRef().child(orderId);
        orderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DonHang d = snapshot.getValue(DonHang.class);
                if (d == null) return;

                if (d.id == null || d.id.trim().isEmpty()) d.id = snapshot.getKey();

                tvOrderInfo.setText("Đơn #" + d.id + (d.thoiGian != null ? " • " + d.thoiGian : ""));
                tvCustomerInfo.setText("Khách hàng: " + (d.tenKhachHang != null ? d.tenKhachHang : ""));
                tvTableInfo.setText("Bàn: " + ((d.tenBan == null || d.tenBan.isEmpty()) ? "Mang về" : d.tenBan));
                tvPaymentInfo.setText("PT thanh toán: " + (d.phuongThucThanhToan != null ? d.phuongThucThanhToan : ""));
                tvStatus.setText("Trạng thái: " + (d.trangThai != null ? d.trangThai : ""));
                tvTotalAmount.setText("Tổng: " + fmt.format(d.tongTien));

                itemList.clear();

                if (d.items != null) {
                    itemList.addAll(d.items.values());
                } else if (d.danhSachMon != null) {
                    for (Map.Entry<String, OrderItem> e : d.danhSachMon.entrySet()) {
                        OrderItem oi = e.getValue();
                        if (oi == null) continue;
                        itemList.add(new ChiTietMon(oi.monId, oi.tenMon, oi.soLuong, oi.gia));
                    }
                }

                itemAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerOrderDetailActivity.this, "Lỗi tải chi tiết đơn: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        orderRef.addValueEventListener(orderListener);
    }

    private void bindViews() {
        tvOrderInfo = findViewById(R.id.tvOrderInfo);
        tvCustomerInfo = findViewById(R.id.tvCustomerInfo);
        tvTableInfo = findViewById(R.id.tvTableInfo);
        tvPaymentInfo = findViewById(R.id.tvPaymentInfo);
        tvStatus = findViewById(R.id.tvStatus);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        rvOrderItems = findViewById(R.id.rvOrderItems);
        btnConfirmCash = findViewById(R.id.btnConfirmCash);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderRef != null && orderListener != null) {
            orderRef.removeEventListener(orderListener);
        }
    }
}
