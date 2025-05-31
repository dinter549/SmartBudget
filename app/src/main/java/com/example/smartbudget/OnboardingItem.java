//  File: app/src/main/java/com/example/smartbudget/OnboardingItem.java
package com.example.smartbudget;

import androidx.annotation.DrawableRes;

/** Данные одного онбординг-слайда. */
public class OnboardingItem {
    @DrawableRes public final int imageRes;
    public final String  title;
    public final String  description;

    public OnboardingItem(@DrawableRes int imageRes,
                          String title,
                          String description) {
        this.imageRes   = imageRes;
        this.title      = title;
        this.description= description;
    }
}
