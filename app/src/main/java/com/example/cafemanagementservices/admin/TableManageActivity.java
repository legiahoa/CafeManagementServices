package com.example.cafemanagementservices.admin;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
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

    private RecyclerView rvTang1, rvTang2; // 2 RecyclerView riêng
    private FloatingActionButton fabAdd;

    // 2 List dữ liệu riêng
    private final List<Ban> listTang1 = new ArrayList<>();
    private final List<Ban> listTang2 = new ArrayList<>();

    // 2 Adapter riêng
    private TableManageAdapter adapter1;
    private TableManageAdapter adapter2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_manage);

        rvTang1 = findViewById(R.id.rvTang1);
        rvTang2 = findViewById(R.id.rvTang2);
        fabAdd = findViewById(R.id.fabAddTable);

        // Cấu hình RecyclerView Tầng 1 (Grid 2 cột)
        rvTang1.setLayoutManager(new GridLayoutManager(this, 2));
        adapter1 = new TableManageAdapter(listTang1);
        rvTang1.setAdapter(adapter1);
        setupAdapterEvents(adapter1); // Gán sự kiện click

        // Cấu hình RecyclerView Tầng 2 (Grid 2 cột)
        rvTang2.setLayoutManager(new GridLayoutManager(this, 2));
        adapter2 = new TableManageAdapter(listTang2);
        rvTang2.setAdapter(adapter2);
        setupAdapterEvents(adapter2); // Gán sự kiện click

        fabAdd.setOnClickListener(v -> showAddDialog());

        loadTables();
    }

    // Hàm gán sự kiện chung cho cả 2 adapter đỡ viết lại code
    private void setupAdapterEvents(TableManageAdapter adapter) {
        adapter.setOnItemClickListener(this::showEditDialog);
        adapter.setOnItemLongClickListener(this::showDeleteDialog);
    }

    private void loadTables() {
        FirebaseService.getBanRef()
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listTang1.clear();
                        listTang2.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Ban b = child.getValue(Ban.class);
                            if (b != null) {
                                b.id = child.getKey();

                                // PHÂN LOẠI TẦNG Ở ĐÂY
                                if ("Tầng 2".equals(b.khuVuc)) {
                                    listTang2.add(b);
                                } else {
                                    // Mặc định cho vào Tầng 1 (hoặc nếu là "Tầng 1")
                                    listTang1.add(b);
                                }
                            }
                        }
                        adapter1.notifyDataSetChanged();
                        adapter2.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TableManageActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- SỬA DIALOG THÊM ĐỂ CHỌN TẦNG ---
    private void showAddDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText edtTen = new EditText(this);
        edtTen.setHint("Tên bàn (vd: Bàn A1)");
        container.addView(edtTen);

        // Thay EditText bằng RadioGroup chọn Tầng
        TextView tvLabel = new TextView(this);
        tvLabel.setText("Chọn khu vực:");
        tvLabel.setPadding(0, 20, 0, 10);
        container.addView(tvLabel);

        RadioGroup rgKhuVuc = new RadioGroup(this);
        rgKhuVuc.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbT1 = new RadioButton(this); rbT1.setText("Tầng 1");
        RadioButton rbT2 = new RadioButton(this); rbT2.setText("Tầng 2");
        rgKhuVuc.addView(rbT1);
        rgKhuVuc.addView(rbT2);
        rbT1.setChecked(true); // Mặc định tầng 1
        container.addView(rgKhuVuc);

        new AlertDialog.Builder(this)
                .setTitle("Thêm bàn mới")
                .setView(container)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String ten = edtTen.getText().toString().trim();
                    if (ten.isEmpty()) {
                        Toast.makeText(this, "Nhập tên bàn", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Lấy giá trị tầng
                    String khuVuc = rbT2.isChecked() ? "Tầng 2" : "Tầng 1";

                    String id = FirebaseService.getBanRef().push().getKey();
                    if (id != null) {
                        Ban b = new Ban();
                        b.id = id;
                        b.tenBan = ten;
                        b.khuVuc = khuVuc;
                        b.trangThai = "Trong";

                        FirebaseService.getBanRef().child(id).setValue(b);
                        Toast.makeText(this, "Đã thêm " + ten + " vào " + khuVuc, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // --- SỬA DIALOG EDIT ĐỂ CHỌN TẦNG ---
    private void showEditDialog(Ban b) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText edtTen = new EditText(this);
        edtTen.setText(b.tenBan);
        edtTen.setHint("Tên bàn");
        container.addView(edtTen);

        // Chọn Khu Vực
        TextView tvKv = new TextView(this);
        tvKv.setText("Khu vực:");
        tvKv.setPadding(0, 20, 0, 10);
        container.addView(tvKv);

        RadioGroup rgKhuVuc = new RadioGroup(this);
        rgKhuVuc.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbT1 = new RadioButton(this); rbT1.setText("Tầng 1");
        RadioButton rbT2 = new RadioButton(this); rbT2.setText("Tầng 2");
        rgKhuVuc.addView(rbT1);
        rgKhuVuc.addView(rbT2);

        // Set khu vực hiện tại
        if ("Tầng 2".equals(b.khuVuc)) rbT2.setChecked(true);
        else rbT1.setChecked(true);

        container.addView(rgKhuVuc);

        // Chọn Trạng Thái
        TextView tvSt = new TextView(this);
        tvSt.setText("Trạng thái:");
        tvSt.setPadding(0, 20, 0, 10);
        container.addView(tvSt);

        RadioGroup rgStatus = new RadioGroup(this);
        rgStatus.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbTrong = new RadioButton(this); rbTrong.setText("Trống");
        RadioButton rbCoNguoi = new RadioButton(this); rbCoNguoi.setText("Có người");
        rgStatus.addView(rbTrong);
        rgStatus.addView(rbCoNguoi);

        if ("CoNguoi".equals(b.trangThai)) rbCoNguoi.setChecked(true);
        else rbTrong.setChecked(true);

        container.addView(rgStatus);

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa bàn")
                .setView(container)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String ten = edtTen.getText().toString().trim();
                    if (ten.isEmpty()) return;

                    String newKhuVuc = rbT2.isChecked() ? "Tầng 2" : "Tầng 1";
                    String newStatus = rbCoNguoi.isChecked() ? "CoNguoi" : "Trong";

                    b.tenBan = ten;
                    b.khuVuc = newKhuVuc;
                    b.trangThai = newStatus;

                    if (b.id != null) {
                        FirebaseService.getBanRef().child(b.id).setValue(b);
                        Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteDialog(Ban b) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bàn")
                .setMessage("Xóa bàn " + b.tenBan + "?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (b.id != null) FirebaseService.getBanRef().child(b.id).removeValue();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ================== ADAPTER GIỮ NGUYÊN ==================
    static class TableManageAdapter extends RecyclerView.Adapter<TableManageAdapter.TableViewHolder> {
        interface OnItemClickListener { void onItemClick(Ban b); }
        interface OnItemLongClickListener { void onItemLongClick(Ban b); }

        private final List<Ban> items;
        private OnItemClickListener clickListener;
        private OnItemLongClickListener longClickListener;

        public TableManageAdapter(List<Ban> items) { this.items = items; }

        public void setOnItemClickListener(OnItemClickListener l) { this.clickListener = l; }
        public void setOnItemLongClickListener(OnItemLongClickListener l) { this.longClickListener = l; }

        @NonNull
        @Override
        public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ban, parent, false);
            return new TableViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TableViewHolder holder, int position) {
            Ban b = items.get(position);
            holder.tvTenBan.setText(b.tenBan);
            holder.tvKhuVuc.setText(b.khuVuc);

            if ("CoNguoi".equals(b.trangThai)) {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FF7043"));
                holder.tvTrangThai.setText("Đang phục vụ");
            } else {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#26A69A"));
                holder.tvTrangThai.setText("Bàn trống");
            }

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onItemClick(b);
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onItemLongClick(b);
                return true;
            });
        }

        @Override
        public int getItemCount() { return items != null ? items.size() : 0; }

        static class TableViewHolder extends RecyclerView.ViewHolder {
            TextView tvTenBan, tvKhuVuc, tvTrangThai;
            CardView cardView;
            public TableViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTenBan = itemView.findViewById(R.id.tvTenBanManage);
                tvKhuVuc = itemView.findViewById(R.id.tvKhuVucBanManage);
                tvTrangThai = itemView.findViewById(R.id.tvTrangThaiBan);
                cardView = itemView.findViewById(R.id.cardBan);
            }
        }
    }
}