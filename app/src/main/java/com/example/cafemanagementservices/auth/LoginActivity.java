package com.example.cafemanagementservices.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cafemanagementservices.admin.AdminDashboardActivity;
import com.example.cafemanagementservices.customer.CustomerHomeActivity;
import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.User;
import com.example.cafemanagementservices.util.HashUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtUsername, edtPassword;
    private MaterialButton btnLogin;
    private TextView tvToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupActions();

        String prefillUser = getIntent().getStringExtra("prefill_username");
        if (prefillUser != null && !prefillUser.isEmpty()) {
            edtUsername.setText(prefillUser);
        }
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        tvToRegister= findViewById(R.id.tvToRegister);
    }

    private void setupActions() {
        tvToRegister.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String username = edtUsername.getText() != null ? edtUsername.getText().toString().trim() : "";
        String password = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";

        // 1. Tính toán Hash của mật khẩu nhập vào
        String passwordHash = HashUtils.md5(password);

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        FirebaseService.getTaiKhoanRef()
                .orderByChild("tenDangNhap")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        btnLogin.setEnabled(true);

                        if (!snapshot.hasChildren()) {
                            Toast.makeText(LoginActivity.this, "Tài khoản không tồn tại", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (DataSnapshot child : snapshot.getChildren()) {
                            User u = child.getValue(User.class);
                            if (u == null) continue;

                            // --- SỬA LỖI TẠI ĐÂY ---
                            // So sánh mật khẩu trong DB với passwordHash (đã mã hóa)
                            if (u.matKhau != null && u.matKhau.equals(passwordHash)) {
                                u.uid = child.getKey(); // Lưu lại UID thật

                                // Kiểm tra vai trò để chuyển hướng
                                if ("KhachHang".equalsIgnoreCase(u.vaiTro)) {
                                    Intent i = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                                    i.putExtra("userId", u.uid);
                                    i.putExtra("userName", u.hoTen);
                                    startActivity(i);
                                } else {
                                    // Admin hoặc NhanVien
                                    Intent i = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                    i.putExtra("userId", u.uid);
                                    i.putExtra("userName", u.hoTen);
                                    startActivity(i);
                                }
                                finish();
                                return;
                            }
                        }

                        // Nếu chạy hết vòng lặp mà không khớp mật khẩu
                        Toast.makeText(LoginActivity.this, "Sai mật khẩu", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}