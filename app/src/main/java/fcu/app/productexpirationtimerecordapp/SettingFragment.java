package fcu.app.productexpirationtimerecordapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingFragment extends Fragment {

    private TextView tvUserName, tvUserEmail;
    private MaterialButton btnLogout;
    private LinearLayout btnGroupManagement, btnNotice;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingFragment newInstance(String param1, String param2) {
        SettingFragment fragment = new SettingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }
    // 將所有綁定與點擊事件寫在 onViewCreated，確保畫面已經完全載入
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 綁定介面元件 (對應 fragment_setting.xml 的 ID)
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnGroupManagement = view.findViewById(R.id.btnGroupManagement);
        btnNotice = view.findViewById(R.id.btnNotifications);


        // 2. 抓取 Firebase 目前登入的使用者資料
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // 顯示 Email
            tvUserEmail.setText(currentUser.getEmail());

            // 顯示使用者名稱 (若是 Google 登入通常會有名稱，信箱註冊可能沒有)
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvUserName.setText(displayName);
            } else {
                tvUserName.setText("FreshKeep 使用者");
            }
        }

        // 3. 群組管理按鈕點擊事件：跳轉至 GroupManagementActivity
        btnGroupManagement.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GroupManagementActivity.class);
            startActivity(intent);
        });

        btnNotice.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NoticeActivity.class);
            startActivity(intent);
        });

        // 4. 登出按鈕事件
        btnLogout.setOnClickListener(v -> {
            // Firebase 登出
            FirebaseAuth.getInstance().signOut();

            // Google 帳號同步登出 (重要：確保下次登入時會再次跳出帳號選單，不會自動登入上一個帳號)
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

            mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
                Toast.makeText(getActivity(), "已成功登出", Toast.LENGTH_SHORT).show();

                // 跳轉回 LoginActivity
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                // 加上這兩個 Flags 是為了清空 Activity 堆疊，避免使用者在登入頁按「返回鍵」又回到設定頁
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        });
    }
}