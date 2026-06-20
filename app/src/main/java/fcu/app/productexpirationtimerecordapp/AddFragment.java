package fcu.app.productexpirationtimerecordapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddFragment extends Fragment {

    private TextView tvDate;
    private TextView tvQuantity;
    private TextInputEditText etFoodName;
    private ChipGroup categoryGroup;
    private MaterialButton btnAddInventory;

    private int quantity = 1;
    // 用來記錄使用者選擇的確切時間，方便直接轉成 Firestore 的 Timestamp
    private Calendar selectedCalendar;

    // 宣告 Firestore 實例
    private FirebaseFirestore db;

    public AddFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_add,
                container,
                false
        );

        // 初始化 Firestore
        db = FirebaseFirestore.getInstance();

        // 初始化日期為今天
        selectedCalendar = Calendar.getInstance();

        // 綁定 UI 元件
        tvDate = view.findViewById(R.id.tvDate);
        tvQuantity = view.findViewById(R.id.tvQuantity);
        etFoodName = view.findViewById(R.id.etFoodName);
        categoryGroup = view.findViewById(R.id.categoryGroup);
        btnAddInventory = view.findViewById(R.id.btnAddInventory);

        TextView btnMinus = view.findViewById(R.id.btnMinus);
        TextView btnPlus = view.findViewById(R.id.btnPlus);

        // 初始化預設顯示的日期 (格式：MM/dd/yyyy)
        updateDateText();

        btnPlus.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });

        btnMinus.setOnClickListener(v -> {
            if(quantity > 1){
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        // 將使用者選擇的日期存入 Calendar，並將時間設為午夜 12:00:00
                        selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0);
                        selectedCalendar.set(Calendar.MILLISECOND, 0);

                        updateDateText();
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // 點擊「加入存貨」按鈕，執行上傳資料庫的邏輯
        btnAddInventory.setOnClickListener(v -> {
            saveDataToFirestore();
        });

        return view;
    }

    private void updateDateText() {
        String date = String.format("%02d/%02d/%04d",
                selectedCalendar.get(Calendar.MONTH) + 1,
                selectedCalendar.get(Calendar.DAY_OF_MONTH),
                selectedCalendar.get(Calendar.YEAR));
        tvDate.setText(date);
    }

    private void saveDataToFirestore() {
        // 1. 取得食材名稱
        String productName = etFoodName.getText().toString().trim();
        if (productName.isEmpty()) {
            Toast.makeText(requireContext(), "請輸入食材名稱", Toast.LENGTH_SHORT).show();
            return; // 終止執行，要求使用者填寫
        }

        // 2. 取得選擇的類別
        String category = "";
        int checkedChipId = categoryGroup.getCheckedChipId();
        if (checkedChipId == R.id.chipVegetable) {
            category = "蔬菜";
        } else if (checkedChipId == R.id.chipFruit) {
            category = "水果";
        } else if (checkedChipId == R.id.chipMilk) {
            category = "乳製品";
        } else if (checkedChipId == R.id.chipMeat) {
            category = "肉類";
        } else {
            Toast.makeText(requireContext(), "請選擇一個類別", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 轉換日期為 Firestore 的 Timestamp 格式
        Timestamp expirationDate = new Timestamp(selectedCalendar.getTime());

        // 4. 將資料打包進 Map (對應你設定的欄位名稱與型態)
        Map<String, Object> inventoryData = new HashMap<>();
        inventoryData.put("productName", productName);
        inventoryData.put("category", category);
        inventoryData.put("expirationDate", expirationDate);
        inventoryData.put("quantity", quantity);

        // 5. 寫入 Cloud Firestore (這裡將 Collection 命名為 "Inventory")
        // 如果這個 Collection 不存在，Firestore 會自動幫你建立
        btnAddInventory.setEnabled(false); // 暫時停用按鈕，防止重複連點

        db.collection("inventory")
                .add(inventoryData)
                .addOnSuccessListener(documentReference -> {
                    // 寫入成功
                    Toast.makeText(requireContext(), "食材已成功加入存貨！", Toast.LENGTH_SHORT).show();

                    // 恢復按鈕狀態並清空表單，方便輸入下一筆
                    btnAddInventory.setEnabled(true);
                    etFoodName.setText("");
                    quantity = 1;
                    tvQuantity.setText("1");
                    categoryGroup.check(R.id.chipFruit); // 預設跳回水果
                })
                .addOnFailureListener(e -> {
                    // 寫入失敗
                    Toast.makeText(requireContext(), "加入失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnAddInventory.setEnabled(true);
                });
    }
}