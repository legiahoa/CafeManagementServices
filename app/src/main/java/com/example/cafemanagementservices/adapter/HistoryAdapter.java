package com.example.cafemanagementservices.adapter;

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
import java.text.Normalizer;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface OnOrderClickListener {
        void onOrderClick(DonHang order);
    }

    private final List<DonHang> orders;
    private final OnOrderClickListener listener;
    private final DecimalFormat fmt = new DecimalFormat("#,### đ");

    public HistoryAdapter(List<DonHang> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DonHang d = orders.get(position);
        holder.bind(d, fmt);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && d != null) listener.onOrderClick(d);
        });
    }

    @Override
    public int getItemCount() {
        return orders == null ? 0 : orders.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderTime, tvOrderTable, tvOrderTotal, tvOrderMethod, tvOrderStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvOrderTime   = itemView.findViewById(R.id.tvOrderTime);
            tvOrderTable  = itemView.findViewById(R.id.tvOrderTable);
            tvOrderTotal  = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderMethod = itemView.findViewById(R.id.tvOrderMethod);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
        }

        void bind(DonHang d, DecimalFormat fmt) {
            if (d == null) return;

            tvOrderTime.setText(d.thoiGian != null ? d.thoiGian : "");

            String tenBan = d.tenBan != null ? d.tenBan : "";
            String tenKhach = (d.tenKhachHang != null && !d.tenKhachHang.trim().isEmpty())
                    ? d.tenKhachHang.trim()
                    : "Khách lẻ";

            String banNorm = norm(tenBan);
            if (banNorm.isEmpty() || banNorm.contains("mang ve")) {
                tvOrderTable.setText("Mang về - " + tenKhach);
            } else {
                tvOrderTable.setText("Bàn " + tenBan + " - " + tenKhach);
            }

            tvOrderTotal.setText("Tổng: " + fmt.format(d.tongTien));

            String pmRaw = d.phuongThucThanhToan != null ? d.phuongThucThanhToan : "";
            String pm = norm(pmRaw);

            boolean isCash = pm.contains("tien mat")
                    || DonHang.PT_TIEN_MAT.equalsIgnoreCase(pmRaw);

            String methodText;
            if (isCash) methodText = "Thanh toán: Tiền mặt";
            else if (pm.contains("momo") || DonHang.PT_MOMO.equalsIgnoreCase(pmRaw)) methodText = "Thanh toán: MoMo";
            else if (pm.contains("zalo") || pm.contains("zalopay") || DonHang.PT_ZALOPAY.equalsIgnoreCase(pmRaw)) methodText = "Thanh toán: ZaloPay";
            else methodText = "Thanh toán: -";

            tvOrderMethod.setText(methodText);

            String stRaw = d.trangThai != null ? d.trangThai : "";
            String st = norm(stRaw);

            String statusText;
            int color;

            if (st.contains("da thanh toan")
                    || st.contains("thanh cong")
                    || st.contains("hoan tat")
                    || DonHang.TRANG_THAI_THANH_CONG.equalsIgnoreCase(stRaw)) {

                statusText = "Trạng thái: Đã thanh toán";
                color = 0xFF66BB6A;

            }
            else if (st.contains("cho")
                    || st.contains("thu tien")
                    || DonHang.TRANG_THAI_CHO_THANH_TOAN.equalsIgnoreCase(stRaw)
                    || DonHang.TRANG_THAI_CHO_XAC_NHAN.equalsIgnoreCase(stRaw)) {

                if (isCash) statusText = "Trạng thái: Chờ thu tiền";
                else statusText = "Trạng thái: " + (stRaw.isEmpty() ? "Đang chờ" : stRaw);

                color = 0xFFFFB74D;

            }
            else if (st.contains("huy") || DonHang.TRANG_THAI_DA_HUY.equalsIgnoreCase(stRaw)) {

                statusText = "Trạng thái: Đã hủy";
                color = 0xFFE57373;

            }
            else if (st.contains("that bai") || st.contains("fail") || DonHang.TRANG_THAI_THAT_BAI.equalsIgnoreCase(stRaw)) {

                statusText = "Trạng thái: Thất bại";
                color = Color.RED;

            }
            else if (stRaw == null || stRaw.trim().isEmpty()) {

                if (isCash) {
                    statusText = "Trạng thái: Chờ thu tiền";
                    color = 0xFFFFB74D;
                } else {
                    statusText = "Trạng thái: Đã thanh toán";
                    color = 0xFF66BB6A;
                }

            } else {
                statusText = "Trạng thái: " + stRaw;
                color = Color.WHITE;
            }

            tvOrderStatus.setText(statusText);
            tvOrderStatus.setTextColor(color);
        }

        private static String norm(String s) {
            if (s == null) return "";
            String t = s.trim().toLowerCase();
            t = Normalizer.normalize(t, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            return t.replaceAll("\\s+", " ");
        }
    }
}
