package fcu.app.productexpirationtimerecordapp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.ProgressBar;

import com.google.android.material.button.MaterialButton;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import android.graphics.Color;

import java.util.ArrayList;

public class InventoryFragment extends Fragment {

    private View rootView;

    private TextView tvVegPercent, tvOtherPercent, tvDrinkPercent, tvDairyPercent;
    private ProgressBar pbVeg, pbOther, pbDrink, pbDairy;

    private MaterialButton btnToggleChart;
    private View progressContainer;
    private PieChart pieChart;

    private boolean isChartMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_inventory, container, false);

        initView();
        updateUI();
        setupChart();

        btnToggleChart.setOnClickListener(v -> toggleView());

        return rootView;
    }

    private void initView() {

        tvVegPercent = rootView.findViewById(R.id.tvVegPercent);
        tvOtherPercent = rootView.findViewById(R.id.tvOtherPercent);
        tvDrinkPercent = rootView.findViewById(R.id.tvDrinkPercent);
        tvDairyPercent = rootView.findViewById(R.id.tvDairyPercent);

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

        // 🎨 每個分類顏色（清爽風格）
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#4CAF50")); // 蔬菜 - 綠
        colors.add(Color.parseColor("#9E9E9E")); // 其他 - 灰
        colors.add(Color.parseColor("#FF9800")); // 飲料 - 橘
        colors.add(Color.parseColor("#2196F3")); // 乳製品 - 藍

        dataSet.setColors(colors);

        // ✨ 圓餅間距 & 圓角感
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);

        // 🔤 字體變小 + 黑色
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.BLACK);

        pieChart.setData(data);

        // 🎯 中心文字（可選）
        pieChart.setCenterText("庫存分佈");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.BLACK);

        // 🎯 基本美化
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);

        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(true);

        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(10f);

        pieChart.invalidate();
    }
}