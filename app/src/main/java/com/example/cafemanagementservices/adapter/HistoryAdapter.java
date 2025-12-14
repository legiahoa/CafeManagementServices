package com.example.cafemanagementservices.adapter;

import android.content.res.Resources;
import android.graphics.Color;
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

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<DonHang> orders;
    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    public HistoryAdapter(List<DonHang> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_order, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        DonHang d = orders.get(position);
        holder.bind(d, fmt);
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderTime, tvOrderTable, tvOrderTotal, tvOrderMethod, tvOrderStatus;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderTime   = itemView.findViewById(R.id.tvOrderTime);
            tvOrderTable  = itemView.findViewById(R.id.tvOrderTable);
            tvOrderTotal  = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderMethod = itemView.findViewById(R.id.tvOrderMethod);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
        }

        void bind(DonHang d, DecimalFormat fmt) {
            tvOrderTime.setText(d.thoiGian);
            tvOrderTable.setText("Bàn " + d.tenBan + " - " + d.tenKhachHang);
            tvOrderTotal.setText("Tổng: " + fmt.format(d.tongTien));

            // Phương thức
            String methodText;
            switch (d.phuongThucThanhToan) {
                case DonHang.PT_MOMO:
                    methodText = "Thanh toán: MoMo";
                    break;
                case DonHang.PT_ZALOPAY:
                    methodText = "Thanh toán: ZaloPay";
                    break;
                case DonHang.PT_TIEN_MAT:
                    methodText = "Thanh toán: Tiền mặt";
                    break;
                default:
                    methodText = "Thanh toán: -";
            }
            tvOrderMethod.setText(methodText);

            // Trạng thái
            String statusText;
            int color;
            Resources res = itemView.getResources();
            switch (d.trangThai) {
                case DonHang.TRANG_THAI_CHO_XAC_NHAN:
                    statusText = "Trạng thái: Đang chờ xác nhận";
                    color = Color.YELLOW;
                    break;
                case DonHang.TRANG_THAI_THANH_CONG:
                    statusText = "Trạng thái: Thành công";
                    color = Color.GREEN;
                    break;
                case DonHang.TRANG_THAI_THAT_BAI:
                    statusText = "Trạng thái: Thất bại";
                    color = Color.RED;
                    break;
                case DonHang.TRANG_THAI_DA_HUY:
                    statusText = "Trạng thái: Đã hủy";
                    color = Color.GRAY;
                    break;
                default:
                    statusText = "Trạng thái: -";
                    color = Color.WHITE;
            }
            tvOrderStatus.setText(statusText);
            tvOrderStatus.setTextColor(color);
        }
    }
}