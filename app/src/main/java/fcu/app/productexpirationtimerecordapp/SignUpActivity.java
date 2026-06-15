package fcu.app.productexpirationtimerecordapp;

import android.os.Bundle;
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

public class SignUpActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword, editConfirmPassword;
    private Button btnRegister;
    private TextView btnLogin; // 加上底部「立即登入」的按鈕綁定
    private FirebaseAuth mAuth;

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

        // 2. 註冊按鈕點擊事件
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 取得輸入文字並去除前後空白
                String name = editName.getText().toString().trim();
                String email = editEmail.getText().toString().trim();
                String password = editPassword.getText().toString().trim();
                String confirmPassword = editConfirmPassword.getText().toString().trim();

                // 基礎防呆：檢查是否有欄位空白
                if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "請填寫所有欄位！", Toast.LENGTH_SHORT).show();
                    return; // 終止執行，不往 Firebase 送資料
                }

                // 基礎防呆：檢查兩次密碼是否一致
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
                                    Toast.makeText(SignUpActivity.this, "註冊成功：" + user.getEmail(), Toast.LENGTH_SHORT).show();

                                    // 註冊成功後，自動關閉此頁面返回登入頁
                                    finish();
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
                // 結束當前註冊頁面，返回上一個（登入）頁面
                finish();
            }
        });
    }
}