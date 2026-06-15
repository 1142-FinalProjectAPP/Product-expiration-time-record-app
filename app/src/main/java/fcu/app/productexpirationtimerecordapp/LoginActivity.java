package fcu.app.productexpirationtimerecordapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editEmail, editPassword;
    private Button btnLogin;
    private TextView btnRegister, btnForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login); // 確保你的登入 XML 檔名為 activity_login

        // 確保你的佈局最外層有加上 android:id="@+id/main"，否則這裡可以改成 android.R.id.content
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化 Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // 綁定 XML 介面元件
        editEmail = findViewById(R.id.emailInput);
        editPassword = findViewById(R.id.passwordInput);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);

        // 1. 登入按鈕功能
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString().trim();
                String password = editPassword.getText().toString().trim();

                // 基礎防呆：檢查是否未輸入帳號密碼
                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "請輸入電子郵件與密碼！", Toast.LENGTH_SHORT).show();
                    return; // 停止往下執行
                }

                // 執行 Firebase 登入驗證
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(LoginActivity.this, "登入成功：" + user.getEmail(), Toast.LENGTH_SHORT).show();

                                    // 跳轉至主畫面 MainActivity
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish(); // 關閉登入畫面，避免按返回鍵又回到登入頁
                                } else {
                                    Toast.makeText(LoginActivity.this, "登入失敗：" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        // 2. 註冊按鈕功能 (跳轉至 SignUpActivity)
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        // 3. 新增：忘記密碼功能
        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString().trim();

                // 要求使用者必須先填寫 Email 才能寄送重置信件
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "請先在上方輸入您的電子郵件，再點選「忘記密碼」", Toast.LENGTH_LONG).show();
                    return;
                }

                // 呼叫 Firebase 發送密碼重置信件
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, "密碼重置信件已發送，請至信箱收取！", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this, "發送失敗：" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}