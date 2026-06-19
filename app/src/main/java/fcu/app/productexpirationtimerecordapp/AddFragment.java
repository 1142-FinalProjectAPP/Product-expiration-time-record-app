package fcu.app.productexpirationtimerecordapp;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Calendar;

public class AddFragment extends Fragment {

    private TextView tvDate;
    private TextView tvQuantity;

    private int quantity = 1;

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

        tvDate = view.findViewById(R.id.tvDate);
        tvQuantity = view.findViewById(R.id.tvQuantity);

        TextView btnMinus = view.findViewById(R.id.btnMinus);
        TextView btnPlus = view.findViewById(R.id.btnPlus);

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
            Calendar calendar =
                    Calendar.getInstance();

            new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {

                        String date =
                                String.format(
                                        "%02d/%02d/%04d",
                                        month + 1,
                                        dayOfMonth,
                                        year
                                );

                        tvDate.setText(date);

                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        return view;
    }
}