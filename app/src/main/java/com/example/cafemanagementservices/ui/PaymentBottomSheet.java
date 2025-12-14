package com.example.cafemanagementservices.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cafemanagementservices.R;
import com.example.cafemanagementservices.firebase.FirebaseService;
import com.example.cafemanagementservices.model.DonHang;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DatabaseReference;

public class PaymentBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TONG_TIEN = "arg_tong_tien";
    private static final String ARG_DON_HANG  = "arg_don_hang";

    private DonHang donHang;
    private long tongTien;

    public interface OnPaymentResultListener {
        void onPaymentFinished(boolean success, String message);
    }

    private OnPaymentResultListener listener;

    public static PaymentBottomSheet newInstance(DonHang donHang, long tongTien) {
        PaymentBottomSheet sheet = new PaymentBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DON_HANG, donHang);
        args.putLong(ARG_TONG_TIEN, tongTien);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnPaymentResultListener) {
            listener = (OnPaymentResultListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_payment, container, false);

        if (getArguments() != null) {
            donHang = (DonHang) getArguments().getSerializable(ARG_DON_HANG);
            tongTien = getArguments().getLong(ARG_TONG_TIEN, 0);
        }

        TextView tvAmount = view.findViewById(R.id.tvPaymentAmount);
        tvAmount.setText("Tổng: " + formatMoney(tongTien));

        Button btnMomo = view.findViewById(R.id.btnPayMomo);
        Button btnZalo = view.findViewById(R.id.btnPayZalo);
        Button btnCash = view.findViewById(R.id.btnPayCash);

        btnMomo.setOnClickListener(v -> payWithMomo());
        btnZalo.setOnClickListener(v -> payWithZalo());
        btnCash.setOnClickListener(v -> payWithCash());

        return view;
    }

    private String formatMoney(long v) {
        java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,### đ");
        return fmt.format(v);
    }

    // ============== TIỀN MẶT ==============
    private void payWithCash() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thanh toán tiền mặt")
                .setMessage("Vui lòng tới quầy để thanh toán. Nhân viên sẽ xác nhận giao dịch.")
                .setPositiveButton("Đồng ý", (d, which) -> {
                    donHang.phuongThucThanhToan = DonHang.PT_TIEN_MAT;
                    donHang.trangThai = DonHang.TRANG_THAI_CHO_XAC_NHAN; // chờ thu ngân xác nhận
                    saveOrderToFirebase("Đơn hàng đã được ghi nhận, vui lòng thanh toán tại quầy.");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ============== MOMO ==============
    private void payWithMomo() {
        showWaitingAndSimulateGateway(DonHang.PT_MOMO);
    }

    // ============== ZALOPAY ==============
    private void payWithZalo() {
        showWaitingAndSimulateGateway(DonHang.PT_ZALOPAY);
    }

    private void showWaitingAndSimulateGateway(String paymentType) {
        // dialog chờ
        Dialog waiting = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đang xử lý thanh toán")
                .setMessage("Vui lòng chờ hệ thống xác thực giao dịch...")
                .setCancelable(false)
                .create();
        waiting.show();

        // Giả lập gateway: 2s sau trả kết quả thành công
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            waiting.dismiss();

            // Ở đây bạn có thể random thất bại / thành công nếu thích
            boolean success = true;

            if (success) {
                donHang.phuongThucThanhToan = paymentType;
                donHang.trangThai = DonHang.TRANG_THAI_THANH_CONG;
                saveOrderToFirebase("Thanh toán thành công!");
            } else {
                donHang.phuongThucThanhToan = paymentType;
                donHang.trangThai = DonHang.TRANG_THAI_THAT_BAI;
                saveOrderToFirebase("Thanh toán thất bại, vui lòng thử lại.");
            }

        }, 2000);
    }

    private void saveOrderToFirebase(String messageForUser) {
        DatabaseReference ref = FirebaseService.getDonHangRef();

        if (donHang.id == null || donHang.id.isEmpty()) {
            String key = ref.push().getKey();
            donHang.id = key;
        }

        ref.child(donHang.id).setValue(donHang)
                .addOnSuccessListener(unused -> {
                    if (listener != null) {
                        listener.onPaymentFinished(
                                DonHang.TRANG_THAI_THANH_CONG.equals(donHang.trangThai)
                                        || DonHang.TRANG_THAI_CHO_XAC_NHAN.equals(donHang.trangThai),
                                messageForUser
                        );
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onPaymentFinished(false,
                                "Lưu đơn hàng thất bại: " + e.getMessage());
                    }
                    dismiss();
                });
    }
}