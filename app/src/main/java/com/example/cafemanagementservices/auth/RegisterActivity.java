package com.example.cafemanagementservices.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.example.cafemanagementservices.util.HashUtils;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtUsername, edtEmail,
            edtPassword, edtConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupActions();
    }

    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsernameReg);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPasswordReg);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupActions() {
        tvBackToLogin.setOnClickListener(v -> {
            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        });

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        String fullName = getText(edtFullName);
        String username = getText(edtUsername);
        String email = getText(edtEmail);
        String password = getText(edtPassword);
        String confirm = getText(edtConfirmPassword);
        String passwordHash = HashUtils.md5(password);

        // Validate cơ bản
        if (fullName.isEmpty() || username.isEmpty() ||
                email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        // Kiểm tra trùng tên đăng nhập
        FirebaseService.getTaiKhoanRef()
                .orderByChild("tenDangNhap")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren()) {
                            btnRegister.setEnabled(true);
                            Toast.makeText(RegisterActivity.this,
                                    "Tên đăng nhập đã tồn tại", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        createUser(fullName, username, email, passwordHash);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this,
                                "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUser(String fullName, String username,
                            String email, String password) {

        String uid = FirebaseService.getTaiKhoanRef().push().getKey();
        if (uid == null) {
            btnRegister.setEnabled(true);
            Toast.makeText(this, "Không thể tạo tài khoản, thử lại", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mặc định role = KhachHang
        User u = new User(
                uid,
                username,
                fullName,
                email,
                "KhachHang",
                password      // lưu plaintext cho đơn giản theo cách bạn đang login
        );

        FirebaseService.getTaiKhoanRef()
                .child(uid)
                .setValue(u)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(RegisterActivity.this,
                            "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    // Quay về Login, prefill username
                    Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                    i.putExtra("prefill_username", username);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this,
                            "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getText(TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }
}