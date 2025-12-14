package com.example.cafemanagementservices.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.model.DonHang;

import java.text.DecimalFormat;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderVH> {

    public interface OnOrderClickListener {
        void onOrderClick(DonHang order);
    }

    private final List<DonHang> data;
    private final OnOrderClickListener listener;
    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    public OrderAdapter(List<DonHang> data, OnOrderClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_admin, parent, false);
        return new OrderVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderVH h, int position) {
        DonHang d = data.get(position);

        // Mã + thời gian
        h.tvOrderId.setText("Đơn #" + (d.id != null ? d.id : ""));
        h.tvOrderTime.setText(d.thoiGian != null ? d.thoiGian : "");

        // 1) KHÁCH: luôn là tên người đó (nếu null thì để "Khách lẻ")
        String tenKhach = (d.tenKhachHang != null && !d.tenKhachHang.isEmpty())
                ? d.tenKhachHang
                : "Khách lẻ";
        h.tvCustomer.setText("Khách: " + tenKhach);

        // 2) BÀN: nếu có tên bàn -> "Bàn: X", nếu không -> hiển thị "Mang về"
        if (d.tenBan != null && !d.tenBan.isEmpty()) {
            h.tvTable.setText("Bàn: " + d.tenBan);
        } else {
            h.tvTable.setText("Mang về");
        }

        // Tổng tiền
        h.tvTotal.setText(fmt.format(d.tongTien));

        // Phương thức thanh toán
        h.tvPayment.setText(d.phuongThucThanhToan != null ? d.phuongThucThanhToan : "");

        // Trạng thái + tô màu
        String trangThai = d.trangThai != null ? d.trangThai : "";
        h.tvStatus.setText(trangThai);

        int color;
        String lower = trangThai.toLowerCase();
        if (lower.contains("chờ")) {
            color = 0xFFFFB74D;         // cam - đang chờ
        } else if (lower.contains("thanh toán") || lower.contains("hoàn tất")) {
            color = 0xFF66BB6A;         // xanh - đã thanh toán
        } else if (lower.contains("hủy")) {
            color = 0xFFE57373;
        } else {
            color = 0xFFCFD8DC;
        }
        h.tvStatus.setTextColor(color);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(d);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class OrderVH extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderTime, tvCustomer, tvTable,
                tvTotal, tvPayment, tvStatus;

        public OrderVH(@NonNull View itemView) {
            super(itemView);
            tvOrderId    = itemView.findViewById(R.id.tvOrderId);
            tvOrderTime  = itemView.findViewById(R.id.tvOrderTime);
            tvCustomer   = itemView.findViewById(R.id.tvOrderCustomer);
            tvTable      = itemView.findViewById(R.id.tvOrderTable);
            tvTotal      = itemView.findViewById(R.id.tvOrderTotal);
            tvPayment    = itemView.findViewById(R.id.tvOrderPayment);
            tvStatus     = itemView.findViewById(R.id.tvOrderStatus);
        }
    }
}
