//  File: app/src/main/java/com/example/smartbudget/OnboardingActivity.java
package com.example.smartbudget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smartbudget.databinding.ActivityOnboardingBinding;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

/** Однократный онбординг для новых пользователей. */
public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private SharedPreferences prefs;
    private final List<OnboardingItem> pages = Arrays.asList(
            new OnboardingItem(
                    R.drawable.ic_onboarding_1,
                    "6 кувшинов = 6 целей",
                    "Каждый ваш доход делится на шесть виртуальных «кувшинов»: "
                            + "Необходимое, Развлечения, Долгосрочные, Образование, "
                            + "Инвестиции и Благотворительность."
            ),
            new OnboardingItem(
                    R.drawable.ic_onboarding_2,
                    "Распределяйте доход",
                    "Установите проценты для каждого кувшина и нажмите "
                            + "«Распределить доход» — SmartBudget разложит деньги автоматически."
            ),
            new OnboardingItem(
                    R.drawable.ic_onboarding_3,
                    "Следите за расходами и целями",
                    "Фиксируйте траты, наблюдайте статистику и пополняйте финансовые цели, "
                            + "чтобы всегда знать, куда уходят деньги."
            )
    );

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* ViewPager + точки-индикаторы */
        binding.viewPager.setAdapter(new OnboardingAdapter(pages));
        new TabLayoutMediator(binding.tabDots, binding.viewPager,
                (t,p)->{}).attach();

        updateButtonText(0);

        /* ——— кнопки ——— */
        binding.btnNext.setOnClickListener(v -> {
            int pos = binding.viewPager.getCurrentItem();
            if (pos < pages.size()-1) {
                binding.viewPager.setCurrentItem(pos+1, true);
            } else {
                finishOnboarding();
            }
        });

        binding.btnSkip.setOnClickListener(v -> finishOnboarding());

        /* текст кнопки меняется при свайпе */
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { updateButtonText(position); }
        });
    }

    private void updateButtonText(int pos) {
        binding.btnNext.setText(pos == pages.size()-1 ? "Начать работу" : "Далее");
    }

    private void finishOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override public void onBackPressed() {
        int pos = binding.viewPager.getCurrentItem();
        if (pos == 0) super.onBackPressed();
        else binding.viewPager.setCurrentItem(pos-1, true);
    }
}
