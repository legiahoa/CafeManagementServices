package com.example.cafemanagementservices.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.adapter.MenuAdapter;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.MonAn;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends AppCompatActivity {

    private RecyclerView rvMenu;
    private ProgressBar progressBar;
    private final List<MonAn> data = new ArrayList<>();
    private MenuAdapter adapter;

    private View btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        rvMenu = findViewById(R.id.rvMenu);
        progressBar = findViewById(R.id.progressMenu);

        rvMenu.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new MenuAdapter(data, monAn ->
                Toast.makeText(MenuActivity.this,
                        "Chọn: " + monAn.tenMon, Toast.LENGTH_SHORT).show());
        rvMenu.setAdapter(adapter);

        // Nút (+) thêm món
        btnAdd = findViewById(R.id.fabAddMon);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showAddMonDialog());
        }

        loadMenuFromFirebase();
    }

    // ====== PHẦN THÊM MỚI: Dialog nhập món ======
    private void showAddMonDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_mon_an, null, false);
        EditText edtTen  = view.findViewById(R.id.edtTenMon);
        EditText edtGia  = view.findViewById(R.id.edtGia);
        EditText edtLoai = view.findViewById(R.id.edtLoai);
        EditText edtMoTa = view.findViewById(R.id.edtMoTa);
        EditText edtHinh = view.findViewById(R.id.edtHinhUrl);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thêm món mới")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSave.setOnClickListener(v -> {

                String tenMon = edtTen.getText() != null ? edtTen.getText().toString().trim() : "";
                String giaStr = edtGia.getText() != null ? edtGia.getText().toString().trim() : "";
                String loai   = edtLoai.getText()!= null ? edtLoai.getText().toString().trim() : "";
                String moTa   = edtMoTa.getText()!= null ? edtMoTa.getText().toString().trim() : "";
                String hinhUrl= edtHinh.getText()!= null ? edtHinh.getText().toString().trim() : "";

                if (TextUtils.isEmpty(tenMon)) {
                    Toast.makeText(this, "Vui lòng nhập tên món", Toast.LENGTH_LONG).show();
                    return;
                }

                long gia;
                try {
                    gia = Long.parseLong(giaStr);
                } catch (Exception e) {
                    Toast.makeText(this, "Giá phải là số", Toast.LENGTH_LONG).show();
                    return;
                }
                if (gia <= 0) {
                    Toast.makeText(this, "Giá phải > 0", Toast.LENGTH_LONG).show();
                    return;
                }

                btnSave.setEnabled(false);

                addMonToFirebase(tenMon, moTa, gia, hinhUrl, loai,
                        () -> {
                            btnSave.setEnabled(true);
                            dialog.dismiss();
                        },
                        () -> btnSave.setEnabled(true)
                );
            });
        });

        dialog.show();
    }

    // ====== PHẦN THÊM MỚI: Ghi lên Firebase MonAn ======
    private void addMonToFirebase(String tenMon, String moTa, long gia,
                                  String hinhUrl, String loai,
                                  Runnable onSuccess, Runnable onFail) {

        DatabaseReference ref = FirebaseService.getMonAnRef();
        String key = ref.push().getKey();
        if (key == null) key = String.valueOf(System.currentTimeMillis());

        MonAn mon = new MonAn(key, tenMon, moTa, gia, hinhUrl, loai);

        ref.child(key)
                .setValue(mon)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã thêm món: " + tenMon, Toast.LENGTH_LONG).show();
                    loadMenuFromFirebase();
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Thêm món thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (onFail != null) onFail.run();
                });
    }

    private void loadMenuFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseService.getMonAnRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        data.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            MonAn m = child.getValue(MonAn.class);
                            if (m != null) {
                                if (m.id == null) m.id = child.getKey();
                                data.add(m);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MenuActivity.this, "Lỗi tải menu: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
