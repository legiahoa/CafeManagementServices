package com.example.cafemanagementservices.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.model.CartItem;

import java.text.DecimalFormat;
import java.util.List;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private final List<CartItem> items;
    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    public BillAdapter(List<CartItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        CartItem ci = items.get(position);
        holder.tvName.setText(ci.monAn.tenMon);
        holder.tvQuantity.setText("Số lượng: " + ci.soLuong);
        holder.tvPrice.setText(fmt.format(ci.monAn.gia));
        holder.tvSubTotal.setText(fmt.format(ci.getThanhTien()));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQuantity, tvPrice, tvSubTotal;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName     = itemView.findViewById(R.id.tvBillItemName);
            tvQuantity = itemView.findViewById(R.id.tvBillItemQty);
            tvPrice    = itemView.findViewById(R.id.tvBillItemPrice);
            tvSubTotal = itemView.findViewById(R.id.tvBillItemSubtotal);
        }
    }
}