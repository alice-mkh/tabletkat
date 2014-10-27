package org.exalm.tabletkat;

import android.content.res.XModuleResources;
import android.content.res.XResources;

import de.robv.android.xposed.XposedBridge;

public class SystemR {
    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    private static boolean initialized;

    public static class color {
        public static int status_bar_clock_color;
        public static int system_bar_background_opaque;
        public static int system_bar_background_semi_transparent;
    }

    public static class dimen {
        public static int notification_panel_header_padding_top;
        public static int notification_panel_width;
        public static int notification_row_max_height;
        public static int notification_row_min_height;
        public static int peek_window_y_offset;
        public static int status_bar_icon_drawing_alpha;
        public static int status_bar_icon_size;
    }

    public static class drawable {
        public static int ic_notifications;
        public static int ic_notify_clear;
        public static int ic_notify_settings;
        public static int ic_sysbar_back;
        public static int ic_sysbar_back_ime;
        public static int ic_sysbar_highlight;
        public static int ic_sysbar_home;
        public static int ic_sysbar_lights_out_dot_large;
        public static int ic_sysbar_lights_out_dot_small;
        public static int ic_sysbar_menu;
        public static int ic_sysbar_recent;
        public static int nav_background;
        public static int recents_thumbnail_bg;
        public static int recents_thumbnail_fg;
        public static int search_light;
        public static int stat_sys_data_bluetooth;
        public static int stat_sys_data_bluetooth_connected;
        public static int status_bar_item_background;
        public static int system_bar_background;
        public static int system_bar_notification_header_bg;
        public static int system_bar_ticker_background;
    }
    public static class id {
        public static int app_thumbnail;
        public static int battery;
        public static int brightness_icon;
        public static int clear_all_button;
        public static int clock;
        public static int content;
        public static int date;
        public static int mobile_signal;
        public static int mobile_type;
        public static int nav_buttons;
        public static int notification_button;
        public static int notificationIcons;
        public static int settings_button;
        public static int signal_cluster;
        public static int statusIcons;
        public static int text;
        public static int wifi_signal;
        public static int recents_root;
    }
    public static class integer {
        public static int config_maxNotificationIcons;
        public static int config_show_search_delay;
        public static int heads_up_notification_decay;
    }
    public static class layout {
        public static int heads_up;
        public static int signal_cluster_view;
        public static int status_bar_recent_item;
        public static int status_bar_recent_panel;
        public static int status_bar_search_panel;
    }
    public static class string {
        public static int accessibility_back;
        public static int accessibility_battery_level;
        public static int accessibility_bluetooth_connected;
        public static int accessibility_bluetooth_disconnected;
        public static int accessibility_clear_all;
        public static int accessibility_desc_quick_settings;
        public static int accessibility_home;
        public static int accessibility_ime_switch_button;
        public static int accessibility_menu;
        public static int accessibility_notifications_button;
        public static int accessibility_recent;
        public static int accessibility_search_light;
        public static int accessibility_settings_button;
        public static int config_statusBarComponent;
        public static int status_bar_input_method_settings_configure_input_methods;
        public static int status_bar_no_recent_apps;
        public static int status_bar_settings_airplane;
        public static int status_bar_settings_auto_brightness_label;
        public static int status_bar_settings_auto_rotation;
        public static int status_bar_settings_notifications;
        public static int status_bar_settings_settings_button;
        public static int status_bar_settings_wifi_button;
        public static int status_bar_use_physical_keyboard;
    }

    private static int get(XResources res, XModuleResources res2, String name, String type) {
        int id = res.getIdentifier(name, type, SYSTEMUI_PACKAGE);
        if (id <= 0) {
            try {
                int i = res2.getIdentifier(name, type, "org.exalm.tabletkat");
                id = res.addResource(res2, res2.fwd(i).getId());
            }catch (Throwable t){
                return id;
            }
            XposedBridge.log("Cannot find " + SYSTEMUI_PACKAGE + "/" + type + ":" + name);
        }
        return id;
    }

