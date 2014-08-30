package org.exalm.tabletkat;

import android.content.res.XModuleResources;
import android.content.res.XResources;

public class TkR {
    private static boolean initialized;

    public static class dimen {
        public static int notification_panel_min_height;
        public static int notification_ticker_width;
        public static int system_bar_height;
        public static int system_bar_icon_drawing_size;
        public static int system_bar_icon_padding;
        public static int system_bar_navigation_key_width;
        public static int system_bar_navigation_menu_key_width;
        public static int status_bar_recents_thumbnail_width;
    }
    public static class drawable {
        public static int ic_notification_dnd;
        public static int ic_sysbar_ime;
        public static int ic_sysbar_ime_pressed;
    }
    public static class id {
        public static int airplane_checkbox;
        public static int airplane_label;
        public static int background;
        public static int bar_contents;
        public static int bar_contents_holder;
        public static int bar_shadow;
        public static int bar_shadow_holder;
        public static int battery_text;
        public static int bluetooth;
        public static int brightness;
        public static int button;
        public static int compat_mode_off_radio;
        public static int compat_mode_on_radio;
        public static int compatModeButton;
        public static int content_frame;
        public static int content_parent;
        public static int do_not_disturb_checkbox;
        public static int do_not_disturb_label;
        public static int dot0;
        public static int dot1;
        public static int dot2;
        public static int dot3;
        public static int fake_space_bar;
        public static int feedbackIconArea;
        public static int hard_keyboard_section;
        public static int hard_keyboard_switch;
        public static int icons;
        public static int ime_settings_shortcut;
        public static int imeSwitchButton;
        public static int input_method_menu_list;
        public static int item_icon;
        public static int item_radio;
        public static int item_settings_icon;
        public static int item_subtitle;
        public static int item_subtype;
        public static int item_title;
        public static int item_vertical_separator;
        public static int large_icon;
        public static int left_icon;
        public static int navigationArea;
        public static int network;
        public static int network_label;
        public static int network_text;
		public static int network_checkbox;
        public static int no_recent_apps;
        public static int notification_scroller;
        public static int notificationArea;
        public static int notificationTrigger;
        public static int right_icon;
        public static int rotate;
        public static int rotate_checkbox;
        public static int rotate_label;
        public static int rotate_separator;
        public static int settings;
        public static int settings_label;
        public static int ticker_expanded;
        public static int title_area;
        public static int use_physical_keyboard_label;
    }
    public static class layout {
        public static int compat_mode_help;
        public static int system_bar;
        public static int system_bar_compat_mode_panel;
        public static int system_bar_input_methods_item;
        public static int system_bar_input_methods_panel;
        public static int system_bar_notification_panel;
        public static int system_bar_recent_item;
        public static int system_bar_settings_view;
        public static int system_bar_ticker_compat;
        public static int system_bar_ticker_panel;
    }
    public static class string {
        public static int notifications_off_text;
        public static int notifications_off_title;
        public static int status_bar_date_formatter;
    }

