package fcu.app.productexpirationtimerecordapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupManagementActivity extends AppCompatActivity {

    private ImageView btnBack;
    private MaterialButton btnInviteMember;
    private TextInputEditText etInviteCode;
    private MaterialButton btnConfirmJoin;
    private CardView btnShareLink;

    private TextView tvMemberCount;
    private LinearLayout emptyStateContainer;
    private LinearLayout memberListContainer;

    private FirebaseFirestore db;
    private String currentUserId;
    private String myActiveGroupCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_management);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnBack = findViewById(R.id.btnBack);
        btnInviteMember = findViewById(R.id.btnInviteMember);
        etInviteCode = findViewById(R.id.etInviteCode);
        btnConfirmJoin = findViewById(R.id.btnConfirmJoin);
        btnShareLink = findViewById(R.id.btnShareLink);

        tvMemberCount = findViewById(R.id.tvMemberCount);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        memberListContainer = findViewById(R.id.memberListContainer);

        btnBack.setOnClickListener(v -> finish());
        btnShareLink.setOnClickListener(v -> shareInviteLink());

        btnInviteMember.setOnClickListener(v -> {
            if (myActiveGroupCode != null) {
                showInviteCodeDialog(myActiveGroupCode);
            } else {
                btnInviteMember.setEnabled(false);
                btnInviteMember.setText("正在建立群組...");
                createGroupInFirestore();
            }
        });

        btnConfirmJoin.setOnClickListener(v -> {
            String inputCode = etInviteCode.getText().toString().trim().toUpperCase();
            if (inputCode.isEmpty()) {
                Toast.makeText(GroupManagementActivity.this, "請先輸入邀請碼！", Toast.LENGTH_SHORT).show();
                return;
            }
            btnConfirmJoin.setEnabled(false);
            btnConfirmJoin.setText("正在加入...");
            joinGroupWithInviteCode(inputCode);
        });

        checkIfUserHasGroup();
    }

    private void checkIfUserHasGroup() {
        db.collection("Groups")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot groupDoc = queryDocumentSnapshots.getDocuments().get(0);
                        myActiveGroupCode = groupDoc.getId();
                        btnInviteMember.setText("查看我的群組邀請碼");
                        listenToGroupMembers(myActiveGroupCode);
                    } else {
                        tvMemberCount.setText("0 位成員");
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        memberListContainer.setVisibility(View.GONE);
                    }
                });
    }

    private void listenToGroupMembers(String groupCode) {
        db.collection("Groups").document(groupCode)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    List<String> memberIds = (List<String>) snapshot.get("members");
                    String ownerId = snapshot.getString("ownerId");

                    if (memberIds != null) {
                        tvMemberCount.setText(memberIds.size() + " 位成員");
                        emptyStateContainer.setVisibility(View.GONE);
                        memberListContainer.setVisibility(View.VISIBLE);
                        memberListContainer.removeAllViews();

                        for (String uid : memberIds) {
                            fetchAndAddMemberRow(uid, ownerId);
                        }
                    }
                });
    }

    private void fetchAndAddMemberRow(String uid, String ownerId) {
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    String name = "未知使用者";
                    if (userDoc.exists()) {
                        name = userDoc.getString("name");
                    }

                    String role = uid.equals(ownerId) ? "群組擁有者 (管理員)" : "成員";
                    String cleanName = name; // 備份乾淨的名字供彈窗顯示

                    if (uid.equals(currentUserId)) {
                        name += " (我)";
                    }

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, 16, 0, 16);

                    CardView avatarCard = new CardView(this);
                    avatarCard.setRadius(40);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(80, 80);
                    cardParams.setMargins(0, 0, 24, 0);
                    avatarCard.setLayoutParams(cardParams);
                    avatarCard.setCardBackgroundColor(Color.parseColor("#E2EAE5"));

                    ImageView avatar = new ImageView(this);
                    avatar.setImageResource(android.R.drawable.ic_menu_myplaces);
                    avatar.setColorFilter(Color.parseColor("#00C49A"));
                    avatarCard.addView(avatar);
                    row.addView(avatarCard);

                    LinearLayout textBlock = new LinearLayout(this);
                    textBlock.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    textBlock.setLayoutParams(textParams);

                    TextView tvName = new TextView(this);
                    tvName.setText(name);
                    tvName.setTextSize(16);
                    tvName.setTextColor(Color.parseColor("#161d1a"));
                    tvName.setTypeface(null, Typeface.BOLD);

                    TextView tvRole = new TextView(this);
                    tvRole.setText(role);
                    tvRole.setTextSize(12);
                    tvRole.setTextColor(uid.equals(ownerId) ? Color.parseColor("#006c53") : Color.parseColor("#6c7a73"));

                    textBlock.addView(tvName);
                    textBlock.addView(tvRole);
                    row.addView(textBlock);

                    // 更多功能按鈕
                    ImageView btnMore = new ImageView(this);
                    btnMore.setImageResource(android.R.drawable.ic_menu_more);
                    btnMore.setColorFilter(Color.parseColor("#bbcac2"));

                    // 🌟【權限控管】：只有群組管理員，且該行「不是管理員自己」時，才啟動刪除按鈕
                    if (currentUserId.equals(ownerId) && !uid.equals(ownerId)) {
                        btnMore.setVisibility(View.VISIBLE);
                        android.util.TypedValue outValue = new android.util.TypedValue();
                        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
                        btnMore.setBackgroundResource(outValue.resourceId);                        btnMore.setClickable(true);
                        btnMore.setFocusable(true);

                        // 點擊觸發刪除確認確認彈窗
                        btnMore.setOnClickListener(v -> showDeleteConfirmDialog(uid, cleanName));
                    } else {
                        // 如果目前登入者不是管理員，或者這一列是管理員自己，就隱藏操作按鈕，防止越權
                        btnMore.setVisibility(View.GONE);
                    }

                    row.addView(btnMore);

                    View divider = new View(this);
                    // 修正大小寫：MATCH_parent 改為 MATCH_PARENT
                    LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
                    divider.setLayoutParams(divParams);
                    divider.setBackgroundColor(Color.parseColor("#1A6C7A73"));

                    memberListContainer.addView(row);
                    memberListContainer.addView(divider);
                });
    }

    /**
     * 🌟 新增：彈出移除成員確認對話框
     */
    private void showDeleteConfirmDialog(String memberUid, String memberName) {
        new AlertDialog.Builder(this)
                .setTitle("移除群組成員")
                .setMessage("您確定要將「" + memberName + "」從家庭冰箱群組中移除嗎？\n移除後他將無法共同管理存貨。")
                .setPositiveButton("確定移除", (dialog, which) -> {
                    // 執行刪除
                    removeMemberFromFirestore(memberUid);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 🌟 新增：從 Firestore 的群組名單中安全地移除成員
     */
    private void removeMemberFromFirestore(String memberUid) {
        if (myActiveGroupCode == null) return;

        // 使用 FieldValue.arrayRemove 原子操作，直接在雲端陣列中移除這個 UID
        db.collection("Groups").document(myActiveGroupCode)
                .update("members", FieldValue.arrayRemove(memberUid))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(GroupManagementActivity.this, "已成功將該成員移出群組", Toast.LENGTH_SHORT).show();
                    // 因為我們有設定 SnapshotListener 即時監聽，資料庫一變動，畫面會自動重新繪製，不需手動更新！
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GroupManagementActivity.this, "移除失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void joinGroupWithInviteCode(String inviteCode) {
        db.collection("Groups").document(inviteCode).get()
                .addOnCompleteListener(task -> {
                    btnConfirmJoin.setEnabled(true);
                    btnConfirmJoin.setText("確認加入");
                    if (task.isSuccessful() && task.getResult().exists()) {
                        db.collection("Groups").document(inviteCode)
                                .update("members", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(GroupManagementActivity.this, "成功加入群組！", Toast.LENGTH_SHORT).show();
                                    etInviteCode.setText("");
                                    etInviteCode.clearFocus();

                                    myActiveGroupCode = inviteCode;
                                    btnInviteMember.setText("查看我的群組邀請碼");
                                    listenToGroupMembers(myActiveGroupCode);
                                });
                    } else {
                        Toast.makeText(GroupManagementActivity.this, "驗證碼無效，請重新檢查！", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createGroupInFirestore() {
        String newInviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("inviteCode", newInviteCode);
        groupData.put("ownerId", currentUserId);
        groupData.put("members", Arrays.asList(currentUserId));
        groupData.put("createdAt", System.currentTimeMillis());

        db.collection("Groups").document(newInviteCode).set(groupData)
                .addOnSuccessListener(aVoid -> {
                    btnInviteMember.setEnabled(true);
                    myActiveGroupCode = newInviteCode;
                    btnInviteMember.setText("查看我的群組邀請碼");
                    showInviteCodeDialog(newInviteCode);
                    listenToGroupMembers(newInviteCode);
                })
                .addOnFailureListener(e -> {
                    btnInviteMember.setEnabled(true);
                    btnInviteMember.setText("邀請新成員");
                    Toast.makeText(GroupManagementActivity.this, "建立失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showInviteCodeDialog(String inviteCode) {
        new AlertDialog.Builder(this)
                .setTitle("您的專屬邀請碼")
                .setMessage("請將以下邀請碼提供給您的家人：\n\n【 " + inviteCode + " 】")
                .setPositiveButton("複製邀請碼", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("FreshKeep Invite Code", inviteCode);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "邀請碼已複製！", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("關閉", null)
                .show();
    }

    private void shareInviteLink() {
        String code = (myActiveGroupCode != null) ? myActiveGroupCode : "尚未建立群組";
        String shareMessage = "嗨！我想邀請你加入我的 FreshKeep 家庭冰箱群組！\n\n請在 App 中輸入邀請碼：【 " + code + " 】";
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "分享邀請至..."));
    }
}