    public static void init(XResources res, XModuleResources res2){
        if (initialized){
            return;
        }
        initialized = true;

        color.status_bar_clock_color = get(res, res2, "status_bar_clock_color", "color");
        color.system_bar_background_opaque = get(res, res2, "system_bar_background_opaque", "color");
        color.system_bar_background_semi_transparent = get(res, res2, "system_bar_background_semi_transparent", "color");

        dimen.notification_panel_header_padding_top = get(res, res2, "notification_panel_header_padding_top", "dimen");
        dimen.notification_panel_width = get(res, res2, "notification_panel_width", "dimen");
        dimen.notification_row_max_height = get(res, res2, "notification_row_max_height", "dimen");
        dimen.notification_row_min_height = get(res, res2, "notification_row_min_height", "dimen");
        dimen.peek_window_y_offset = get(res, res2, "peek_window_y_offset", "dimen");
        dimen.status_bar_icon_drawing_alpha = get(res, res2, "status_bar_icon_drawing_alpha", "dimen");
        dimen.status_bar_icon_size = get(res, res2, "status_bar_icon_size", "dimen");

        drawable.ic_notifications = get(res, res2, "ic_notifications", "drawable");
        drawable.ic_notify_clear = get(res, res2, "ic_notify_clear", "drawable");
        drawable.ic_notify_settings = get(res, res2, "ic_notify_settings", "drawable");
        drawable.ic_sysbar_back = get(res, res2, "ic_sysbar_back", "drawable");
        drawable.ic_sysbar_back_ime = get(res, res2, "ic_sysbar_back_ime", "drawable");
        drawable.ic_sysbar_highlight = get(res, res2, "ic_sysbar_highlight", "drawable");
        drawable.ic_sysbar_home = get(res, res2, "ic_sysbar_home", "drawable");
        drawable.ic_sysbar_menu = get(res, res2, "ic_sysbar_menu", "drawable");
        drawable.ic_sysbar_lights_out_dot_large = get(res, res2, "ic_sysbar_lights_out_dot_large", "drawable");
        drawable.ic_sysbar_lights_out_dot_small = get(res, res2, "ic_sysbar_lights_out_dot_small", "drawable");
        drawable.ic_sysbar_recent = get(res, res2, "ic_sysbar_recent", "drawable");
        drawable.nav_background = get(res, res2, "nav_background", "drawable");
        drawable.recents_thumbnail_bg = get(res, res2, "recents_thumbnail_bg", "drawable");
        drawable.recents_thumbnail_fg = get(res, res2, "recents_thumbnail_fg", "drawable");
        drawable.search_light = get(res, res2, "search_light", "drawable");
        drawable.stat_sys_data_bluetooth = get(res, res2, "stat_sys_data_bluetooth", "drawable");
        drawable.stat_sys_data_bluetooth_connected = get(res, res2, "stat_sys_data_bluetooth_connected", "drawable");
        drawable.status_bar_item_background = get(res, res2, "status_bar_item_background", "drawable");
        drawable.system_bar_background = get(res, res2, "system_bar_background", "drawable");
        drawable.system_bar_notification_header_bg = get(res, res2, "system_bar_notification_header_bg", "drawable");
        drawable.system_bar_ticker_background = get(res, res2, "system_bar_ticker_background", "drawable");

        id.app_thumbnail = get(res, res2, "app_thumbnail", "id");
        id.battery = get(res, res2, "battery", "id");
        id.brightness_icon = get(res, res2, "brightness_icon", "id");
        id.clear_all_button = get(res, res2, "clear_all_button", "id");
        id.clock = get(res, res2, "clock", "id");
        id.content = get(res, res2, "content", "id");
        id.date =get(res, res2, "date", "id");
        id.mobile_signal = get(res, res2, "mobile_signal", "id");
        id.mobile_type = get(res, res2, "mobile_type", "id");
        id.nav_buttons = get(res, res2, "nav_buttons", "id");
        id.notification_button = get(res, res2, "notification_button", "id");
        id.notificationIcons = get(res, res2, "notificationIcons", "id");
        id.settings_button = get(res, res2, "settings_button", "id");
        id.signal_cluster = get(res, res2, "signal_cluster", "id");
        id.statusIcons = get(res, res2, "statusIcons", "id");
        id.text = get(res, res2, "text", "id");
        id.wifi_signal = get(res, res2, "wifi_signal", "id");
        id.recents_root = get(res, res2, "recents_root", "id");

        integer.config_maxNotificationIcons = get(res, res2, "config_maxNotificationIcons", "integer");
        integer.config_show_search_delay = get(res, res2, "config_show_search_delay", "integer");
        integer.heads_up_notification_decay = get(res, res2, "heads_up_notification_decay", "integer");

        layout.heads_up = get(res, res2, "heads_up", "layout");
        //MediaTek dual-SIM support
        int mtk = res.getIdentifier("gemini_signal_cluster_view", "layout", SYSTEMUI_PACKAGE);
        if (mtk > 0){
            layout.signal_cluster_view = mtk;
        }else {
            layout.signal_cluster_view = get(res, res2, "signal_cluster_view", "layout");
        }
        layout.status_bar_recent_item = get(res, res2, "status_bar_recent_item", "layout");
        layout.status_bar_recent_panel = get(res, res2, "status_bar_recent_panel", "layout");
        layout.status_bar_search_panel = get(res, res2, "status_bar_search_panel", "layout");

        string.accessibility_back = get(res, res2, "accessibility_back", "string");
        string.accessibility_battery_level = get(res, res2, "accessibility_battery_level", "string");
        string.accessibility_bluetooth_connected = get(res, res2, "accessibility_bluetooth_connected", "string");
        string.accessibility_bluetooth_disconnected = get(res, res2, "accessibility_bluetooth_disconnected", "string");
        string.accessibility_clear_all = get(res, res2, "accessibility_clear_all", "string");
        string.accessibility_desc_quick_settings = get(res, res2, "accessibility_desc_quick_settings", "string");
        string.accessibility_home = get(res, res2, "accessibility_home", "string");
        string.accessibility_ime_switch_button = get(res, res2, "accessibility_ime_switch_button", "string");
        string.accessibility_menu = get(res, res2, "accessibility_menu", "string");
        string.accessibility_notifications_button = get(res, res2, "accessibility_notifications_button", "string");
        string.accessibility_recent = get(res, res2, "accessibility_recent", "string");
        string.accessibility_search_light = get(res, res2, "accessibility_search_light", "string");
        string.accessibility_settings_button = get(res, res2, "accessibility_settings_button", "string");
        string.config_statusBarComponent = get(res, res2, "config_statusBarComponent", "string");
        string.status_bar_input_method_settings_configure_input_methods = get(res, res2, "status_bar_input_method_settings_configure_input_methods", "string");
        string.status_bar_no_recent_apps = get(res, res2, "status_bar_no_recent_apps", "string");
        string.status_bar_settings_airplane = get(res, res2, "status_bar_settings_airplane", "string");
        string.status_bar_settings_auto_brightness_label = get(res, res2, "status_bar_settings_auto_brightness_label", "string");
        string.status_bar_settings_auto_rotation = get(res, res2, "status_bar_settings_auto_rotation", "string");
        string.status_bar_settings_notifications = get(res, res2, "status_bar_settings_notifications", "string");
        string.status_bar_settings_settings_button = get(res, res2, "status_bar_settings_settings_button", "string");
        string.status_bar_settings_wifi_button = get(res, res2, "status_bar_settings_wifi_button", "string");
        string.status_bar_use_physical_keyboard = get(res, res2, "status_bar_use_physical_keyboard", "string");
    }
}
