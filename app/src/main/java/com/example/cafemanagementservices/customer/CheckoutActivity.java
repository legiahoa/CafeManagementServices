package com.example.cafemanagementservices.customer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.BillAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.CartItem;
import com.example.cafemanagementservices.model.ChiTietMon;
import com.example.cafemanagementservices.model.DonHang;
import com.example.cafemanagementservices.ui.PaymentBottomSheet;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DatabaseReference;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity implements PaymentBottomSheet.OnPaymentResultListener {

    public static final String EXTRA_CART = "cart_items";
    public static final String EXTRA_USER_NAME = "user_name";
    public static final String EXTRA_USER_ID    = "extra_user_id";
    private Button btnBack;
    private TextView tvBillTotal;
    private Button btnThanhToan;
    private DonHang currentOrder;
    private RecyclerView rvBillItems;
    private List<CartItem> cartItems = new ArrayList<>();
    private String currentUserName;
    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        bindViews();
        getDataFromIntent();
        setupRecycler();
        updateTotal();

        btnBack.setOnClickListener(v -> confirmBackToMenu());
        btnThanhToan.setOnClickListener(v -> showPaymentSheet());
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvBillTotal = findViewById(R.id.tvBillTotal);
        btnThanhToan = findViewById(R.id.btnThanhToan);
        rvBillItems = findViewById(R.id.rvBillItems);
    }

    private void getDataFromIntent() {
        Intent i = getIntent();
        currentUserName = i.getStringExtra(EXTRA_USER_NAME);

        ArrayList<CartItem> list = (ArrayList<CartItem>) i.getSerializableExtra(EXTRA_CART);
        if (list != null) {
            cartItems.clear();
            cartItems.addAll(list);
        }
    }

    private void setupRecycler() {
        rvBillItems.setLayoutManager(new GridLayoutManager(this, 1));
        rvBillItems.setAdapter(new BillAdapter(cartItems));
    }

    private void updateTotal() {
        long sum = 0;
        for (CartItem ci : cartItems) {
            sum += ci.getThanhTien();
        }
        tvBillTotal.setText("Tổng: " + fmt.format(sum));
    }

    /** Dialog xác nhận khi quay lại màn chọn món */
    private void confirmBackToMenu() {
        new AlertDialog.Builder(this)
                .setTitle("Quay lại chọn món?")
                .setMessage("Nếu quay lại, các món đã chọn sẽ bị xoá khỏi hoá đơn. Bạn chắc chắn chứ?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    Intent data = new Intent();
                    data.putExtra("clear_cart", true);
                    setResult(RESULT_OK, data);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    // ====== THANH TOÁN =======

    private void showPaymentSheet() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_payment, null);
        dialog.setContentView(view);

        TextView tvPaymentAmount = view.findViewById(R.id.tvPaymentAmount);
        Button btnPayMomo = view.findViewById(R.id.btnPayMomo);
        Button btnPayZalo = view.findViewById(R.id.btnPayZalo);
        Button btnPayCash = view.findViewById(R.id.btnPayCash);

        long tongTien = 0;
        for (CartItem item : cartItems) {
            tongTien += item.getThanhTien();
        }
        tvPaymentAmount.setText("Tổng: " + fmt.format(tongTien));

        btnPayMomo.setOnClickListener(v -> {
            createOrder("MoMo");
            dialog.dismiss();
        });

        btnPayZalo.setOnClickListener(v -> {
            createOrder("ZaloPay");
            dialog.dismiss();
        });

        btnPayCash.setOnClickListener(v -> {
            createOrder("Tiền mặt");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createOrder(String paymentMethod) {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        long tongTien = 0;
        for (CartItem ci : cartItems) tongTien += ci.getThanhTien();

        Map<String, ChiTietMon> itemsMap = new HashMap<>();
        for (CartItem ci : cartItems) {
            itemsMap.put(ci.monAn.id,
                    new ChiTietMon(ci.monAn.id, ci.monAn.tenMon, ci.soLuong, ci.monAn.gia));
        }

        DatabaseReference donHangRef = FirebaseService.getDonHangRef();
        String orderId = donHangRef.push().getKey();
        if (orderId == null) {
            Toast.makeText(this, "Không tạo được ID đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String now = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
        ).format(new java.util.Date());

        DonHang dh = new DonHang();
        dh.id = orderId;
        dh.tenBan = "Mang về";
        dh.tenKhachHang = (currentUserName != null && !currentUserName.isEmpty())
                ? currentUserName
                : "Khách lẻ";
        dh.tongTien = tongTien;
        dh.thoiGian = now;
        dh.trangThai = "Chờ xử lý";
        dh.phuongThucThanhToan = paymentMethod;
        dh.items = itemsMap;

        donHangRef.child(orderId).setValue(dh)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Đặt món & thanh toán (" + paymentMethod + ") thành công",
                            Toast.LENGTH_SHORT).show();

                    Intent data = new Intent();
                    data.putExtra("clear_cart", true);
                    setResult(RESULT_OK, data);
                    finish();   // quay lại màn chọn món
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tạo đơn: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
    private void showPaymentSheet() {
        if (currentOrder == null) return;

        long tongTien = currentOrder.tongTien;

        PaymentBottomSheet sheet = PaymentBottomSheet.newInstance(currentOrder, tongTien);
        sheet.show(getSupportFragmentManager(), "PaymentSheet");
    }

    // gọi hàm này khi bấm nút "Thanh toán" ở màn hóa đơn
    private void setupActions() {
        Button btnThanhToan = findViewById(R.id.btnThanhToan);
        btnThanhToan.setOnClickListener(v -> showPaymentSheet());
    }

    // ===== callback từ BottomSheet =====
    @Override
    public void onPaymentFinished(boolean success, String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        if (success) {
            // clear giỏ, quay lại home
            setResult(RESULT_OK);
            finish();
        }
    }
}