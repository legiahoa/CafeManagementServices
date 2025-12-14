package com.example.cafemanagementservices.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.MenuAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.CartItem;
import com.example.cafemanagementservices.model.ChiTietMon;
import com.example.cafemanagementservices.model.DonHang;
import com.example.cafemanagementservices.model.MonAn;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerHomeActivity extends AppCompatActivity {

    private RecyclerView rvMenuToday;
    private ActivityResultLauncher<Intent> checkoutLauncher;

    private ProgressBar progressMenuToday;
    private View layoutCartBar;
    private TextView tvCartSummary;
    private Button btnOrderNow;
    private TextView tvHello;

    private final List<MonAn> menuList = new ArrayList<>();
    private MenuAdapter menuAdapter;

    private final Map<String, CartItem> cart = new LinkedHashMap<>();
    private final DecimalFormat fmt = new DecimalFormat("#,### đ");
    private String currentUserName;
    private String currentUserId;

    private static final int REQ_CHECKOUT = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        View root = findViewById(R.id.rootCustomerHome);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        currentUserId = getIntent().getStringExtra("user_id");

        Button btnHistory = findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v -> {
            Intent i = new Intent(this, HistoryActivity.class);
            i.putExtra("user_id", currentUserId);
            startActivity(i);
        });
        bindViews();
        setupRecycler();
        loadMenuToday();
        setupCartBar();
        checkoutLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        boolean clearCart = result.getData().getBooleanExtra("clear_cart", false);
                        if (clearCart) {
                            cart.clear();
                            updateCartBar();   // hàm bạn đang dùng để cập nhật tổng tiền / số món
                        }
                    }
                }
        );
    }

    private void bindViews() {
        tvHello = findViewById(R.id.tvHello);
        rvMenuToday = findViewById(R.id.rvMenuToday);
        progressMenuToday = findViewById(R.id.progressMenuToday);
        layoutCartBar = findViewById(R.id.layoutCartBar);
        tvCartSummary = findViewById(R.id.tvCartSummary);
        btnOrderNow = findViewById(R.id.btnOrderNow);

        String name = getIntent().getStringExtra("fullName");
        currentUserName = name;
        if (name != null && !name.isEmpty()) {
            tvHello.setText("Xin chào, " + name + " 👋");
        }
    }

    private void setupRecycler() {
        rvMenuToday.setLayoutManager(new GridLayoutManager(this, 2));
        menuAdapter = new MenuAdapter(menuList, this::showAddToCartSheet);
        rvMenuToday.setAdapter(menuAdapter);
    }

    private void loadMenuToday() {
        progressMenuToday.setVisibility(View.VISIBLE);

        FirebaseService.getMonAnRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        menuList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            MonAn m = child.getValue(MonAn.class);
                            if (m != null) menuList.add(m);
                        }
                        menuAdapter.notifyDataSetChanged();
                        progressMenuToday.setVisibility(View.GONE);

                        if (menuList.isEmpty()) {
                            Toast.makeText(CustomerHomeActivity.this,
                                    "Không có món nào trong MonAn", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressMenuToday.setVisibility(View.GONE);
                        Toast.makeText(CustomerHomeActivity.this,
                                "Lỗi tải menu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupCartBar() {
        btnOrderNow.setOnClickListener(v -> {
            if (cart.isEmpty()) {
                Toast.makeText(this, "Bạn chưa chọn món nào", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(CustomerHomeActivity.this, CheckoutActivity.class);
            ArrayList<CartItem> list = new ArrayList<>(cart.values());
            intent.putExtra(CheckoutActivity.EXTRA_CART, list);
            intent.putExtra(CheckoutActivity.EXTRA_USER_NAME, currentUserName);
            checkoutLauncher.launch(intent);
        });

    }
    private void showPaymentSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_payment, null);
        dialog.setContentView(view);

        TextView tvPaymentAmount = view.findViewById(R.id.tvPaymentAmount);
        Button btnPayMomo = view.findViewById(R.id.btnPayMomo);
        Button btnPayZalo = view.findViewById(R.id.btnPayZalo);
        Button btnPayCash = view.findViewById(R.id.btnPayCash);

        // Tính tổng tiền giỏ hàng
        long tongTien = 0;
        for (CartItem item : cart.values()) {
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
        if (cart.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tính tổng
        long tongTien = 0;
        for (CartItem item : cart.values()) {
            tongTien += item.getThanhTien();
        }

        Map<String, ChiTietMon> itemsMap = new java.util.HashMap<>();
        for (Map.Entry<String, CartItem> entry : cart.entrySet()) {
            String monId = entry.getKey();
            CartItem ci = entry.getValue();
            itemsMap.put(monId, new ChiTietMon(
                    ci.monAn.id,
                    ci.monAn.tenMon,
                    ci.soLuong,
                    ci.monAn.gia
            ));
        }

        DatabaseReference donHangRef = FirebaseService.getDonHangRef();
        String orderId = donHangRef.push().getKey();
        if (orderId == null) {
            Toast.makeText(this, "Không tạo được ID đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Thời gian hiện tại
        String now = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());

        DonHang dh = new DonHang();
        dh.id = orderId;
        dh.tenBan = "Mang về"; // sau này nếu có chọn bàn thì thay ở đây
        dh.tenKhachHang = (currentUserName != null && !currentUserName.isEmpty())
                ? currentUserName
                : "Khách lẻ";
        dh.tongTien = tongTien;
        dh.thoiGian = now;
        dh.trangThai = "Chờ xử lý";           // phía quản lý có thể đổi sang "Đã thanh toán" sau
        dh.phuongThucThanhToan = paymentMethod;
        dh.items = itemsMap;

        donHangRef.child(orderId).setValue(dh)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Đặt món thành công (" + paymentMethod + ")",
                            Toast.LENGTH_SHORT).show();
                    cart.clear();
                    updateCartBar();

                    // TODO: nếu muốn làm màn "Thanh toán thành công" thì startActivity ở đây
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Lỗi tạo đơn: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
    private String getTongTienFormatted() {
        long tongTien = 0;
        for (CartItem item : cart.values()) tongTien += item.getThanhTien();
        return fmt.format(tongTien);
    }

    private void updateCartBar() {
        int tongSoLuong = 0;
        long tongTien = 0;
        for (CartItem item : cart.values()) {
            tongSoLuong += item.soLuong;
            tongTien += item.getThanhTien();
        }

        if (tongSoLuong == 0) {
            layoutCartBar.setVisibility(View.GONE);
        } else {
            layoutCartBar.setVisibility(View.VISIBLE);
            tvCartSummary.setText(tongSoLuong + " món • Tổng: " + fmt.format(tongTien));
        }
    }

    private void showAddToCartSheet(MonAn monAn) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_add_to_cart, null);
        dialog.setContentView(view);

        ImageView imgMon = view.findViewById(R.id.imgMonSheet);
        TextView tvTenMon = view.findViewById(R.id.tvTenMonSheet);
        TextView tvGiaMon = view.findViewById(R.id.tvGiaMonSheet);
        TextView tvSoLuong = view.findViewById(R.id.tvSoLuongSheet);
        TextView tvThanhTien = view.findViewById(R.id.tvThanhTienSheet);
        ImageButton btnGiam = view.findViewById(R.id.btnGiamSheet);
        ImageButton btnTang = view.findViewById(R.id.btnTangSheet);
        Button btnThemGio = view.findViewById(R.id.btnThemGioSheet);
        Button btnXoaMon = view.findViewById(R.id.btnXoaMonSheet);

        Glide.with(this)
                .load(monAn.hinhAnhUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(imgMon);

        tvTenMon.setText(monAn.tenMon);
        tvGiaMon.setText(fmt.format(monAn.gia));
        CartItem existing = cart.get(monAn.id);
        final int[] soLuong = {1};

        if (existing != null) {
            soLuong[0] = existing.soLuong;
            btnThemGio.setText("Cập nhật giỏ");
            btnXoaMon.setVisibility(View.VISIBLE);          // cho phép xóa
        } else {
            btnThemGio.setText("Thêm vào giỏ");
            btnXoaMon.setVisibility(View.GONE);            // chưa có trong giỏ thì khỏi hiện
        }
        tvSoLuong.setText(String.valueOf(soLuong[0]));
        tvThanhTien.setText("Thành tiền: " + fmt.format(monAn.gia * soLuong[0]));

        btnGiam.setOnClickListener(v -> {
            if (soLuong[0] > 1) {
                soLuong[0]--;
                tvSoLuong.setText(String.valueOf(soLuong[0]));
                tvThanhTien.setText("Thành tiền: " + fmt.format(monAn.gia * soLuong[0]));
            }
        });

        btnTang.setOnClickListener(v -> {
            soLuong[0]++;
            tvSoLuong.setText(String.valueOf(soLuong[0]));
            tvThanhTien.setText("Thành tiền: " + fmt.format(monAn.gia * soLuong[0]));
        });

        btnThemGio.setOnClickListener(v -> {
            CartItem ex = cart.get(monAn.id);
            if (ex == null) {
                cart.put(monAn.id, new CartItem(monAn, soLuong[0]));
            } else {
                ex.soLuong += soLuong[0];
            }
            updateCartBar();
            dialog.dismiss();
            Toast.makeText(this,
                    "Đã thêm " + soLuong[0] + " x " + monAn.tenMon + " vào giỏ",
                    Toast.LENGTH_SHORT).show();
        });
        // XÓA MÓN KHỎI GIỎ
        btnXoaMon.setOnClickListener(v -> {
            cart.remove(monAn.id);
            updateCartBar();
            dialog.dismiss();
            Toast.makeText(this,
                    "Đã xóa " + monAn.tenMon + " khỏi giỏ",
                    Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }
}
