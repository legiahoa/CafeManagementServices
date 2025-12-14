package com.example.cafemanagementservices.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.DonHang;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.Calendar;

public class RevenueReportActivity extends AppCompatActivity {

    private TextView tvSelectedDate, tvTotalRevenue;
    private MaterialButton btnPickDate;
    private String currentDateFilter;
    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_report);

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        btnPickDate = findViewById(R.id.btnPickDate);

        btnPickDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String mm = (month + 1 < 10) ? "0" + (month + 1) : "" + (month + 1);
                    String dd = (dayOfMonth < 10) ? "0" + dayOfMonth : "" + dayOfMonth;
                    currentDateFilter = year + "-" + mm + "-" + dd;
                    tvSelectedDate.setText(currentDateFilter);
                    loadRevenueForDate(currentDateFilter);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadRevenueForDate(String date) {
        FirebaseService.getDonHangRef()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long total = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            DonHang d = child.getValue(DonHang.class);
                            if (d == null) continue;
                            if (d.thoiGian == null || d.trangThai == null) continue;
                            if (d.trangThai.equals("HoanTat") &&
                                    d.thoiGian.startsWith(date)) {
                                total += d.tongTien;
                            }
                        }
                        tvTotalRevenue.setText("Doanh thu: " + fmt.format(total));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(RevenueReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}