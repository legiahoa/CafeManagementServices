package com.example.cafemanagementservices.admin;

import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends AppCompatActivity {

    private RecyclerView rvMenu;
    private ProgressBar progressBar;
    private final List<MonAn> data = new ArrayList<>();
    private MenuAdapter adapter;

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

        loadMenuFromFirebase();
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
                            if (m != null) data.add(m);
                        }
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MenuActivity.this, "Lỗi tải menu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}