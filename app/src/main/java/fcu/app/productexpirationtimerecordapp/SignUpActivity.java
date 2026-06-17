package fcu.app.productexpirationtimerecordapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword, editConfirmPassword;
    private Button btnRegister, btnGoogle; // 新增 Google 按鈕變數
    private TextView btnLogin;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient; // 新增 Google 登入客戶端
    private static final String TAG = "SignUpActivity"; // 新增 TAG 用於 Log

    // 建立一個接收 Google 登入視窗回傳結果的 Launcher
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        // Google 帳號驗證成功，取得帳戶資訊與 Token
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "Google登入成功，準備驗證Firebase: " + account.getId());

                        // 將 Token 交給 Firebase 進行最終註冊/登入
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        // Google 驗證失敗 (例如使用者關閉視窗)
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Google 驗證失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化 Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // 1. 綁定正確的 XML ID
        editName = findViewById(R.id.usernameInput);
        editEmail = findViewById(R.id.emailInput);
        editPassword = findViewById(R.id.passwordInput);
        editConfirmPassword = findViewById(R.id.confirmPasswordInput);
        btnRegister = findViewById(R.id.btnSignUp);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle); // 綁定註冊介面的 Google 按鈕 ID

        /* ========================
           一般信箱註冊區塊
           ======================== */

        // 2. 註冊按鈕點擊事件
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString().trim();
                String email = editEmail.getText().toString().trim();
                String password = editPassword.getText().toString().trim();
                String confirmPassword = editConfirmPassword.getText().toString().trim();

                if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "請填寫所有欄位！", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(SignUpActivity.this, "兩次輸入的密碼不一致！", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 執行 Firebase 註冊
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    if (user != null) {
                                        // 🌟 關鍵新增：建立更新個人資料的請求，將使用者輸入的名稱存入 Firebase 檔案
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(name) // 設定顯示名稱
                                                .build();

                                        // 執行更新
                                        user.updateProfile(profileUpdates)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> updateTask) {
                                                        if (updateTask.isSuccessful()) {

                                                            // 🌟【這裡新增】：同步將用戶名稱與 Email 寫入 Firestore 的 Users 集合中
                                                            Map<String, Object> userData = new HashMap<>();
                                                            userData.put("name", name);
                                                            userData.put("email", email);

                                                            FirebaseFirestore.getInstance().collection("Users").document(user.getUid())
                                                                    .set(userData)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        Toast.makeText(SignUpActivity.this, "註冊成功！", Toast.LENGTH_SHORT).show();
                                                                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                                                        finish();
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                } else {
                                    Toast.makeText(SignUpActivity.this, "註冊失敗：" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        // 3. 底部「立即登入」點擊事件
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        /* ========================
           Google 第三方登入/註冊區塊
           ======================== */

        // 1. 設定 Google 登入選項
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                // 備註：如果這裡的 R.string.default_web_client_id 報紅字，請改用之前教過的「直接貼上字串」硬派寫法！
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 2. 點擊 Google 按鈕時，呼叫帳號選擇視窗
        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    // 3. 將 Google 的 Token 傳給 Firebase 進行驗證的自訂方法
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(SignUpActivity.this, "Google 登入/註冊成功：" + user.getEmail(), Toast.LENGTH_SHORT).show();

                        // 註冊/登入成功後，直接跳轉到主畫面，並關閉當前頁面
                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Log.w(TAG, "firebaseAuthWithGoogle:failure", task.getException());
                        Toast.makeText(SignUpActivity.this, "Firebase 驗證失敗", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}