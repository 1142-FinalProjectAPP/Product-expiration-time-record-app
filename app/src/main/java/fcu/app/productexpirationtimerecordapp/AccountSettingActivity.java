package fcu.app.productexpirationtimerecordapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountSettingActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout layoutPasswordChange;
    private TextInputEditText etOldPassword, etNewPassword;
    private MaterialButton btnUpdatePassword, btnDeleteAccount;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_setting);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        layoutPasswordChange = findViewById(R.id.layoutPasswordChange);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        btnBack.setOnClickListener(v -> finish());

        // 檢查登入方式
        checkLoginProvider();

        btnUpdatePassword.setOnClickListener(v -> updatePassword());
        btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    /**
     * 🌟 檢查是不是 Google 登入。如果是，密碼歸 Google 管，我們隱藏修改密碼區塊。
     */
    private void checkLoginProvider() {
        if (currentUser != null) {
            for (UserInfo userInfo : currentUser.getProviderData()) {
                if (userInfo.getProviderId().equals("google.com")) {
                    layoutPasswordChange.setVisibility(View.GONE);
                    break;
                }
            }
        }
    }

    /**
     * 🌟 修改密碼邏輯 (需要先用舊密碼重新驗證)
     */
    private void updatePassword() {
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (oldPass.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(this, "請輸入舊密碼與新密碼", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "新密碼至少需要 6 個字元", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("驗證與更新中...");

        // 1. 取得重新驗證的憑證
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPass);

        // 2. 重新驗證身分
        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 3. 驗證成功，正式更新密碼
                currentUser.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                    btnUpdatePassword.setEnabled(true);
                    btnUpdatePassword.setText("確認修改密碼");

                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, "密碼修改成功！", Toast.LENGTH_SHORT).show();
                        etOldPassword.setText("");
                        etNewPassword.setText("");
                    } else {
                        Toast.makeText(this, "密碼修改失敗：" + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                btnUpdatePassword.setEnabled(true);
                btnUpdatePassword.setText("確認修改密碼");
                Toast.makeText(this, "舊密碼錯誤，請重新輸入！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 顯示刪除帳號確認對話框
     */
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("警告：永久刪除帳號")
                .setMessage("這將會清除您在 FreshKeep 的所有資料，確定要繼續嗎？")
                .setPositiveButton("確定刪除", (dialog, which) -> executeAccountDeletion())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 🌟 執行刪除帳號流程 (清除 Firestore 資料 -> 清除 Auth -> 登出)
     */
    private void executeAccountDeletion() {
        btnDeleteAccount.setEnabled(false);
        btnDeleteAccount.setText("正在刪除資料...");

        String uid = currentUser.getUid();

        // 1. 先刪除資料庫 (Firestore) 裡的個人檔案
        db.collection("Users").document(uid).delete().addOnCompleteListener(task -> {

            // 2. 刪除 Firebase Auth 帳號
            currentUser.delete().addOnCompleteListener(deleteTask -> {
                if (deleteTask.isSuccessful()) {
                    // 3. 確保 Google 登入狀態也清除
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
                    GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
                    mGoogleSignInClient.signOut();

                    Toast.makeText(this, "帳號已成功刪除，期待與您再次相見！", Toast.LENGTH_LONG).show();

                    // 跳轉回登入頁，並清空 Activity 堆疊
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    // 🌟 如果太久沒登入，Firebase 會要求重新驗證才能刪除
                    btnDeleteAccount.setEnabled(true);
                    btnDeleteAccount.setText("永久刪除帳號");
                    Toast.makeText(this, "基於安全考量，請「登出後重新登入」再來執行刪除帳號！", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}