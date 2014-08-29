package org.exalm.tabletkat;

import android.content.res.XResources;

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
        public static int notification_panel_bg;
        //public static int notify_item_glow_bottom;
        public static int recents_thumbnail_bg;
        public static int recents_thumbnail_fg;
        public static int stat_sys_data_bluetooth;
        public static int stat_sys_data_bluetooth_connected;
        public static int status_bar_item_background;
        public static int system_bar_background;
        public static int system_bar_notification_header_bg;
        public static int system_bar_ticker_background;
    }
    public static class id {
        public static int app_thumbnail;
        public static int back;
        public static int battery;
        public static int brightness_icon;
        public static int clear_all_button;
        public static int clock;
        public static int content;
        public static int date;
        public static int home;
        public static int menu;
        public static int mobile_signal;
        public static int mobile_type;
        public static int notification_button;
        public static int notificationIcons;
        public static int recent_apps;
        public static int settings_button;
        public static int signal_cluster;
        public static int signal_cluster_view;
        public static int statusIcons;
        public static int text;
        public static int wifi_signal;
    }
    public static class integer {
        public static int config_maxNotificationIcons;
        public static int config_show_search_delay;
    }
    public static class layout {
        public static int signal_cluster_view;
        public static int status_bar_recent_item;
    }
    public static class string {
        public static int accessibility_back;
        public static int accessibility_bluetooth_connected;
        public static int accessibility_bluetooth_disconnected;
        public static int accessibility_clear_all;
        public static int accessibility_compatibility_zoom_button;
        public static int accessibility_desc_quick_settings;
        public static int accessibility_home;
        public static int accessibility_ime_switch_button;
        public static int accessibility_menu;
        public static int accessibility_notifications_button;
        public static int accessibility_recent;
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

    public static void init(XResources res){
        if (initialized){
            return;
        }
        initialized = true;

        color.status_bar_clock_color = res.getIdentifier("status_bar_clock_color", "color", SYSTEMUI_PACKAGE);
        color.system_bar_background_opaque = res.getIdentifier("system_bar_background_opaque", "color", SYSTEMUI_PACKAGE);
        color.system_bar_background_semi_transparent = res.getIdentifier("system_bar_background_semi_transparent", "color", SYSTEMUI_PACKAGE);

        dimen.notification_panel_header_padding_top = res.getIdentifier("notification_panel_header_padding_top", "dimen", SYSTEMUI_PACKAGE);
        dimen.notification_panel_width = res.getIdentifier("notification_panel_width", "dimen", SYSTEMUI_PACKAGE);
        dimen.notification_row_max_height = res.getIdentifier("notification_row_max_height", "dimen", SYSTEMUI_PACKAGE);
        dimen.notification_row_min_height = res.getIdentifier("notification_row_min_height", "dimen", SYSTEMUI_PACKAGE);
        dimen.status_bar_icon_size = res.getIdentifier("status_bar_icon_size", "dimen", SYSTEMUI_PACKAGE);

        drawable.ic_notifications = res.getIdentifier("ic_notifications", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_notify_clear = res.getIdentifier("ic_notify_clear", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_notify_settings = res.getIdentifier("ic_notify_settings", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_sysbar_back = res.getIdentifier("ic_sysbar_back", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_sysbar_back_ime = res.getIdentifier("ic_sysbar_back_ime", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_sysbar_highlight = res.getIdentifier("ic_sysbar_highlight", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_sysbar_home = res.getIdentifier("ic_sysbar_home", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_sysbar_menu = res.getIdentifier("ic_sysbar_menu", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_sysbar_lights_out_dot_large = res.getIdentifier("ic_sysbar_lights_out_dot_large", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_sysbar_lights_out_dot_small = res.getIdentifier("ic_sysbar_lights_out_dot_small", "drawable", SYSTEMUI_PACKAGE);
        drawable.ic_sysbar_recent = res.getIdentifier("ic_sysbar_recent", "drawable", SYSTEMUI_PACKAGE);
        drawable.notification_panel_bg = res.getIdentifier("notification_panel_bg", "drawable", SYSTEMUI_PACKAGE);
        drawable.nav_background = res.getIdentifier("nav_background", "drawable", SYSTEMUI_PACKAGE);
//        drawable.notify_item_glow_bottom = res.getIdentifier("notify_item_glow_bottom", "drawable", SYSTEMUI_PACKAGE);
        drawable.recents_thumbnail_bg = res.getIdentifier("recents_thumbnail_bg", "drawable", SYSTEMUI_PACKAGE);
        drawable.recents_thumbnail_fg = res.getIdentifier("recents_thumbnail_fg", "drawable", SYSTEMUI_PACKAGE);
        drawable.stat_sys_data_bluetooth = res.getIdentifier("stat_sys_data_bluetooth", "drawable", SYSTEMUI_PACKAGE);
        drawable.stat_sys_data_bluetooth_connected = res.getIdentifier("stat_sys_data_bluetooth_connected", "drawable", SYSTEMUI_PACKAGE);
        drawable.status_bar_item_background = res.getIdentifier("status_bar_item_background", "drawable", SYSTEMUI_PACKAGE);
        drawable.system_bar_background = res.getIdentifier("system_bar_background", "drawable", SYSTEMUI_PACKAGE);
        drawable.system_bar_notification_header_bg = res.getIdentifier("system_bar_notification_header_bg", "drawable", SYSTEMUI_PACKAGE);
        drawable.system_bar_ticker_background = res.getIdentifier("system_bar_ticker_background", "drawable", SYSTEMUI_PACKAGE);

        id.app_thumbnail = res.getIdentifier("app_thumbnail", "id", SYSTEMUI_PACKAGE);
        id.back = res.getIdentifier("back", "id", SYSTEMUI_PACKAGE);
        id.battery = res.getIdentifier("battery", "id", SYSTEMUI_PACKAGE);
        id.brightness_icon = res.getIdentifier("brightness_icon", "id", SYSTEMUI_PACKAGE);
        id.clear_all_button = res.getIdentifier("clear_all_button", "id", SYSTEMUI_PACKAGE);
        id.clock = res.getIdentifier("clock", "id", SYSTEMUI_PACKAGE);
        id.content = res.getIdentifier("content", "id", SYSTEMUI_PACKAGE);
        id.date =res.getIdentifier("date", "id", SYSTEMUI_PACKAGE);
        id.home = res.getIdentifier("home", "id", SYSTEMUI_PACKAGE);
        id.menu = res.getIdentifier("menu", "id", SYSTEMUI_PACKAGE);
        id.mobile_signal = res.getIdentifier("mobile_signal", "id", SYSTEMUI_PACKAGE);
        id.mobile_type = res.getIdentifier("mobile_type", "id", SYSTEMUI_PACKAGE);
        id.notification_button = res.getIdentifier("notification_button", "id", SYSTEMUI_PACKAGE);
        id.notificationIcons = res.getIdentifier("notificationIcons", "id", SYSTEMUI_PACKAGE);
        id.recent_apps = res.getIdentifier("recent_apps", "id", SYSTEMUI_PACKAGE);
        id.settings_button = res.getIdentifier("settings_button", "id", SYSTEMUI_PACKAGE);
        id.signal_cluster = res.getIdentifier("signal_cluster", "id", SYSTEMUI_PACKAGE);
        id.signal_cluster_view = res.getIdentifier("signal_cluster_view", "id", SYSTEMUI_PACKAGE);
        id.statusIcons = res.getIdentifier("statusIcons", "id", SYSTEMUI_PACKAGE);
        id.text = res.getIdentifier("text", "id", SYSTEMUI_PACKAGE);
        id.wifi_signal = res.getIdentifier("wifi_signal", "id", SYSTEMUI_PACKAGE);

        integer.config_maxNotificationIcons = res.getIdentifier("config_maxNotificationIcons", "integer", SYSTEMUI_PACKAGE);
        integer.config_show_search_delay = res.getIdentifier("config_show_search_delay", "integer", SYSTEMUI_PACKAGE);

        //MediaTek dual-SIM support
        int mtk = res.getIdentifier("gemini_signal_cluster_view", "layout", SYSTEMUI_PACKAGE);
        if (mtk > 0){
            layout.signal_cluster_view = mtk;
        }else {
            layout.signal_cluster_view = res.getIdentifier("signal_cluster_view", "layout", SYSTEMUI_PACKAGE);
        }
        layout.status_bar_recent_item = res.getIdentifier("status_bar_recent_item", "layout", SYSTEMUI_PACKAGE);

        string.accessibility_back = res.getIdentifier("accessibility_back", "string", SYSTEMUI_PACKAGE);
        string.accessibility_bluetooth_connected = res.getIdentifier("accessibility_bluetooth_connected", "string", SYSTEMUI_PACKAGE);
        string.accessibility_bluetooth_disconnected = res.getIdentifier("accessibility_bluetooth_disconnected", "string", SYSTEMUI_PACKAGE);
        string.accessibility_clear_all = res.getIdentifier("accessibility_clear_all", "string", SYSTEMUI_PACKAGE);
        string.accessibility_compatibility_zoom_button = res.getIdentifier("accessibility_compatibility_zoom_button", "string", SYSTEMUI_PACKAGE);
        string.accessibility_desc_quick_settings = res.getIdentifier("accessibility_desc_quick_settings", "string", SYSTEMUI_PACKAGE);
        string.accessibility_home = res.getIdentifier("accessibility_home", "string", SYSTEMUI_PACKAGE);
        string.accessibility_ime_switch_button = res.getIdentifier("accessibility_ime_switch_button", "string", SYSTEMUI_PACKAGE);
        string.accessibility_menu = res.getIdentifier("accessibility_menu", "string", SYSTEMUI_PACKAGE);
        string.accessibility_notifications_button = res.getIdentifier("accessibility_notifications_button", "string", SYSTEMUI_PACKAGE);
        string.accessibility_recent = res.getIdentifier("accessibility_recent", "string", SYSTEMUI_PACKAGE);
        string.accessibility_settings_button = res.getIdentifier("accessibility_settings_button", "string", SYSTEMUI_PACKAGE);
        string.config_statusBarComponent = res.getIdentifier("config_statusBarComponent", "string", SYSTEMUI_PACKAGE);
        string.status_bar_input_method_settings_configure_input_methods = res.getIdentifier("status_bar_input_method_settings_configure_input_methods", "string", SYSTEMUI_PACKAGE);
        string.status_bar_no_recent_apps = res.getIdentifier("status_bar_no_recent_apps", "string", SYSTEMUI_PACKAGE);
        string.status_bar_settings_airplane = res.getIdentifier("status_bar_settings_airplane", "string", SYSTEMUI_PACKAGE);
        string.status_bar_settings_auto_brightness_label = res.getIdentifier("status_bar_settings_auto_brightness_label", "string", SYSTEMUI_PACKAGE);
        string.status_bar_settings_auto_rotation = res.getIdentifier("status_bar_settings_auto_rotation", "string", SYSTEMUI_PACKAGE);
        string.status_bar_settings_notifications = res.getIdentifier("status_bar_settings_notifications", "string", SYSTEMUI_PACKAGE);
        string.status_bar_settings_settings_button = res.getIdentifier("status_bar_settings_settings_button", "string", SYSTEMUI_PACKAGE);
        string.status_bar_settings_wifi_button = res.getIdentifier("status_bar_settings_wifi_button", "string", SYSTEMUI_PACKAGE);
        string.status_bar_use_physical_keyboard = res.getIdentifier("status_bar_use_physical_keyboard", "string", SYSTEMUI_PACKAGE);
    }
}
