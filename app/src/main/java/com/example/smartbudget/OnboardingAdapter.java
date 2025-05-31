//  File: app/src/main/java/com/example/smartbudget/OnboardingAdapter.java
package com.example.smartbudget;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbudget.databinding.OnboardingPageBinding;

import java.util.List;

/** Адаптер, показывающий страницы онбординга во ViewPager2. */
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

    private final List<OnboardingItem> pages;

    public OnboardingAdapter(List<OnboardingItem> pages) { this.pages = pages; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OnboardingPageBinding b = OnboardingPageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        OnboardingItem p = pages.get(pos);
        h.image.setImageResource(p.imageRes);
        h.title.setText(p.title);
        h.desc .setText(p.description);
    }

    @Override public int getItemCount() { return pages.size(); }

    /* ─────────────────────── */
    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView  title, desc;
        VH(OnboardingPageBinding b){
            super(b.getRoot());
            image = b.ivIllustration;
            title = b.tvTitle;
            desc  = b.tvDesc;
        }
    }
}

