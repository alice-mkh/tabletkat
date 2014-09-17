package org.exalm.tabletkat.launcher;

import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.exalm.tabletkat.IMod;
import org.exalm.tabletkat.R;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class LauncherMod implements IMod {
    private Class mHoloImageViewClass;

    private int drawable_divider_launcher_holo;
    private int drawable_ic_home_all_apps;

    private int id_dock_divider;
    private int id_drag_target_bar;
    private int id_hotseat;
    private int id_paged_view_indicator;
    private int id_qsb_divider;
    private int id_qsb_search_bar;
    private int id_search_button;
    private int id_voice_button;
    private int id_voice_button_proxy;
    private int id_workspace;

    private int string_accessibility_all_apps_button;
    private int string_accessibility_search_button;
    private int string_accessibility_voice_search_button;

    private String mCurrentPackage;

    public void addHooks(String packageName, ClassLoader cl) {
        mCurrentPackage = packageName;
        if (packageName.equals("com.android.launcher")){
            packageName += "2";
        }
        mHoloImageViewClass = XposedHelpers.findClass(packageName + ".HolographicImageView", cl);
    }

    @Override
    public void addHooks(ClassLoader cl) {

    }

    private void removeView(View container, int id) {
        View v = container.findViewById(id);
        if (v == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) v.getParent();
        parent.removeView(v);
    }

    @Override
    public void initResources(XResources res, XModuleResources res2) {
        String pkg = res.getPackageName();

        res.setReplacement(pkg, "bool", "allow_rotation", true);
        res.setReplacement(pkg, "bool", "config_workspaceFadeAdjacentScreens", true);
        res.setReplacement(pkg, "bool", "config_useDropTargetDownTransition", true);
        res.setReplacement(pkg, "bool", "is_large_screen", true);

        res.setReplacement(pkg, "dimen", "button_bar_height", 0);
        res.setReplacement(pkg, "dimen", "button_bar_height_top_padding", 0);
        res.setReplacement(pkg, "dimen", "button_bar_height_bottom_padding", 0);
        res.setReplacement(pkg, "dimen", "button_bar_height_plus_padding", 0);
        res.setReplacement(pkg, "dimen", "button_bar_width_left_padding", 0);
        res.setReplacement(pkg, "dimen", "button_bar_width_right_padding", 0);

        res.setReplacement(pkg, "integer", "cell_count_x", res2.fwd(R.integer.launcher2_cell_count_x));
        res.setReplacement(pkg, "integer", "cell_count_y", res2.fwd(R.integer.launcher2_cell_count_y));

        res.setReplacement(pkg, "dimen", "cell_layout_left_padding_port", res2.fwd(R.dimen.launcher2_cell_layout_left_padding_port));
        res.setReplacement(pkg, "dimen", "cell_layout_left_padding_land", res2.fwd(R.dimen.launcher2_cell_layout_left_padding_land));
        res.setReplacement(pkg, "dimen", "cell_layout_right_padding_port", res2.fwd(R.dimen.launcher2_cell_layout_right_padding_port));
        res.setReplacement(pkg, "dimen", "cell_layout_right_padding_land", res2.fwd(R.dimen.launcher2_cell_layout_right_padding_land));
        res.setReplacement(pkg, "dimen", "cell_layout_top_padding_port", res2.fwd(R.dimen.launcher2_cell_layout_top_padding_port));
        res.setReplacement(pkg, "dimen", "cell_layout_top_padding_land", res2.fwd(R.dimen.launcher2_cell_layout_top_padding_land));
        res.setReplacement(pkg, "dimen", "cell_layout_bottom_padding_port", res2.fwd(R.dimen.launcher2_cell_layout_bottom_padding_port));
        res.setReplacement(pkg, "dimen", "cell_layout_bottom_padding_land", res2.fwd(R.dimen.launcher2_cell_layout_bottom_padding_land));

        res.setReplacement(pkg, "dimen", "qsb_bar_height", res2.fwd(R.dimen.launcher2_qsb_bar_height));
        res.setReplacement(pkg, "dimen", "qsb_bar_height_inset", res2.fwd(R.dimen.launcher2_qsb_bar_height_inset));
        res.setReplacement(pkg, "dimen", "search_bar_height", res2.fwd(R.dimen.launcher2_qsb_bar_height));

        res.setReplacement(pkg, "dimen", "workspace_top_padding_land", res2.fwd(R.dimen.launcher2_workspace_top_padding));
        res.setReplacement(pkg, "dimen", "workspace_top_padding_port", res2.fwd(R.dimen.launcher2_workspace_top_padding));
        res.setReplacement(pkg, "dimen", "workspace_bottom_padding_land", 0);
        res.setReplacement(pkg, "dimen", "workspace_bottom_padding_port", 0);
        res.setReplacement(pkg, "dimen", "workspace_left_padding_land", 0);
        res.setReplacement(pkg, "dimen", "workspace_left_padding_port", 0);
        res.setReplacement(pkg, "dimen", "workspace_right_padding_land", 0);
        res.setReplacement(pkg, "dimen", "workspace_right_padding_port", 0);

        res.setReplacement(pkg, "dimen", "workspace_width_gap_land", res2.fwd(R.dimen.launcher2_workspace_width_gap_land));
        res.setReplacement(pkg, "dimen", "workspace_width_gap_port", res2.fwd(R.dimen.launcher2_workspace_width_gap_port));
        res.setReplacement(pkg, "dimen", "workspace_height_gap_land", res2.fwd(R.dimen.launcher2_workspace_height_gap_land));
        res.setReplacement(pkg, "dimen", "workspace_height_gap_port", res2.fwd(R.dimen.launcher2_workspace_height_gap_port));

        res.setReplacement(pkg, "drawable", "workspace_bg", new XResources.DrawableLoader() {
            @Override
            public Drawable newDrawable(XResources res, int id) throws Throwable {
                return new ColorDrawable(Color.TRANSPARENT);
            }
        });

        res.setReplacement(pkg, "drawable", "divider_launcher_holo", res2.fwd(R.drawable.launcher2_divider_launcher_holo));
        drawable_divider_launcher_holo = res.getIdentifier("divider_launcher_holo", "drawable", pkg);
        drawable_ic_home_all_apps = res.addResource(res2, res2.fwd(R.drawable.ic_home_all_apps_holo_dark).getId());

        id_dock_divider = res.getIdentifier("dock_divider", "id", pkg);
        id_drag_target_bar = res.getIdentifier("drag_target_bar", "id", pkg);
        id_hotseat = res.getIdentifier("hotseat", "id", pkg);
        id_paged_view_indicator = res.getIdentifier("paged_view_indicator", "id", pkg);
        id_qsb_divider = res.getIdentifier("qsb_divider", "id", pkg);
        id_qsb_search_bar = res.getIdentifier("qsb_search_bar", "id", pkg);
        id_search_button = res.getIdentifier("search_button", "id", pkg);
        id_voice_button = res.getIdentifier("voice_button", "id", pkg);
        id_voice_button_proxy = res.getIdentifier("voice_button_proxy", "id", pkg);
        id_workspace = res.getIdentifier("workspace", "id", pkg);

        string_accessibility_all_apps_button = res.getIdentifier("accessibility_all_apps_button", "string", pkg);
        string_accessibility_search_button = res.getIdentifier("accessibility_search_button", "string", pkg);
        string_accessibility_voice_search_button = res.getIdentifier("accessibility_voice_search_button", "string", pkg);

        res.hookLayout(pkg, "layout", "launcher", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam layoutInflatedParam) throws Throwable {
                View v = layoutInflatedParam.view;

                DisplayMetrics d = v.getResources().getDisplayMetrics();
                int dimen_qsb_bar_height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, d);
                int workspace_padding_top = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, d);

                v.findViewById(id_hotseat).setVisibility(View.GONE);

                removeView(v, id_dock_divider);
                removeView(v, id_paged_view_indicator);

                removeView(v, id_qsb_divider);

                v.findViewById(id_voice_button_proxy).setClickable(false);
                v.findViewById(id_voice_button_proxy).setFocusable(false);

                v.findViewById(id_workspace).setPadding(0, workspace_padding_top, 0, 0);

                ViewGroup qsbContainer = (ViewGroup) v.findViewById(id_drag_target_bar).getParent();

                qsbContainer.setPadding(0, 0, 0, 0);
                FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) qsbContainer.getLayoutParams();
                p.height = dimen_qsb_bar_height;
                p.width = ViewGroup.LayoutParams.MATCH_PARENT;
                p.gravity = Gravity.TOP;

                qsbContainer.setLayoutParams(p);
                qsbContainer.requestLayout();

                View qsb = v.findViewById(id_qsb_search_bar);
                RelativeLayout l = new RelativeLayout(qsbContainer.getContext());
                l.setId(qsb.getId());
                qsbContainer.removeView(qsb);
                qsbContainer.addView(l, 0);
                createTabletActionBar(l);
                XposedHelpers.callMethod(qsbContainer, "onFinishInflate");
            }
        });
    }

    private void createTabletActionBar(RelativeLayout l){
        DisplayMetrics d = l.getResources().getDisplayMetrics();
        int dimen_toolbar_button_horizontal_padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, d);

        ImageView searchButton = (ImageView) XposedHelpers.newInstance(mHoloImageViewClass, l.getContext());
        searchButton.setImageResource(drawable_ic_home_all_apps);
        searchButton.setAdjustViewBounds(true);
        searchButton.setFocusable(true);
        searchButton.setClickable(true);
        searchButton.setId(id_search_button);
        RelativeLayout.LayoutParams searchParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        searchParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        searchParams.addRule(RelativeLayout.CENTER_VERTICAL);
        searchButton.setLayoutParams(searchParams);
        searchButton.setPadding(
                dimen_toolbar_button_horizontal_padding,
                0,
                dimen_toolbar_button_horizontal_padding,
                0
        );
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XposedHelpers.callMethod(v.getContext(), "onClickSearchButton", v);
            }
        });
        searchButton.setContentDescription(searchButton.getResources().getString(string_accessibility_search_button));
        l.addView(searchButton);

        ImageView searchDivider = new ImageButton(l.getContext());
        searchDivider.setImageResource(drawable_divider_launcher_holo);
        searchDivider.setId(TkR.id.battery_text);
        searchDivider.setBackground(null);
        searchDivider.setClickable(true);
        searchDivider.setFocusable(false);
        RelativeLayout.LayoutParams searchDividerParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        searchDividerParams.addRule(RelativeLayout.END_OF, searchButton.getId());
        searchDivider.setLayoutParams(searchDividerParams);
        searchDivider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XposedHelpers.callMethod(v.getContext(), "onClickSearchButton", v);
            }
        });
        l.addView(searchDivider);

        ImageView voiceButton = (ImageView) XposedHelpers.newInstance(mHoloImageViewClass, l.getContext());
        voiceButton.setImageResource(drawable_ic_home_all_apps);
        voiceButton.setAdjustViewBounds(true);
        voiceButton.setFocusable(true);
        voiceButton.setClickable(true);
        voiceButton.setId(id_voice_button);
        RelativeLayout.LayoutParams voiceParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        voiceParams.addRule(RelativeLayout.END_OF, searchDivider.getId());
        voiceButton.setLayoutParams(voiceParams);
        voiceButton.setPadding(
                dimen_toolbar_button_horizontal_padding,
                0,
                dimen_toolbar_button_horizontal_padding,
                0
        );
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XposedHelpers.callMethod(v.getContext(), "onClickVoiceButton", v);
            }
        });
        voiceButton.setContentDescription(voiceButton.getResources().getString(string_accessibility_voice_search_button));
        l.addView(voiceButton);

        ImageView allAppsButton = (ImageView) XposedHelpers.newInstance(mHoloImageViewClass, l.getContext());
        allAppsButton.setImageResource(drawable_ic_home_all_apps);
        allAppsButton.setAdjustViewBounds(true);
        allAppsButton.setFocusable(true);
        allAppsButton.setClickable(true);
        RelativeLayout.LayoutParams allAppsParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        allAppsParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        allAppsParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        allAppsButton.setLayoutParams(allAppsParams);
        allAppsButton.setPadding(
                dimen_toolbar_button_horizontal_padding,
                0,
                dimen_toolbar_button_horizontal_padding,
                0
        );
        allAppsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XposedHelpers.callMethod(v.getContext(), "onClickAllAppsButton", v);
            }
        });
        allAppsButton.setContentDescription(allAppsButton.getResources().getString(string_accessibility_all_apps_button));
        l.addView(allAppsButton);
    }

    public static boolean isSupported(String s){
        return s.equals("com.android.launcher");
    }

    public String getPackage() {
        return mCurrentPackage;
    }
}
