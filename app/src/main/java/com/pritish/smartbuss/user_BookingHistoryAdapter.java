package com.pritish.smartbuss;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.wajahatkarim3.easyflipview.EasyFlipView;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Locale;

public class user_BookingHistoryAdapter extends RecyclerView.Adapter<user_BookingHistoryAdapter.BookingViewHolder> {

    private final ArrayList<BookingWrapper> bookingHistoryList;
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int KEY_LENGTH = 16;
    private static final int QR_CODE_SIZE = 300;
    private static final int QR_CODE_BACKGROUND_COLOR = Color.WHITE;
    private static final int QR_CODE_FOREGROUND_COLOR = Color.BLACK;
    private static final String TAG = "ShowTicketAdapter";
    private final DatabaseReference keyRef;

    public user_BookingHistoryAdapter(ArrayList<BookingWrapper> bookingHistoryList) {
        this.bookingHistoryList = bookingHistoryList;
        this.keyRef = FirebaseDatabase.getInstance().getReference("SmartBus/TicketKeys");
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_show_ticket, parent, false);
        return new BookingViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingWrapper bookingWrapper = bookingHistoryList.get(position);

        bindFrontDetails(holder, bookingWrapper);

        try {
            String qrContent = generateQRContentForAdapter(bookingWrapper);
            if (qrContent != null) {
                Bitmap qrBitmap = generateQRCodeBitmap(qrContent);
                holder.qrImageView.setImageBitmap(qrBitmap);

                String transactionId = bookingWrapper.getTransactionId();
                if (transactionId != null) {
                    String[] parts = qrContent.split("\\|");
                    if (parts.length > 5) {
                        saveKeyToDatabase(transactionId, parts[4]);
                    }
                } else {
                    Log.e(TAG, "Transaction ID is null, cannot save key.");
                }
            } else {
                Log.e(TAG, "QR Content is null for transaction: " + bookingWrapper.getTransactionId());
                holder.qrImageView.setImageResource(android.R.drawable.ic_dialog_alert);
            }
        } catch (Exception e) {
            Log.e(TAG, "QR Code Error", e);
            holder.qrImageView.setImageResource(android.R.drawable.ic_dialog_alert);
        }

        updateWatermark(holder, bookingWrapper.getScannedAt());
        holder.flipView.setOnClickListener(v -> holder.flipView.flipTheView());
    }

    private void bindFrontDetails(BookingViewHolder holder, BookingWrapper bookingWrapper) {
        holder.time.setText(String.format("Time: %s", bookingWrapper.getTime() != null ? bookingWrapper.getTime() : "N/A"));
        holder.date.setText(String.format("Date: %s", bookingWrapper.getDate() != null ? bookingWrapper.getDate() : "N/A"));
        holder.startStop.setText(String.format("From: %s", bookingWrapper.getStartStop() != null ? bookingWrapper.getStartStop() : "N/A"));
        holder.destinationStop.setText(String.format("To: %s", bookingWrapper.getDestinationStop() != null ? bookingWrapper.getDestinationStop() : "N/A"));

        // Modified part:  Safely get the price, handling potential null values.
        String price = bookingWrapper.getPrice();
        holder.price.setText(String.format("Price: %s", price != null ? price : "N/A"));

        holder.transactionID.setText(String.format("TXN ID: %s", bookingWrapper.getTransactionId() != null ? bookingWrapper.getTransactionId() : "N/A"));
    }

    private String generateQRContentForAdapter(BookingWrapper bookingWrapper) {
        String transactionId = bookingWrapper.getTransactionId();
        String date = bookingWrapper.getDate();
        String time = bookingWrapper.getTime();
        String startStop = bookingWrapper.getStartStop();
        String phoneNumber = bookingWrapper.getPhoneNumber();

        if (transactionId == null || date == null || time == null || startStop == null || phoneNumber == null) {
            Log.e(TAG, "One or more fields for QR code generation are null. TXN_ID: " + transactionId);
            return null;
        }

        String randomKey = generateRandomKey();
        return String.format(Locale.US,
                "%s|%s|%s|%s|%s|%s",
                transactionId,
                date,
                time,
                startStop,
                randomKey,
                phoneNumber
        );
    }

    private String generateRandomKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder keyBuilder = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            keyBuilder.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return keyBuilder.toString();
    }

    private Bitmap generateQRCodeBitmap(String content) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

        Bitmap bitmap = Bitmap.createBitmap(QR_CODE_SIZE, QR_CODE_SIZE, Bitmap.Config.RGB_565);
        for (int x = 0; x < QR_CODE_SIZE; x++) {
            for (int y = 0; y < QR_CODE_SIZE; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? QR_CODE_FOREGROUND_COLOR : QR_CODE_BACKGROUND_COLOR);
            }
        }
        return bitmap;
    }

    private void saveKeyToDatabase(String transactionId, String randomKey) {
        if (transactionId == null || randomKey == null || transactionId.isEmpty() || randomKey.isEmpty()) {
            Log.e(TAG, "Invalid key data - TXN: " + transactionId + ", Key: " + randomKey);
            return;
        }

        Log.d(TAG, "Saving key for TXN: " + transactionId);
        keyRef.child(transactionId).setValue(randomKey)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Key saved successfully for TXN: " + transactionId);
                        verifyKeyInDatabase(transactionId, randomKey);
                    } else {
                        Log.e(TAG, "Key save failed for TXN: " + transactionId, task.getException());
                    }
                });
    }

    private void verifyKeyInDatabase(String transactionId, String expectedKey) {
        keyRef.child(transactionId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String storedKey = snapshot.getValue(String.class);
                if (expectedKey.equals(storedKey)) {
                    Log.d(TAG, "Key verification successful for TXN: " + transactionId);
                } else {
                    Log.e(TAG, "Key verification failed for TXN: " + transactionId + ". Stored: " + storedKey + ", Expected: " + expectedKey);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Verification cancelled for TXN: " + transactionId, error.toException());

            }

        });
    }

    private void updateWatermark(BookingViewHolder holder, String scannedAt) {
        if (scannedAt != null && !scannedAt.isEmpty()) {
            holder.watermark.setVisibility(View.VISIBLE);
            holder.watermark.setText(String.format("Scanned: %s", scannedAt));
        } else {
            holder.watermark.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookingHistoryList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        public TextView time, date, startStop, destinationStop, price, transactionID, watermark;
        public ImageView qrImageView;
        public EasyFlipView flipView;

        public BookingViewHolder(View itemView) {
            super(itemView);
            flipView = itemView.findViewById(R.id.flip_view);
            time = itemView.findViewById(R.id.time);
            date = itemView.findViewById(R.id.date);
            startStop = itemView.findViewById(R.id.startStop);
            destinationStop = itemView.findViewById(R.id.destinationStop);
            price = itemView.findViewById(R.id.price);
            transactionID = itemView.findViewById(R.id.transactionID);
            qrImageView = itemView.findViewById(R.id.qrImageView);
            watermark = itemView.findViewById(R.id.watermarkTextView);
        }
    }
}