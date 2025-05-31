
package com.example.smartbudget;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbudget.databinding.ActivityAddGoalBinding;

import java.util.Calendar;
import java.util.Locale;


public class AddGoalActivity extends AppCompatActivity {

    public static final String EXTRA_GOAL = "extra_goal";

    private ActivityAddGoalBinding binding;
    private long pickedDateMillis = 0; // 0 = без срока

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddGoalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        binding.btnPickDate.setOnClickListener(v -> showDatePicker());


        binding.btnCreate.setOnClickListener(v -> createGoal());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance(Locale.getDefault());
        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, dayOfMonth, 0, 0, 0);
                    pickedDateMillis = sel.getTimeInMillis();
                    String txt = dayOfMonth + "." + (month+1) + "." + year;
                    binding.btnPickDate.setText(txt);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void createGoal() {
        String name = binding.etGoalName.getText().toString().trim();
        String txtTarget = binding.etTargetAmount.getText().toString()
                .replace(',', '.').trim();

        double target;
        try { target = Double.parseDouble(txtTarget); }
        catch (NumberFormatException e) { target = -1; }

        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show();
            return;
        }
        if (target <= 0) {
            Toast.makeText(this, "Введите корректную сумму (>0)", Toast.LENGTH_SHORT).show();
            return;
        }

        Goal g = new Goal(name, target, pickedDateMillis);

        Intent data = new Intent();
        data.putExtra(EXTRA_GOAL, g);
        setResult(RESULT_OK, data);
        finish();
    }
}
