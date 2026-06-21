package fcu.app.productexpirationtimerecordapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class InventoryFragment extends Fragment {

    private static final String INVENTORY_COLLECTION = "inventory";

    private View rootView;

    private TextView tvVegPercent, tvOtherPercent, tvDrinkPercent, tvDairyPercent;
    private TextView tvSoonExpireCount, tvWasteRate;
    private ProgressBar pbVeg, pbOther, pbDrink, pbDairy;

    private MaterialButton btnToggleChart;
    private View progressContainer;
    private PieChart pieChart;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration inventoryListener;

    private boolean isChartMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_inventory, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initView();
        updateUI();
        setupChart();

        btnToggleChart.setOnClickListener(v -> toggleView());

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadInventoryStats();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (inventoryListener != null) {
            inventoryListener.remove();
            inventoryListener = null;
        }
    }

    private void initView() {
        tvVegPercent = rootView.findViewById(R.id.tvVegPercent);
        tvOtherPercent = rootView.findViewById(R.id.tvOtherPercent);
        tvDrinkPercent = rootView.findViewById(R.id.tvDrinkPercent);
        tvDairyPercent = rootView.findViewById(R.id.tvDairyPercent);
        tvSoonExpireCount = rootView.findViewById(R.id.tvSoonExpireCount);
        tvWasteRate = rootView.findViewById(R.id.tvWasteRate);

        pbVeg = rootView.findViewById(R.id.pbVeg);
        pbOther = rootView.findViewById(R.id.pbOther);
        pbDrink = rootView.findViewById(R.id.pbDrink);
        pbDairy = rootView.findViewById(R.id.pbDairy);

        btnToggleChart = rootView.findViewById(R.id.btnToggleChart);
        pieChart = rootView.findViewById(R.id.pieChart);
        progressContainer = rootView.findViewById(R.id.progressContainer);
    }

    private void updateUI() {
        setItem(pbVeg, tvVegPercent, 25);
        setItem(pbOther, tvOtherPercent, 36);
        setItem(pbDrink, tvDrinkPercent, 22);
        setItem(pbDairy, tvDairyPercent, 17);

        tvSoonExpireCount.setText("0 件");
        tvWasteRate.setText("0.0%");
    }

    private void setItem(ProgressBar bar, TextView text, int value) {
        bar.setProgress(value);
        text.setText(value + "%");
    }

    private void toggleView() {
        isChartMode = !isChartMode;

        if (isChartMode) {
            progressContainer.setVisibility(View.GONE);
            pieChart.setVisibility(View.VISIBLE);
            btnToggleChart.setText("列表");
        } else {
            progressContainer.setVisibility(View.VISIBLE);
            pieChart.setVisibility(View.GONE);
            btnToggleChart.setText("圓餅圖");
        }
    }

    private void setupChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(25f, "蔬菜"));
        entries.add(new PieEntry(36f, "其他"));
        entries.add(new PieEntry(22f, "飲料"));
        entries.add(new PieEntry(17f, "乳製品"));

        PieDataSet dataSet = new PieDataSet(entries, "");

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#4CAF50"));
        colors.add(Color.parseColor("#9E9E9E"));
        colors.add(Color.parseColor("#FF9800"));
        colors.add(Color.parseColor("#2196F3"));

        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);
        pieChart.setCenterText("庫存分佈");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.BLACK);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(true);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(10f);
        pieChart.invalidate();
    }

    private void loadInventoryStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            tvSoonExpireCount.setText("0 件");
            tvWasteRate.setText("0.0%");
            return;
        }

        if (inventoryListener != null) {
            inventoryListener.remove();
        }

        inventoryListener = db.collection(INVENTORY_COLLECTION)
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        tvSoonExpireCount.setText("0 件");
                        tvWasteRate.setText("0.0%");
                        return;
                    }

                    updateInventoryStats(value);
                });
    }

    private void updateInventoryStats(QuerySnapshot snapshot) {
        int totalCount = 0;
        int soonExpireCount = 0;
        int expiredCount = 0;

        long todayStartMillis = getStartOfTodayMillis();

        for (DocumentSnapshot document : snapshot.getDocuments()) {
            Date expirationDate = extractExpirationDate(document.get("expirationDate"));
            if (expirationDate == null) {
                continue;
            }

            totalCount++;

            long dayDifference = getDayDifference(todayStartMillis, expirationDate);
            if (dayDifference < 0) {
                expiredCount++;
            } else if (dayDifference <= 2) {
                soonExpireCount++;
            }
        }

        tvSoonExpireCount.setText(soonExpireCount + " 件");
        tvWasteRate.setText(formatWasteRate(expiredCount, totalCount));
    }

    private Date extractExpirationDate(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        return null;
    }

    private long getStartOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getDayDifference(long todayStartMillis, Date expirationDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(expirationDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long expirationStartMillis = calendar.getTimeInMillis();
        return TimeUnit.MILLISECONDS.toDays(expirationStartMillis - todayStartMillis);
    }

    private String formatWasteRate(int expiredCount, int totalCount) {
        if (totalCount <= 0) {
            return "0.0%";
        }

        double wasteRate = (expiredCount * 100.0) / totalCount;
        return String.format(Locale.getDefault(), "%.1f%%", wasteRate);
    }
}
