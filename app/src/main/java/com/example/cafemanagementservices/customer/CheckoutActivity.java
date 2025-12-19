package com.example.cafemanagementservices.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.BillAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.Ban;
import com.example.cafemanagementservices.model.CartItem;
import com.example.cafemanagementservices.model.ChiTietMon;
import com.example.cafemanagementservices.model.DonHang;
import com.example.cafemanagementservices.payment.MomoBackendClient;
import com.example.cafemanagementservices.payment.PaymentPendingActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {
    private String getMomoCreateUrl() {
        String s = getString(R.string.momo_create_url);
        return s != null ? s.trim() : "";
    }

    private ActivityResultLauncher<Intent> momoPendingLauncher;
    private AlertDialog momoLoadingDialog;

    public static final String EXTRA_CART = "cart_items";
    public static final String EXTRA_USER_NAME = "user_name";
    public static final String EXTRA_USER_ID = "extra_user_id";

    private Button btnBack, btnThanhToan, btnSelectTable;
    private TextView tvBillTotal, tvSelectedTable;
    private RecyclerView rvBillItems;

    private final ArrayList<CartItem> cartItems = new ArrayList<>();
    private String currentUserName;
    private String currentUserId;

    private String selectedBanId = null;
    private String selectedBanName = "Mang về";

    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        bindViews();
        getDataFromIntent();
        setupRecycler();
        updateTotal();

        momoPendingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        boolean clearCart = result.getData().getBooleanExtra("clear_cart", false);
                        if (clearCart) {
                            Intent data = new Intent();
                            data.putExtra("clear_cart", true);
                            setResult(RESULT_OK, data);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Thanh toán chưa hoàn tất", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnBack.setOnClickListener(v -> confirmBackToMenu());
        btnThanhToan.setOnClickListener(v -> showPaymentSheet());
        btnSelectTable.setOnClickListener(v -> showTableSelectionDialog());
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvBillTotal = findViewById(R.id.tvBillTotal);
        btnThanhToan = findViewById(R.id.btnThanhToan);
        rvBillItems = findViewById(R.id.rvBillItems);

        btnSelectTable = findViewById(R.id.btnSelectTableCheckout);
        tvSelectedTable = findViewById(R.id.tvSelectedTableCheckout);
    }

    private void showTableSelectionDialog() {
        FirebaseService.getBanRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Ban> listBan = new ArrayList<>();
                ArrayList<String> listTen = new ArrayList<>();

                listTen.add("Mang về");
                listBan.add(null);

                for (DataSnapshot child : snapshot.getChildren()) {
                    Ban b = child.getValue(Ban.class);
                    if (b != null && "Trong".equals(b.trangThai)) {
                        b.id = child.getKey();
                        listBan.add(b);
                        listTen.add(b.tenBan + " (" + b.khuVuc + ")");
                    }
                }

                new AlertDialog.Builder(CheckoutActivity.this)
                        .setTitle("Chọn vị trí ngồi")
                        .setItems(listTen.toArray(new String[0]), (dialog, which) -> {
                            if (which == 0) {
                                selectedBanId = null;
                                selectedBanName = "Mang về";
                            } else {
                                Ban selected = listBan.get(which);
                                selectedBanId = selected.id;
                                selectedBanName = selected.tenBan;
                            }
                            tvSelectedTable.setText("Bàn: " + selectedBanName);
                        })
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CheckoutActivity.this, "Lỗi tải bàn: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getDataFromIntent() {
        Intent i = getIntent();

        currentUserName = i.getStringExtra(EXTRA_USER_NAME);
        if (currentUserName == null) currentUserName = i.getStringExtra("userName");
        if (currentUserName == null) currentUserName = i.getStringExtra("fullName");

        currentUserId = i.getStringExtra(EXTRA_USER_ID);
        if (currentUserId == null) currentUserId = i.getStringExtra("userId");
        if (currentUserId == null) currentUserId = i.getStringExtra("user_id");

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
        for (CartItem ci : cartItems) sum += ci.getThanhTien();
        tvBillTotal.setText("Tổng: " + fmt.format(sum));
    }

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

    private void showPaymentSheet() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_payment, null);
        dialog.setContentView(view);

        TextView tvPaymentAmount = view.findViewById(R.id.tvPaymentAmount);
        Button btnPayMomo = view.findViewById(R.id.btnPayMomo);
        Button btnPayZalo = view.findViewById(R.id.btnPayZalo);
        Button btnPayCash = view.findViewById(R.id.btnPayCash);

        long tongTien = 0;
        for (CartItem item : cartItems) tongTien += item.getThanhTien();
        tvPaymentAmount.setText("Tổng: " + fmt.format(tongTien));

        btnPayMomo.setOnClickListener(v -> { createOrder("MoMo"); dialog.dismiss(); });
        btnPayZalo.setOnClickListener(v -> { createOrder("ZaloPay"); dialog.dismiss(); });
        btnPayCash.setOnClickListener(v -> { createOrder("Tiền mặt"); dialog.dismiss(); });

        dialog.show();
    }

    private void createOrder(String paymentMethod) {
        if (cartItems.isEmpty()) return;

        String finalName = (currentUserName != null && !currentUserName.trim().isEmpty())
                ? currentUserName : "Khách lẻ";

        saveOrder(paymentMethod, finalName);
    }

    private void saveOrder(String paymentMethod, String displayName) {
        long tongTien = 0;
        for (CartItem ci : cartItems) tongTien += ci.getThanhTien();

        Map<String, ChiTietMon> itemsMap = new HashMap<>();
        for (CartItem ci : cartItems) {
            itemsMap.put(ci.monAn.id, new ChiTietMon(ci.monAn.id, ci.monAn.tenMon, ci.soLuong, ci.monAn.gia));
        }

        DatabaseReference donHangRef = FirebaseService.getDonHangRef();
        String orderId = genMomoOrderId();
        if (orderId == null) return;

        String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new java.util.Date());

        String status;
        if ("Tiền mặt".equalsIgnoreCase(paymentMethod)) {
            status = "Chờ thu tiền";
        } else if ("MoMo".equalsIgnoreCase(paymentMethod)) {
            status = "Chờ thanh toán";
        } else {
            // ZaloPay chưa làm flow => đừng set "Đã thanh toán" để tránh sai trạng thái
            status = "Chờ xử lý";
        }

        DonHang dh = new DonHang();
        dh.id = orderId;
        dh.userId = currentUserId;
        dh.tenBan = selectedBanName;
        dh.banId = selectedBanId;
        dh.tenKhachHang = displayName;
        dh.tongTien = tongTien;
        dh.thoiGian = now;
        dh.trangThai = status;
        dh.phuongThucThanhToan = paymentMethod;
        dh.items = itemsMap;

        final long tongTienFinal = tongTien;

        donHangRef.child(orderId).setValue(dh)
                .addOnSuccessListener(unused -> {
                    if (selectedBanId != null) {
                        FirebaseService.getBanRef().child(selectedBanId).child("trangThai").setValue("CoNguoi");
                    }

                    if ("MoMo".equalsIgnoreCase(paymentMethod)) {
                        Toast.makeText(this, "Đã tạo đơn, chuyển sang MoMo...", Toast.LENGTH_SHORT).show();
                        startMomoPayment(orderId, tongTienFinal);
                    } else {
                        Toast.makeText(this, "Đặt món thành công!", Toast.LENGTH_SHORT).show();
                        Intent data = new Intent();
                        data.putExtra("clear_cart", true);
                        setResult(RESULT_OK, data);
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tạo đơn: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void startMomoPayment(String orderId, long amount) {
        if (momoLoadingDialog == null) {
            momoLoadingDialog = new AlertDialog.Builder(this)
                    .setTitle("MoMo")
                    .setMessage("Đang tạo giao dịch...")
                    .setCancelable(false)
                    .create();
        }
        momoLoadingDialog.show();

        String createUrl = getMomoCreateUrl();
        if (createUrl.isEmpty() || createUrl.contains("YOUR_NGROK_DOMAIN")) {
            if (momoLoadingDialog.isShowing()) momoLoadingDialog.dismiss();
            FirebaseService.getDonHangRef().child(orderId).child("trangThai").setValue("Thanh toán lỗi");
            Toast.makeText(this, "Bạn chưa cấu hình momo_create_url (ngrok).", Toast.LENGTH_LONG).show();
            return;
        }

        String orderInfo = "Thanh toán đơn " + orderId;

        MomoBackendClient.createPayment(
                createUrl,
                orderId,
                amount,
                orderInfo,
                new MomoBackendClient.Callback() {
                    @Override
                    public void onSuccess(MomoBackendClient.CreatePaymentResult r) {
                        if (momoLoadingDialog != null && momoLoadingDialog.isShowing()) {
                            momoLoadingDialog.dismiss();
                        }

                        // ✅ 1) Check resultCode từ backend
                        if (r.resultCode != null && r.resultCode != 0) {
                            FirebaseService.getDonHangRef().child(orderId).child("trangThai")
                                    .setValue("Thanh toán lỗi");
                            Toast.makeText(CheckoutActivity.this,
                                    "MoMo lỗi (" + r.resultCode + "): " + (r.message != null ? r.message : ""),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // ✅ 2) Check link rỗng
                        boolean emptyPayUrl = (r.payUrl == null || r.payUrl.trim().isEmpty());
                        boolean emptyDeep = (r.deeplink == null || r.deeplink.trim().isEmpty());
                        if (emptyPayUrl && emptyDeep) {
                            FirebaseService.getDonHangRef().child(orderId).child("trangThai")
                                    .setValue("Thanh toán lỗi");
                            Toast.makeText(CheckoutActivity.this,
                                    "Không nhận được link thanh toán từ server.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Lưu link về Firebase (debug/audit)
                        java.util.Map<String, Object> upd = new java.util.HashMap<>();
                        if (r.requestId != null) upd.put("momoRequestId", r.requestId);
                        if (r.payUrl != null) upd.put("momoPayUrl", r.payUrl);
                        if (r.deeplink != null) upd.put("momoDeeplink", r.deeplink);
                        if (r.message != null) upd.put("momoCreateMessage", r.message);
                        if (r.resultCode != null) upd.put("momoCreateResultCode", r.resultCode);
                        FirebaseService.getDonHangRef().child(orderId).updateChildren(upd);

                        Intent it = new Intent(CheckoutActivity.this, PaymentPendingActivity.class);
                        it.putExtra(PaymentPendingActivity.EXTRA_ORDER_ID, orderId);
                        it.putExtra(PaymentPendingActivity.EXTRA_PAY_URL, r.payUrl);
                        it.putExtra(PaymentPendingActivity.EXTRA_DEEPLINK, r.deeplink);
                        momoPendingLauncher.launch(it);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (momoLoadingDialog != null && momoLoadingDialog.isShowing()) {
                            momoLoadingDialog.dismiss();
                        }
                        FirebaseService.getDonHangRef().child(orderId).child("trangThai")
                                .setValue("Thanh toán lỗi");
                        Toast.makeText(CheckoutActivity.this,
                                "Không tạo được MoMo: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
    private static String genMomoOrderId() {
        long ts = System.currentTimeMillis();
        int rnd = (int)(Math.random() * 9000) + 1000;
        return "DH" + ts + rnd;
    }
}
