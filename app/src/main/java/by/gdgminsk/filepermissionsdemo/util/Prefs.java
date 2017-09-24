package by.gdgminsk.filepermissionsdemo.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IdRes;

import by.gdgminsk.filepermissionsdemo.R;

public class Prefs {

    private static final String PREF_STORAGE_SWITCH_STATE = "storage_switch_state";
    private static final String PREF_MIGRATION_SWITCH_STATE = "migration_switch_state";
    private static final String PREF_IMAGE_LOADED = "image_loaded";

    private SharedPreferences mPrefs;

    private static Prefs sInstance;

    private Prefs(SharedPreferences prefs) {
        mPrefs = prefs;
    }

    public static Prefs get(Context context) {
        if (sInstance == null) {
            sInstance = new Prefs(context.getSharedPreferences("app_settings", Context.MODE_PRIVATE));
        }
        return sInstance;
    }

    public void setImageLoaded(boolean imageLoaded) {
        mPrefs.edit().putBoolean(PREF_IMAGE_LOADED, imageLoaded).apply();
    }

    public boolean isImageLoaded() {
        return mPrefs.getBoolean(PREF_IMAGE_LOADED, false);
    }

    public void setMigrationMode(@IdRes int migrationMode) {
        mPrefs.edit().putInt(PREF_MIGRATION_SWITCH_STATE, migrationMode).apply();
    }

    @IdRes
    public int getMigrationMode() {
        return mPrefs.getInt(PREF_MIGRATION_SWITCH_STATE, R.id.migration_switch_java_move);
    }

    public void setStorageMode(@IdRes int storageMode) {
        mPrefs.edit().putInt(PREF_STORAGE_SWITCH_STATE, storageMode).apply();
    }

    @IdRes
    public int getStorageMode() {
        return mPrefs.getInt(PREF_STORAGE_SWITCH_STATE, R.id.storage_switch_common);
    }

    public void reset() {
        mPrefs.edit()
                .remove(PREF_IMAGE_LOADED)
                .remove(PREF_MIGRATION_SWITCH_STATE)
                .remove(PREF_STORAGE_SWITCH_STATE)
                .apply();
    }
}
