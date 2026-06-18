package fcu.app.productexpirationtimerecordapp;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class NoticeActivity extends AppCompatActivity {

    // 介面元件
    private ImageView btnBack;
    private SwitchCompat switchPush, switchWarning, switchExpired;
    private MaterialCardView btnTimePicker, btnFrequency;
    private TextView tvCurrentTime, tvCurrentFrequency;

    // SharedPreferences 用於儲存設定記憶
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "NoticeSettings";

    // 頻率選項清單
    private final String[] frequencyOptions = {"每日一次", "每兩天一次", "每週一次"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // 1. 綁定介面元件
        btnBack = findViewById(R.id.btnBack);
        switchPush = findViewById(R.id.switchPush);
        switchWarning = findViewById(R.id.switchWarning);
        switchExpired = findViewById(R.id.switchExpired);
        btnTimePicker = findViewById(R.id.btnTimePicker);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        btnFrequency = findViewById(R.id.btnFrequency);
        tvCurrentFrequency = findViewById(R.id.tvCurrentFrequency);

        // 2. 載入並套用之前儲存的設定
        loadSettings();

        // 3. 設定點擊與開關事件
        setupListeners();
    }

    /**
     * 載入 SharedPreferences 中的記憶設定
     */
    private void loadSettings() {
        // 載入開關狀態 (預設為 true)
        switchPush.setChecked(sharedPreferences.getBoolean("isPushEnabled", true));
        switchWarning.setChecked(sharedPreferences.getBoolean("isWarningEnabled", true));
        switchExpired.setChecked(sharedPreferences.getBoolean("isExpiredEnabled", true));

        // 如果總開關是關閉的，就讓下面兩個子開關無法點擊
        updateSubSwitchesState(switchPush.isChecked());

        // 載入時間 (預設早上 09:00)
        int savedHour = sharedPreferences.getInt("noticeHour", 9);
        int savedMinute = sharedPreferences.getInt("noticeMinute", 0);
        updateTimeText(savedHour, savedMinute);

        // 載入頻率 (預設 index 0: 每日一次)
        int savedFreqIndex = sharedPreferences.getInt("noticeFrequencyIndex", 0);
        tvCurrentFrequency.setText(frequencyOptions[savedFreqIndex]);
    }

    /**
     * 設定所有的監聽器 (Listeners)
     */
    private void setupListeners() {
        // 返回按鈕
        btnBack.setOnClickListener(v -> finish());

        // 推播通知總開關
        switchPush.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBooleanSetting("isPushEnabled", isChecked);
            updateSubSwitchesState(isChecked);
            String status = isChecked ? "已開啟" : "已關閉";
            Toast.makeText(this, "推播通知" + status, Toast.LENGTH_SHORT).show();
        });

        // 兩個子開關
        switchWarning.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveBooleanSetting("isWarningEnabled", isChecked)
        );
        switchExpired.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveBooleanSetting("isExpiredEnabled", isChecked)
        );

        // 時間選擇器按鈕
        btnTimePicker.setOnClickListener(v -> showTimePickerDialog());

        // 頻率選擇器按鈕
        btnFrequency.setOnClickListener(v -> showFrequencyDialog());
    }

    /**
     * 控制子開關的啟用狀態 (總開關關閉時，子開關變灰無法點擊)
     */
    private void updateSubSwitchesState(boolean isPushEnabled) {
        switchWarning.setEnabled(isPushEnabled);
        switchExpired.setEnabled(isPushEnabled);
        // 如果總開關關了，把子開關的視覺透明度調低，讓使用者知道它被禁用了
        switchWarning.setAlpha(isPushEnabled ? 1.0f : 0.5f);
        switchExpired.setAlpha(isPushEnabled ? 1.0f : 0.5f);
    }

    /**
     * 顯示 Android 內建的時間選擇器
     */
    private void showTimePickerDialog() {
        // 取得目前儲存的時間作為對話框的預設值
        int hour = sharedPreferences.getInt("noticeHour", 9);
        int minute = sharedPreferences.getInt("noticeMinute", 0);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    // 使用者點擊「確定」後，儲存時間並更新畫面
                    saveIntSetting("noticeHour", selectedHour);
                    saveIntSetting("noticeMinute", selectedMinute);
                    updateTimeText(selectedHour, selectedMinute);
                },
                hour,
                minute,
                false // 設為 false 會有 AM/PM 選項，設為 true 是 24 小時制
        );
        timePickerDialog.show();
    }

    /**
     * 顯示單選列表彈窗，讓使用者選擇頻率
     */
    private void showFrequencyDialog() {
        int currentSelection = sharedPreferences.getInt("noticeFrequencyIndex", 0);

        new AlertDialog.Builder(this)
                .setTitle("選擇提醒頻率")
                .setSingleChoiceItems(frequencyOptions, currentSelection, (dialog, which) -> {
                    // 儲存選中的 Index，並更新畫面
                    saveIntSetting("noticeFrequencyIndex", which);
                    tvCurrentFrequency.setText(frequencyOptions[which]);

                    Toast.makeText(this, "已設定為：" + frequencyOptions[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss(); // 選完自動關閉彈窗
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 輔助方法：更新時間文字的顯示格式 (例如：早上 09:00 或 下午 02:30)
     */
    private void updateTimeText(int hour, int minute) {
        String amPm = (hour < 12) ? "早上" : "下午";
        int displayHour = (hour > 12) ? hour - 12 : (hour == 0 ? 12 : hour);

        // 使用 String.format 確保分鐘如果是 0 會顯示成 00
        String timeString = String.format(Locale.getDefault(), "%s %02d:%02d", amPm, displayHour, minute);
        tvCurrentTime.setText(timeString);
    }

    /**
     * 輔助方法：將 Boolean 寫入 SharedPreferences
     */
    private void saveBooleanSetting(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    /**
     * 輔助方法：將 Integer 寫入 SharedPreferences
     */
    private void saveIntSetting(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }
}