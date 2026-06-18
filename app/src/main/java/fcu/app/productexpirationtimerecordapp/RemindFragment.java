package fcu.app.productexpirationtimerecordapp;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RemindFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RemindFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView recyclerView;
    private RemindAdapter adapter;
    private List<RemindItem> itemList;

    // 介面元件
    private LinearLayout emptyStateContainer;
    private MaterialCardView summaryCard;
    private RelativeLayout listHeader;
    private TextView tvExpiredCount;

    public RemindFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RemindFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RemindFragment newInstance(String param1, String param2) {
        RemindFragment fragment = new RemindFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
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
        return inflater.inflate(R.layout.fragment_remind, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 綁定介面元件
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        summaryCard = view.findViewById(R.id.summaryCard);
        listHeader = view.findViewById(R.id.listHeader);
        tvExpiredCount = view.findViewById(R.id.tvExpiredCount);
        recyclerView = view.findViewById(R.id.recyclerViewReminders);

        // 2. 準備假資料 (模擬資料庫撈出來的商品)
        itemList = new ArrayList<>();
        itemList.add(new RemindItem("全脂牛奶", "今日到期", true));
        itemList.add(new RemindItem("有機酪梨", "1 天後到期", true));
        itemList.add(new RemindItem("嫩菠菜", "2 天後到期", false));
        itemList.add(new RemindItem("雞胸肉", "3 天後到期", false));

        // 3. 設定 RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RemindAdapter(itemList);
        recyclerView.setAdapter(adapter);

        // 4. 檢查初始狀態
        checkEmptyState();
    }

    /**
     * 檢查是否有商品，用來切換「空白狀態」與「列表狀態」
     */
    private void checkEmptyState() {
        if (itemList.isEmpty()) {
            // 沒有商品：顯示空白提示，隱藏列表與警告
            emptyStateContainer.setVisibility(View.VISIBLE);
            summaryCard.setVisibility(View.GONE);
            listHeader.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // 有商品：隱藏空白提示，顯示列表
            emptyStateContainer.setVisibility(View.GONE);
            listHeader.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);

            // 計算過期/緊急商品數量
            int warningCount = 0;
            for (RemindItem item : itemList) {
                if (item.isWarning) warningCount++;
            }

            if (warningCount > 0) {
                summaryCard.setVisibility(View.VISIBLE);
                tvExpiredCount.setText(warningCount + " 項食材即將過期");
            } else {
                summaryCard.setVisibility(View.GONE);
            }
        }
    }

    /**
     * ==========================================
     * 資料模型 (Model) - 代表單一個商品
     * ==========================================
     */
    public static class RemindItem {
        String name;
        String expiryDate;
        boolean isWarning;

        public RemindItem(String name, String expiryDate, boolean isWarning) {
            this.name = name;
            this.expiryDate = expiryDate;
            this.isWarning = isWarning;
        }
    }

    /**
     * ==========================================
     * 列表適配器 (Adapter) - 負責產生清單與滑動邏輯
     * ==========================================
     */
    public class RemindAdapter extends RecyclerView.Adapter<RemindAdapter.ViewHolder> {

        private List<RemindItem> items;
        // 定義左滑最大距離 (轉換 dp 為 pixel)，大約是兩個按鈕的寬度
        private final int MAX_SWIPE_DISTANCE = (int) (240 * getResources().getDisplayMetrics().density);

        public RemindAdapter(List<RemindItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_remind_swipe, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RemindItem item = items.get(position);
            holder.tvProductName.setText(item.name);
            holder.tvExpiryDate.setText(item.expiryDate);

            // 如果是緊急狀態，把字體變成橘色/紅色
            if (item.isWarning) {
                holder.tvExpiryDate.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                holder.tvExpiryDate.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }

            // 確保每次重新綁定時，卡片都在原位 (避免滑動狀態錯亂)
            holder.foregroundCard.setTranslationX(0);

            // 🌟 實作左滑觸控邏輯
            holder.foregroundCard.setOnTouchListener(new View.OnTouchListener() {
                private float startX, startY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - startX;
                            float dy = event.getRawY() - startY;

                            // 判斷是橫向滑動時，攔截事件不讓 RecyclerView 觸發上下滾動
                            if (Math.abs(dx) > Math.abs(dy)) {
                                v.getParent().requestDisallowInterceptTouchEvent(true);

                                // 只允許向左滑 (dx < 0)，且不超過設定的最大距離
                                if (dx < 0) {
                                    v.setTranslationX(Math.max(dx, -MAX_SWIPE_DISTANCE));
                                } else {
                                    v.setTranslationX(0); // 不允許向右滑
                                }
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // 放開手指時，判斷滑動距離是否過半
                            if (v.getTranslationX() < -MAX_SWIPE_DISTANCE / 2f) {
                                // 滑超過一半，自動吸附展開到底
                                v.animate().translationX(-MAX_SWIPE_DISTANCE).setDuration(200).start();
                            } else {
                                // 滑不到一半，彈回原位
                                v.animate().translationX(0).setDuration(200).start();
                            }
                            break;
                    }
                    return true;
                }
            });

            // 🌟 已吃完按鈕點擊事件
            holder.btnEaten.setOnClickListener(v -> {
                // 將卡片彈回原位
                holder.foregroundCard.animate().translationX(0).setDuration(200).withEndAction(() -> {
                    // 刪除該項目並更新畫面
                    removeItem(holder.getAdapterPosition());
                    Toast.makeText(getContext(), item.name + " 已標示為吃完", Toast.LENGTH_SHORT).show();
                }).start();
            });

            // 🌟 刪除按鈕點擊事件
            holder.btnDelete.setOnClickListener(v -> {
                holder.foregroundCard.animate().translationX(0).setDuration(200).withEndAction(() -> {
                    removeItem(holder.getAdapterPosition());
                    Toast.makeText(getContext(), item.name + " 已刪除", Toast.LENGTH_SHORT).show();
                }).start();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        // 自訂方法：移除商品並檢查空白狀態
        private void removeItem(int position) {
            if (position != RecyclerView.NO_POSITION && position < items.size()) {
                items.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, items.size());

                // 每次刪除後重新檢查是否清空了
                checkEmptyState();
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView foregroundCard;
            TextView tvProductName, tvExpiryDate;
            MaterialButton btnEaten, btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                foregroundCard = itemView.findViewById(R.id.foregroundCard);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
                btnEaten = itemView.findViewById(R.id.btnEaten);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}