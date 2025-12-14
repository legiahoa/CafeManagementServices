package com.example.cafemanagementservices.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.OrderItemAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.ChiTietMon;
import com.example.cafemanagementservices.model.DonHang;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvOrderInfo, tvCustomerInfo, tvTableInfo, tvPaymentInfo, tvStatus, tvTotalAmount;
    private RecyclerView rvOrderItems;
    private Button btnConfirmCash;

    private final List<ChiTietMon> itemList = new ArrayList<>();
    private OrderItemAdapter itemAdapter;

    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    private String orderId;
    private DonHang currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        bindViews();
        setupRecycler();
        setupActions();
        loadOrderDetail();
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

    private void setupRecycler() {
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new OrderItemAdapter(itemList);
        rvOrderItems.setAdapter(itemAdapter);
    }

    private void setupActions() {
        btnConfirmCash.setOnClickListener(v -> confirmCashPayment());
    }

    private void loadOrderDetail() {
        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null || orderId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu orderId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseService.getDonHangRef()
                .child(orderId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        DonHang d = snapshot.getValue(DonHang.class);
                        if (d == null) {
                            Toast.makeText(OrderDetailActivity.this, "Không tìm thấy đơn", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        if (d.id == null || d.id.trim().isEmpty()) d.id = snapshot.getKey();
                        currentOrder = d;
                        bindOrderToUI(d);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OrderDetailActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindOrderToUI(DonHang d) {
        tvOrderInfo.setText("Đơn #" + d.id + (d.thoiGian != null ? " • " + d.thoiGian : ""));
        tvCustomerInfo.setText("Khách hàng: " + (d.tenKhachHang != null ? d.tenKhachHang : ""));
        tvTableInfo.setText("Bàn: " + ((d.tenBan == null || d.tenBan.isEmpty()) ? "Mang về" : d.tenBan));
        tvPaymentInfo.setText("PT thanh toán: " + (d.phuongThucThanhToan != null ? d.phuongThucThanhToan : ""));
        tvStatus.setText("Trạng thái: " + (d.trangThai != null ? d.trangThai : ""));
        tvTotalAmount.setText("Tổng: " + fmt.format(d.tongTien));

        itemList.clear();
        if (d.items != null) itemList.addAll(d.items.values());
        itemAdapter.notifyDataSetChanged();

        boolean isCash = isCashMethod(d.phuongThucThanhToan);
        boolean isPaid = isPaidStatus(d.trangThai);

        if (isCash && !isPaid) btnConfirmCash.setVisibility(View.VISIBLE);
        else btnConfirmCash.setVisibility(View.GONE);
    }

    private void confirmCashPayment() {
        if (currentOrder == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đã thu tiền?")
                .setMessage("Đơn #" + currentOrder.id + "\nSố tiền: " + fmt.format(currentOrder.tongTien))
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    FirebaseService.getDonHangRef()
                            .child(currentOrder.id)
                            .child("trangThai")
                            .setValue("Đã thanh toán")
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Đã cập nhật: Đã thanh toán", Toast.LENGTH_SHORT).show();
                                currentOrder.trangThai = "Đã thanh toán";
                                tvStatus.setText("Trạng thái: Đã thanh toán");

                                btnConfirmCash.setEnabled(false);
                                btnConfirmCash.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private boolean isCashMethod(String pmRaw) {
        String pm = norm(pmRaw);
        return pm.contains("tien mat") || DonHang.PT_TIEN_MAT.equalsIgnoreCase(pmRaw);
    }

    private boolean isPaidStatus(String stRaw) {
        String st = norm(stRaw);
        return st.contains("da thanh toan")
                || st.contains("hoan tat")
                || DonHang.TRANG_THAI_THANH_CONG.equalsIgnoreCase(stRaw);
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase();
        t = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.replaceAll("\\s+", " ");
    }
}
