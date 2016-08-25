package com.twohe.mysecondapplication;

/**
 * Created by morri on 23.08.2016.
 */
public class Setting {
    private long id;
    private String setting;
    private String setting_value;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSetting() {
        return setting;
    }

    public String getSettingValue() {
        return setting_value;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    public void setSettingValue(String setting_value) {
        this.setting_value = setting_value;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return setting + ' ' + setting_value;
    }
}
