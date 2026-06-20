package fcu.app.productexpirationtimerecordapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddFragment extends Fragment {

    private TextView tvDate;
    private TextView tvQuantity;
    private TextInputEditText etFoodName;
    private ChipGroup categoryGroup;
    private MaterialButton btnAddInventory;
    private MaterialButton btnScanBarcode;
    private Chip chipAddCategory; // 新增類別的按鈕

    private int quantity = 1;
    private Calendar selectedCalendar;
    private FirebaseFirestore db;
    private OkHttpClient httpClient;

    public AddFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add, container, false);

        db = FirebaseFirestore.getInstance();
        httpClient = new OkHttpClient();
        selectedCalendar = Calendar.getInstance();

        tvDate = view.findViewById(R.id.tvDate);
        tvQuantity = view.findViewById(R.id.tvQuantity);
        etFoodName = view.findViewById(R.id.etFoodName);
        categoryGroup = view.findViewById(R.id.categoryGroup);
        btnAddInventory = view.findViewById(R.id.btnAddInventory);
        btnScanBarcode = view.findViewById(R.id.btnScanBarcode);
        chipAddCategory = view.findViewById(R.id.chipAddCategory); // 綁定新增類別按鈕

        TextView btnMinus = view.findViewById(R.id.btnMinus);
        TextView btnPlus = view.findViewById(R.id.btnPlus);

        updateDateText();

        // 條碼掃描功能
        btnScanBarcode.setOnClickListener(v -> startBarcodeScan());

        // 數量加減
        btnPlus.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        // 日期選擇
        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0);
                        selectedCalendar.set(Calendar.MILLISECOND, 0);
                        updateDateText();
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // 動態新增類別按鈕的監聽器
        chipAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // 加入存貨
        btnAddInventory.setOnClickListener(v -> saveDataToFirestore());

        return view;
    }

    // 顯示輸入自訂類別的對話框
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("新增自訂類別");

        // 建立一個輸入框
        final TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("例如：海鮮、冷凍食品");
        input.setSingleLine(true);

        // 加一層 Layout 設定邊距，讓 UI 比較好看
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setPadding(60, 20, 60, 0);
        layout.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton("新增", (dialog, which) -> {
            String newCategoryName = input.getText().toString().trim();
            if (!newCategoryName.isEmpty()) {
                addNewCategoryChip(newCategoryName);
            } else {
                Toast.makeText(requireContext(), "類別名稱不能為空", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // 在畫面上動態產生一個新的 Chip
    private void addNewCategoryChip(String categoryName) {
        Chip newChip = new Chip(requireContext());
        newChip.setText(categoryName);
        newChip.setCheckable(true);
        // 設定與原本標籤類似的外觀
        newChip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#00C49A")));
        newChip.setChipStrokeWidth(getResources().getDisplayMetrics().density * 1); // 1dp 邊框

        // 將新標籤加入到 ChipGroup 中，並放在「+ 新增」按鈕的前一個位置
        int childCount = categoryGroup.getChildCount();
        categoryGroup.addView(newChip, childCount - 1);

        // 自動勾選剛建立的標籤
        categoryGroup.check(newChip.getId());
    }

    private void startBarcodeScan() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(requireContext(), options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    Toast.makeText(requireContext(), "掃描成功，正在查詢資料...", Toast.LENGTH_SHORT).show();
                    fetchProductInfoFromApi(rawValue);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "掃描發生錯誤：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 呼叫 Open Food Facts API 查詢商品名稱 (升級除錯版)
    private void fetchProductInfoFromApi(String barcode) {
        String url = "https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json";

        // 在 Android Studio 的 Logcat 印出網址，方便我們追蹤
        Log.d("API_TEST", "正在查詢網址: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "ProductExpirationRecordApp/1.0") // 加上這行報上名來
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API_TEST", "網路連線失敗: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "網路連線失敗：" + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // (200 OK) 成功抓到資料，維持原本的解析邏輯
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);

                        if (json.has("status") && json.getInt("status") == 1 && json.has("product")) {
                            JSONObject product = json.getJSONObject("product");

                            String productName = product.optString("product_name", "");
                            if (productName.isEmpty()) productName = product.optString("product_name_zh", "");
                            if (productName.isEmpty()) productName = product.optString("generic_name", "");

                            final String finalName = productName;
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (!finalName.isEmpty()) {
                                        etFoodName.setText(finalName);
                                        Toast.makeText(requireContext(), "已自動帶入：" + finalName, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "找到商品，但資料庫沒建檔名稱", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 404) {
                    // --- 新增：專門處理 404 找不到商品的情況 ---
                    Log.d("API_TEST", "資料庫中沒有此商品 (404)");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "公開資料庫找不到此條碼，請手動輸入名稱", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    // 其他真正的伺服器當機或錯誤 (例如 500)
                    Log.e("API_TEST", "伺服器錯誤碼: " + response.code());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "伺服器異常 (錯誤碼: " + response.code() + ")", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }

    private void updateDateText() {
        String date = String.format("%02d/%02d/%04d",
                selectedCalendar.get(Calendar.MONTH) + 1,
                selectedCalendar.get(Calendar.DAY_OF_MONTH),
                selectedCalendar.get(Calendar.YEAR));
        tvDate.setText(date);
    }

    private void saveDataToFirestore() {
        String productName = etFoodName.getText().toString().trim();
        if (productName.isEmpty()) {
            Toast.makeText(requireContext(), "請輸入食材名稱", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- 重大邏輯修改：動態抓取被選中標籤的文字 ---
        int checkedChipId = categoryGroup.getCheckedChipId();
        // 確保使用者有選擇標籤，且選擇的不是「+ 新增」按鈕本身
        if (checkedChipId == View.NO_ID || checkedChipId == R.id.chipAddCategory) {
            Toast.makeText(requireContext(), "請選擇一個有效的類別", Toast.LENGTH_SHORT).show();
            return;
        }

        // 透過被選中的 ID，去抓取該 Chip 元件，然後讀取上面的文字
        Chip selectedChip = categoryGroup.findViewById(checkedChipId);
        String category = selectedChip.getText().toString();

        Timestamp expirationDate = new Timestamp(selectedCalendar.getTime());

        Map<String, Object> inventoryData = new HashMap<>();
        inventoryData.put("productName", productName);
        inventoryData.put("category", category); // 這裡現在會存入動態產生的文字了！
        inventoryData.put("expirationDate", expirationDate);
        inventoryData.put("quantity", quantity);

        btnAddInventory.setEnabled(false);

        db.collection("Inventory")
                .add(inventoryData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "食材已成功加入存貨！", Toast.LENGTH_SHORT).show();
                    btnAddInventory.setEnabled(true);

                    // 重設表單狀態
                    etFoodName.setText("");
                    quantity = 1;
                    tvQuantity.setText("1");
                    categoryGroup.check(R.id.chipFruit);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "加入失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnAddInventory.setEnabled(true);
                });
    }
}