    public static void init(XResources res, XModuleResources res2) {
        if (initialized){
            return;
        }
        initialized = true;

        dimen.notification_panel_min_height = add(res, res2, R.dimen.notification_panel_min_height);
        dimen.notification_ticker_width = add(res, res2, R.dimen.notification_ticker_width);
        dimen.system_bar_height = add(res, res2, R.dimen.system_bar_height);
        dimen.system_bar_icon_drawing_size = add(res, res2, R.dimen.system_bar_icon_drawing_size);
        dimen.system_bar_icon_padding = add(res, res2, R.dimen.system_bar_icon_padding);
        dimen.system_bar_navigation_key_width = add(res, res2, R.dimen.system_bar_navigation_key_width);
        dimen.system_bar_navigation_menu_key_width = add(res, res2, R.dimen.system_bar_navigation_menu_key_width);
        dimen.status_bar_recents_thumbnail_width = add(res, res2, R.dimen.status_bar_recents_thumbnail_width);

        drawable.ic_notification_dnd = add(res, res2, R.drawable.ic_notification_dnd);
        drawable.ic_sysbar_ime = add(res, res2, R.drawable.ic_sysbar_ime);
        drawable.ic_sysbar_ime_pressed = add(res, res2, R.drawable.ic_sysbar_ime_pressed);

        id.airplane_checkbox = add(res, res2, R.id.airplane_checkbox);
        id.airplane_label = add(res, res2, R.id.airplane_label);
        id.background = add(res, res2, R.id.background);
        id.bar_contents = add(res, res2, R.id.bar_contents);
        id.bar_contents_holder = add(res, res2, R.id.bar_contents_holder);
        id.bar_shadow = add(res, res2, R.id.bar_shadow);
        id.bar_shadow_holder = add(res, res2, R.id.bar_shadow_holder);
        id.battery_text = add(res, res2, R.id.battery_text);
        id.bluetooth = add(res, res2, R.id.bluetooth);
        id.brightness = add(res, res2, R.id.brightness);
        id.button = add(res, res2, R.id.button);
        id.compat_mode_off_radio = add(res, res2, R.id.compat_mode_off_radio);
        id.compat_mode_on_radio = add(res, res2, R.id.compat_mode_on_radio);
        id.compatModeButton = add(res, res2, R.id.compatModeButton);
        id.content_frame = add(res, res2, R.id.content_frame);
        id.content_parent = add(res, res2, R.id.content_parent);
        id.do_not_disturb_checkbox = add(res, res2, R.id.do_not_disturb_checkbox);
        id.do_not_disturb_label = add(res, res2, R.id.do_not_disturb_label);
        id.dot0 = add(res, res2, R.id.dot0);
        id.dot1 = add(res, res2, R.id.dot1);
        id.dot2 = add(res, res2, R.id.dot2);
        id.dot3 = add(res, res2, R.id.dot3);
        id.fake_space_bar = add(res, res2, R.id.fake_space_bar);
        id.feedbackIconArea = add(res, res2, R.id.feedbackIconArea);
        id.hard_keyboard_section = add(res, res2, R.id.hard_keyboard_section);
        id.hard_keyboard_switch = add(res, res2, R.id.hard_keyboard_switch);
        id.icons = add(res, res2, R.id.icons);
        id.ime_settings_shortcut = add(res, res2, R.id.ime_settings_shortcut);
        id.imeSwitchButton = add(res, res2, R.id.imeSwitchButton);
        id.input_method_menu_list = add(res, res2, R.id.input_method_menu_list);
        id.item_icon = add(res, res2, R.id.item_icon);
        id.item_radio = add(res, res2, R.id.item_radio);
        id.item_settings_icon = add(res, res2, R.id.item_settings_icon);
        id.item_subtitle = add(res, res2, R.id.item_subtitle);
        id.item_subtype = add(res, res2, R.id.item_subtype);
        id.item_title = add(res, res2, R.id.item_title);
        id.item_vertical_separator = add(res, res2, R.id.item_vertical_separator);
        id.large_icon = add(res, res2, R.id.large_icon);
        id.left_icon = add(res, res2, R.id.left_icon);
        id.navigationArea = add(res, res2, R.id.navigationArea);
        id.network = add(res, res2, R.id.network);
        id.network_label = add(res, res2, R.id.network_label);
        id.network_text = add(res, res2, R.id.network_text);
		id.network_checkbox = add(res, res2, R.id.network_checkbox);
        id.no_recent_apps = add(res, res2, R.id.no_recent_apps);
        id.notification_scroller = add(res, res2, R.id.notification_scroller);
        id.notificationArea = add(res, res2, R.id.notificationArea);
        id.notificationTrigger = add(res, res2, R.id.notificationTrigger);
        id.right_icon = add(res, res2, R.id.right_icon);
        id.rotate = add(res, res2, R.id.rotate);
        id.rotate_checkbox = add(res, res2, R.id.rotate_checkbox);
        id.rotate_label = add(res, res2, R.id.rotate_label);
        id.rotate_separator = add(res, res2, R.id.rotate_separator);
        id.settings = add(res, res2, R.id.settings);
        id.settings_label = add(res, res2, R.id.settings_label);
        id.ticker_expanded = add(res, res2, R.id.ticker_expanded);
        id.title_area = add(res, res2, R.id.title_area);
        id.use_physical_keyboard_label = add(res, res2, R.id.use_physical_keyboard_label);

        layout.compat_mode_help = add(res, res2, R.layout.compat_mode_help);
        layout.system_bar = add(res, res2, R.layout.system_bar);
        layout.system_bar_compat_mode_panel = add(res, res2, R.layout.system_bar_compat_mode_panel);
        layout.system_bar_input_methods_item = add(res, res2, R.layout.system_bar_input_methods_item);
        layout.system_bar_input_methods_panel = add(res, res2, R.layout.system_bar_input_methods_panel);
        layout.system_bar_notification_panel = add(res, res2, R.layout.system_bar_notification_panel);
        layout.system_bar_recent_item = add(res, res2, R.layout.system_bar_recent_item);
        layout.system_bar_settings_view = add(res, res2, R.layout.system_bar_settings_view);
        layout.system_bar_ticker_compat = add(res, res2, R.layout.system_bar_ticker_compat);
        layout.system_bar_ticker_panel = add(res, res2, R.layout.system_bar_ticker_panel);

        string.notifications_off_text = add(res, res2, R.string.notifications_off_text);
        string.notifications_off_title = add(res, res2, R.string.notifications_off_title);
        string.status_bar_date_formatter = add(res, res2, R.string.status_bar_date_formatter);
    }

    private static int add(XResources res, XModuleResources res2, int id){
        return res.addResource(res2, res2.fwd(id).getId());
    }
}
