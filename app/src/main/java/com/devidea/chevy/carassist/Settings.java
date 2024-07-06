package com.devidea.chevy.carassist;

import android.content.SharedPreferences;

/* loaded from: classes.dex */
public class Settings {
    public static int AUTO_LAUNCH_AMAP = 1;
    public static int AUTO_LAUNCH_INNER = 2;
    public static int AUTO_LAUNCH_NONE;
    public boolean bAutoOpenBT = true;
    public boolean bAutoOpenGps = true;
    public boolean isKeepScreenOn = true;
    public int dayNightMode = 0;
    public boolean isTrafficVoiceOn = true;
    public boolean isNaviVoiceOn = true;
    public boolean is3DNaviOn = false;
    public boolean voicePauseMusic = false;
    public int speedAjust = 0;
    public int mCongestion = 1;
    public int mAvoidHighSpeed = 0;
    public int mCost = 0;
    public int mHighSpeed = 1;
    public int mTempVoiceOn = 1;
    public int mVoltageVoiceOn = 0;
    public int mTpmsVoiceOn = 1;
    public int mHandbrakeVoiceOn = 1;
    public int mSeatbeltVoiceOn = 1;
    public int mDoorVoiceOn = 1;
    public int mSpeakerIndex = 0;
    public String mCarType = "0";
    public String mCarNumber = "";
    public boolean mRestriction = true;
    public boolean mVoiceWakeup = true;
    public int mAutoLaunchItem = AUTO_LAUNCH_NONE;
    public boolean mDspLimitSpeed = true;
    public boolean mDspAppNotification = false;

    /*public void initUI() {
        ViewModel.getIns().setViewProperty(R.id.limit_speed_on, this.mDspLimitSpeed ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.wake_up_on, this.mVoiceWakeup ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.bt_auto_open, this.bAutoOpenBT ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.gps_auto_open, this.bAutoOpenGps ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.keep_screen_on, this.isKeepScreenOn ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.navi_set_navi_voice, this.isNaviVoiceOn ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.navi_set_traffic_voice, this.isTrafficVoiceOn ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.navi_set_3d_navi, this.is3DNaviOn ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.voice_lower_music, !this.voicePauseMusic ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.voice_pause_music, this.voicePauseMusic ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.navi_strategy_avoid_highspeed, this.mAvoidHighSpeed);
        ViewModel.getIns().setViewProperty(R.id.navi_strategy_highspeed, this.mHighSpeed);
        ViewModel.getIns().setViewProperty(R.id.navi_strategy_cost, this.mCost);
        ViewModel.getIns().setViewProperty(R.id.navi_strategy_congestion, this.mCongestion);
        ViewModel.getIns().setViewProperty(R.id.temperature_voice_on, this.mTempVoiceOn);
        ViewModel.getIns().setViewProperty(R.id.voltage_voice_on, this.mVoltageVoiceOn);
        ViewModel.getIns().setViewProperty(R.id.tpms_voice_on, this.mTpmsVoiceOn);
        ViewModel.getIns().setViewProperty(R.id.handbrake_voice_on, this.mHandbrakeVoiceOn);
        ViewModel.getIns().setViewProperty(R.id.seatbelt_voice_on, this.mSeatbeltVoiceOn);
        ViewModel.getIns().setViewProperty(R.id.doors_voice_on, this.mDoorVoiceOn);
        ViewModel.getIns().setViewProperty(R.id.auto_launch_amap, this.mAutoLaunchItem == AUTO_LAUNCH_AMAP ? 1 : 0);
        ViewModel.getIns().setViewProperty(R.id.auto_launch_inner_map, this.mAutoLaunchItem == AUTO_LAUNCH_INNER ? 1 : 0);
        int[] iArr = {R.id.speaker_amap_default, R.id.speaker_normal_woman, R.id.speaker_normal_man, R.id.speaker_specific_woman, R.id.speaker_specific_man, R.id.speaker_child};
        int i = 0;
        while (i < iArr.length) {
            ViewModel.getIns().setViewProperty(iArr[i], i == this.mSpeakerIndex ? 1 : 0);
            i++;
        }
        String[] strArr = {"自动模式", "白天模式", "夜间模式"};
        if (this.dayNightMode < 3) {
            ViewModel.getIns().setViewProperty(R.id.navi_set_day_night_mode_value, strArr[this.dayNightMode]);
        }
    }*/

