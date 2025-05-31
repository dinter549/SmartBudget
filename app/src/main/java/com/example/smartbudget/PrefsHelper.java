package com.example.smartbudget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

/**
 * Утилиты для (де-)сериализации крупного состояния в SharedPreferences:
 *   • список целей
 *   • история расходов шести кувшинов
 */
public class PrefsHelper {

    /* ---- цели ---- */
    private static final String KEY_GOALS     = "goals_json";
    private static final Type   LIST_GOAL_T   =
            new TypeToken<ArrayList<Goal>>() {}.getType();

    /* ---- истории расходов ---- */
    private static final String KEY_HISTORIES = "histories_json";
    private static final Type   MAP_HIST_T    =
            new TypeToken<EnumMap<Category, ArrayList<ExpenseEntry>>>() {}.getType();

    private static final Gson gson = new Gson();

    /* ─────────────────── цели ─────────────────── */

    public static void saveGoals(Context ctx, ArrayList<Goal> goals) {
        prefs(ctx).edit()
                .putString(KEY_GOALS, gson.toJson(goals, LIST_GOAL_T))
                .apply();
    }

    public static ArrayList<Goal> loadGoals(Context ctx) {
        String json = prefs(ctx).getString(KEY_GOALS, null);
        if (json == null) return new ArrayList<>();
        try { return gson.fromJson(json, LIST_GOAL_T); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    /* ────────────────── истории ────────────────── */

    public static void saveHistories(Context ctx,
                                     Map<Category, ArrayList<ExpenseEntry>> map) {
        prefs(ctx).edit()
                .putString(KEY_HISTORIES, gson.toJson(map, MAP_HIST_T))
                .apply();
    }

    public static EnumMap<Category, ArrayList<ExpenseEntry>> loadHistories(Context ctx) {
        String json = prefs(ctx).getString(KEY_HISTORIES, null);
        EnumMap<Category, ArrayList<ExpenseEntry>> map =
                new EnumMap<>(Category.class);

        if (json != null) {
            try { map.putAll(gson.<EnumMap<Category, ArrayList<ExpenseEntry>>>fromJson(json, MAP_HIST_T)); }
            catch (Exception ignored) {}
        }

        /* гарантируем, что у каждой категории есть список (пусть и пустой) */
        for (Category c : Category.values()) {
            map.putIfAbsent(c, new ArrayList<>());
        }
        return map;
    }

    /* ───────────────────────────────────────────── */

    private static SharedPreferences prefs(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }
}
