package fcu.app.productexpirationtimerecordapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBack, ivAvatar;
    private MaterialCardView btnChangeAvatar;
    private TextInputEditText etName, etEmail, etPhone;
    private MaterialButton btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    // 儲存使用者選擇的圖片路徑
    private Uri selectedImageUri = null;

    // 🌟 註冊一個選擇圖片的 Launcher
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // 將選擇的圖片立刻顯示在畫面上預覽
                    ivAvatar.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        btnBack = findViewById(R.id.btnBack);
        ivAvatar = findViewById(R.id.ivAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);

        loadUserData();
        setupListeners();
    }

    private void loadUserData() {
        if (currentUser == null) return;

        etEmail.setText(currentUser.getEmail());

        // 🌟 如果 Auth 裡面已經有大頭貼的網址，就用 Glide 載入它
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .circleCrop() // 裁切成圓形
                    .into(ivAvatar);
        }

        db.collection("Users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name == null || name.isEmpty()) {
                            name = currentUser.getDisplayName();
                        }
                        etName.setText(name);

                        String phone = documentSnapshot.getString("phone");
                        if (phone != null) etPhone.setText(phone);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "資料載入失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // 🌟 點擊大頭貼或相機按鈕，呼叫系統相簿選圖片
        btnChangeAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        ivAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(this, "姓名不能為空喔！", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("儲存中...");

            // 判斷使用者有沒有選新照片
            if (selectedImageUri != null) {
                // 有選新照片 -> 先上傳照片，再更新資料
                uploadImageAndSave(newName, newPhone);
            } else {
                // 沒選新照片 -> 直接更新文字資料
                saveProfileData(newName, newPhone, null);
            }
        });
    }

    /**
     * 上傳圖片到 Firebase Storage
     */
    private void uploadImageAndSave(String newName, String newPhone) {
        // 設定圖片存在雲端的路徑： profile_images/使用者UID.jpg
        StorageReference fileRef = storage.getReference()
                .child("profile_images/" + currentUser.getUid() + ".jpg");

        // 開始上傳
        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // 上傳成功後，取得圖片的下載網址
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String photoUrl = uri.toString();
                        // 拿著網址，去更新資料庫
                        saveProfileData(newName, newPhone, photoUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("儲存修改");
                    Toast.makeText(this, "圖片上傳失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 更新 Firestore 與 Auth 資料
     */
    private void saveProfileData(String newName, String newPhone, String newPhotoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("phone", newPhone);
        if (newPhotoUrl != null) {
            updates.put("photoUrl", newPhotoUrl);
        }

        db.collection("Users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> updateAuthProfile(newName, newPhotoUrl))
                .addOnFailureListener(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("NOT_FOUND")) {
                        db.collection("Users").document(currentUser.getUid()).set(updates)
                                .addOnSuccessListener(aVoid -> updateAuthProfile(newName, newPhotoUrl));
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText("儲存修改");
                        Toast.makeText(this, "儲存失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * 更新 Auth 的基本資訊 (讓 currentUser 直接帶著名字和頭像網址)
     */
    private void updateAuthProfile(String newName, String newPhotoUrl) {
        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName);

        if (newPhotoUrl != null) {
            profileBuilder.setPhotoUri(Uri.parse(newPhotoUrl));
        }

        currentUser.updateProfile(profileBuilder.build())
                .addOnCompleteListener(task -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("儲存修改");

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "個人資料已成功更新！", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "更新失敗，請稍後再試", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}