    public void storeSettings(SharedPreferences.Editor editor) {
        editor.putBoolean("mDspLimitSpeed", this.mDspLimitSpeed);
        editor.putBoolean("bAutoOpenBT", this.bAutoOpenBT);
        editor.putBoolean("bAutoOpenGps", this.bAutoOpenGps);
        editor.putBoolean("bKeepScreenOn", this.isKeepScreenOn);
        editor.putBoolean("isTrafficVoiceOn", this.isTrafficVoiceOn);
        editor.putBoolean("isNaviVoiceOn", this.isNaviVoiceOn);
        editor.putBoolean("is3DNaviOn", this.is3DNaviOn);
        editor.putBoolean("voicePauseMusic", this.voicePauseMusic);
        editor.putBoolean("mRestriction", this.mRestriction);
        editor.putBoolean("mVoiceWakeup", this.mVoiceWakeup);
        editor.putBoolean("mDspAppNotification", this.mDspAppNotification);
        editor.putInt("dayNightMode", this.dayNightMode);
        editor.putInt("mCongestion", this.mCongestion);
        editor.putInt("mAvoidHighSpeed", this.mAvoidHighSpeed);
        editor.putInt("mCost", this.mCost);
        editor.putInt("mHighSpeed", this.mHighSpeed);
        editor.putInt("mTempVoiceOn", this.mTempVoiceOn);
        editor.putInt("mVoltageVoiceOn", this.mVoltageVoiceOn);
        editor.putInt("mTpmsVoiceOn", this.mTpmsVoiceOn);
        editor.putInt("mHandbrakeVoiceOn", this.mHandbrakeVoiceOn);
        editor.putInt("mSeatbeltVoiceOn", this.mSeatbeltVoiceOn);
        editor.putInt("mDoorVoiceOn", this.mDoorVoiceOn);
        editor.putInt("mSpeakerIndex", this.mSpeakerIndex);
        editor.putString("mCarType", this.mCarType);
        editor.putString("mCarNumber", this.mCarNumber);
        editor.putInt("mAutoLaunchItem", this.mAutoLaunchItem);
        editor.commit();
    }

    public void restoreSettings(SharedPreferences sharedPreferences) {
        this.mDspLimitSpeed = sharedPreferences.getBoolean("mDspLimitSpeed", true);
        this.bAutoOpenBT = sharedPreferences.getBoolean("bAutoOpenBT", true);
        this.bAutoOpenGps = sharedPreferences.getBoolean("bAutoOpenGps", true);
        this.isKeepScreenOn = sharedPreferences.getBoolean("bKeepScreenOn", true);
        this.isTrafficVoiceOn = sharedPreferences.getBoolean("isTrafficVoiceOn", true);
        this.isNaviVoiceOn = sharedPreferences.getBoolean("isNaviVoiceOn", true);
        this.is3DNaviOn = sharedPreferences.getBoolean("is3DNaviOn", false);
        this.voicePauseMusic = sharedPreferences.getBoolean("voicePauseMusic", false);
        this.mRestriction = sharedPreferences.getBoolean("mRestriction", true);
        this.mVoiceWakeup = sharedPreferences.getBoolean("mVoiceWakeup", true);
        this.mDspAppNotification = sharedPreferences.getBoolean("mDspAppNotification", false);
        this.dayNightMode = sharedPreferences.getInt("dayNightMode", 0);
        this.mCongestion = sharedPreferences.getInt("mCongestion", 1);
        this.mAvoidHighSpeed = sharedPreferences.getInt("mAvoidHighSpeed", 0);
        this.mHighSpeed = sharedPreferences.getInt("mHighSpeed", 1);
        this.mCost = sharedPreferences.getInt("mCost", 0);
        this.mTempVoiceOn = sharedPreferences.getInt("mTempVoiceOn", 1);
        this.mVoltageVoiceOn = sharedPreferences.getInt("mVoltageVoiceOn", 0);
        this.mTpmsVoiceOn = sharedPreferences.getInt("mTpmsVoiceOn", 1);
        this.mHandbrakeVoiceOn = sharedPreferences.getInt("mHandbrakeVoiceOn", 1);
        this.mSeatbeltVoiceOn = sharedPreferences.getInt("mSeatbeltVoiceOn", 1);
        this.mDoorVoiceOn = sharedPreferences.getInt("mDoorVoiceOn", 1);
        this.mSpeakerIndex = sharedPreferences.getInt("mSpeakerIndex", 0);
        this.mCarType = sharedPreferences.getString("mCarType", "0");
        this.mCarNumber = sharedPreferences.getString("mCarNumber", "");
        this.mAutoLaunchItem = sharedPreferences.getInt("mAutoLaunchItem", AUTO_LAUNCH_NONE);
    }
}
