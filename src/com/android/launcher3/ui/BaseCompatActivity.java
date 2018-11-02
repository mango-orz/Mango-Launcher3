package com.android.launcher3.ui;


import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.R;
import com.android.launcher3.util.security.PermissionManager;

/**
 * @author tic
 * created on 18-9-17
 */
public abstract class BaseCompatActivity extends AppCompatActivity implements PermissionManager.Callback {

    private String TAG = getClass().getSimpleName();

    private ViewGroup mContent;
    private PermissionManager mSecurity;
    private ActionBar mActionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        mContent = findViewById(R.id.main_content);

        mActionBar = getActionBar();
        if (mActionBar == null) {
            Log.i(TAG, "No actionBar");
        }

        View view = loadLayout(mContent);
        if (view != null) {
            mContent.removeAllViews();
            mContent.addView(view);
        }

        mSecurity = new PermissionManager(this);
        mSecurity.requestPermission();
    }

    protected void showHomeButton() {
        if (mActionBar == null) {
            Log.i(TAG, "No actionBar found");
        } else {
            mActionBar.setDisplayShowHomeEnabled(true);
        }
    }

    protected void showBackButton() {
        if (mActionBar == null) {
            Log.i(TAG, "No actionBar found");
        } else {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public void setTitle(CharSequence charSequence) {
        if (mActionBar != null) {
            mActionBar.setTitle(charSequence);
        } else {
            super.setTitle(charSequence);
        }
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getString(titleId));
    }

    private View loadLayout(ViewGroup group) {
        int layout = layout();
        if (layout == 0) {
            throw new RuntimeException("please override layout method");
        } else {
            return LayoutInflater.from(this).inflate(layout, group, false);
        }
    }

    protected abstract int layout();

    protected void addView(View view, int index) {
        mContent.addView(view, index);
    }

    protected void addView(View view) {
        mContent.removeAllViews();
        mContent.addView(view);
    }

    @Override
    public void onPermissionRefuse(@NonNull String permissions) {

    }

    @Override
    public Activity getActivity() {
        return this;
    }
}
