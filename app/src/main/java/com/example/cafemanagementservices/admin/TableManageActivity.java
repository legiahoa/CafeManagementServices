package com.example.cafemanagementservices.admin;


import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.Ban;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TableManageActivity extends AppCompatActivity {

    private RecyclerView rvTables;
    private FloatingActionButton fabAdd;
    private final List<Ban> banList = new ArrayList<>();
    private TableManageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_manage);

        rvTables = findViewById(R.id.rvTableManage);
        fabAdd = findViewById(R.id.fabAddTable);

        rvTables.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TableManageAdapter(banList);
        rvTables.setAdapter(adapter);
        adapter.setOnItemLongClickListener(this::showDeleteDialog);

        fabAdd.setOnClickListener(v -> showAddDialog());

        loadTables();
    }

    private void loadTables() {
        FirebaseService.getBanRef()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        banList.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Ban b = child.getValue(Ban.class);
                            if (b != null) {
                                b.id = child.getKey();
                                banList.add(b);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TableManageActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText edtTen = new EditText(this);
        edtTen.setHint("Tên bàn (vd: Bàn A1)");
        container.addView(edtTen);

        EditText edtKhuVuc = new EditText(this);
        edtKhuVuc.setHint("Khu vực (vd: Tầng 1)");
        container.addView(edtKhuVuc);

        new AlertDialog.Builder(this)
                .setTitle("Thêm bàn")
                .setView(container)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String ten = edtTen.getText().toString().trim();
                    String khuVuc = edtKhuVuc.getText().toString().trim();
                    if (ten.isEmpty() || khuVuc.isEmpty()) {
                        Toast.makeText(this, "Nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Ban b = new Ban();
                    b.tenBan = ten;
                    b.khuVuc = khuVuc;
                    b.trangThai = "Trong";

                    String id = FirebaseService.getBanRef().push().getKey();
                    if (id != null) {
                        FirebaseService.getBanRef().child(id).setValue(b);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteDialog(Ban b) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bàn")
                .setMessage("Bạn muốn xóa \"" + b.tenBan + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (b.id != null) {
                        FirebaseService.getBanRef().child(b.id).removeValue();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    static class TableManageAdapter extends RecyclerView.Adapter<TableManageAdapter.TableViewHolder> {

        interface OnItemLongClickListener {
            void onItemLongClick(Ban b);
        }

        private final List<Ban> items;
        private OnItemLongClickListener longClickListener;

        public TableManageAdapter(List<Ban> items) {
            this.items = items;
        }

        public void setOnItemLongClickListener(OnItemLongClickListener l) {
            this.longClickListener = l;
        }

        @NonNull
        @Override
        public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ban, parent, false);
            return new TableViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TableViewHolder holder, int position) {
            Ban b = items.get(position);
            holder.tvTenBan.setText(b.tenBan);
            holder.tvKhuVuc.setText("Khu vực: " + b.khuVuc);
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onItemLongClick(b);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        static class TableViewHolder extends RecyclerView.ViewHolder {
            TextView tvTenBan, tvKhuVuc;
            public TableViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTenBan = itemView.findViewById(R.id.tvTenBanManage);
                tvKhuVuc = itemView.findViewById(R.id.tvKhuVucBanManage);
            }
        }
    }
}