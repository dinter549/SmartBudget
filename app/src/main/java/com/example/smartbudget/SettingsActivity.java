//  File: app/src/main/java/com/example/smartbudget/SettingsActivity.java
package com.example.smartbudget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.smartbudget.databinding.ActivitySettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * Экран «Настройки».
 * Убрана смена валюты; добавлен пункт «Помощь» с памяткой по методу 6 кувшинов.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    //───────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
        setupBottomNav();
    }

    /** Нижняя панель навигации (без изменений) */
    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_settings);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
                return true;

            } else if (id == R.id.nav_stats) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra(MainActivity.EXTRA_NAV_TO_STATS, true);
                startActivity(i);
                finish();
                return true;

            } else if (id == R.id.nav_goals) {
                double total = Double.longBitsToDouble(
                        PreferenceManager.getDefaultSharedPreferences(this)
                                .getLong("bal_total", 0));
                Intent g = new Intent(this, GoalsActivity.class);
                g.putExtra(GoalsActivity.EXTRA_TOTAL_BALANCE, total);
                g.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(g);
                return true;

            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }

    @Override protected void onResume() {
        super.onResume();
        binding.bottomNav.setSelectedItemId(R.id.nav_settings);
    }

    //───────────────────────────────────────────────────────────────
    /**
     * Preference-фрагмент настроек.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs_settings, rootKey);

            /* ── ТЁМНАЯ ТЕМА ── */
            Preference darkPref = findPreference("pref_dark_mode");
            if (darkPref != null) {
                darkPref.setOnPreferenceChangeListener((p, newVal) -> {
                    boolean dark = (Boolean) newVal;
                    AppCompatDelegate.setDefaultNightMode(
                            dark ? AppCompatDelegate.MODE_NIGHT_YES
                                    : AppCompatDelegate.MODE_NIGHT_NO);
                    return true;
                });
            }

            /* ── ПРОЦЕНТЫ 0–100 ── */
            initPercentField("pref_pct_necessary");
            initPercentField("pref_pct_entertainment");
            initPercentField("pref_pct_longterm");
            initPercentField("pref_pct_education");
            initPercentField("pref_pct_investments");
            initPercentField("pref_pct_charity");

            /* ── ПАМЯТКА «Помощь» ── */
            Preference helpPref = findPreference("pref_help");
            if (helpPref != null) {
                helpPref.setOnPreferenceClickListener(p -> {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Метод 6 кувшинов — рекомендации")
                            .setMessage(
                                    "• Процентное распределение кувшинов можно (и нужно) "
                                            + "подстроить под свои цели и образ жизни. \n\n"
                                            + "• Никогда не берите деньги из одного кувшина "
                                            + "на расходы другого. Дисциплина — ключ к результату. \n\n"
                                            + "• Не стремитесь «обнулить» кувшин к концу месяца. "
                                            + "Ненужные траты завтра могут оказаться нужными сегодня.")
                            .setPositiveButton("Понятно", null)
                            .show();
                    return true;
                });
            }

            /* ── СБРОС ДАННЫХ ── */
            Preference resetPref = findPreference("pref_reset");
            if (resetPref != null) {
                resetPref.setOnPreferenceClickListener(p -> {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Сбросить все данные?")
                            .setMessage("Все балансы, цели и история расходов будут удалены.")
                            .setPositiveButton("Сбросить", (d, w) -> performFullReset())
                            .setNegativeButton("Отмена", null)
                            .show();
                    return true;
                });
            }
        }

        /** Полный сброс SharedPreferences + перезапуск */
        private void performFullReset() {
            Context ctx = requireContext().getApplicationContext();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            sp.edit().clear().apply();

            Snackbar.make(requireView(), "Данные сброшены", Snackbar.LENGTH_SHORT).show();

            Intent restart = new Intent(ctx, MainActivity.class);
            restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(restart);
            requireActivity().finishAffinity();
        }

        /** Проверка процента 0…100 */
        private void initPercentField(String key) {
            EditTextPreference pref = findPreference(key);
            if (pref == null) return;

            pref.setOnBindEditTextListener(
                    edit -> edit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER));

            pref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());

            pref.setOnPreferenceChangeListener((p, newVal) -> {
                try {
                    int v = Integer.parseInt(String.valueOf(newVal));
                    if (v < 0 || v > 100) throw new NumberFormatException();
                    return true;
                } catch (NumberFormatException e) {
                    Snackbar.make(requireView(),
                            "Введите число от 0 до 100", Snackbar.LENGTH_LONG).show();
                    return false;
                }
            });
        }
    }
}
