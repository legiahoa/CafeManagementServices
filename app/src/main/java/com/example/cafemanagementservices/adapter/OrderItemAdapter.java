package com.example.cafemanagementservices.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.model.ChiTietMon;

import java.text.DecimalFormat;
import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.VH> {

    private final List<ChiTietMon> items;
    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    public OrderItemAdapter(List<ChiTietMon> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChiTietMon item = items.get(position);
        if (item == null) return;

        holder.tvTenMon.setText(item.tenMon != null ? item.tenMon : "");
        holder.tvSoLuong.setText("Số lượng: " + item.soLuong);
        holder.tvDonGia.setText(fmt.format(item.donGia));

        long thanhTien = (long) item.soLuong * item.donGia;
        holder.tvThanhTien.setText(fmt.format(thanhTien));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTenMon, tvSoLuong, tvDonGia, tvThanhTien;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTenMon = itemView.findViewById(R.id.tvBillItemName);
            tvSoLuong = itemView.findViewById(R.id.tvBillItemQty);
            tvDonGia = itemView.findViewById(R.id.tvBillItemPrice);
            tvThanhTien = itemView.findViewById(R.id.tvBillItemSubtotal);
        }
    }
}
