package com.itc.thaithang.yourperformance.apdapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.itc.thaithang.yourperformance.fragment.ReportFragment;
import com.itc.thaithang.yourperformance.fragment.SetTimeFragment;
import com.itc.thaithang.yourperformance.fragment.SettingFragment;

public class PagerAdapter extends FragmentPagerAdapter {

    private int count;
    private ReportFragment fragReport;
    private SetTimeFragment fragSetTime;
    private SettingFragment fragSetting;

    public PagerAdapter(FragmentManager fm, int count) {
        super(fm);
        this.count = count;
    }

    @Override
    public Fragment getItem(int i) {
        switch(i){
            case 0:
                return fragReport;
            case 1:
                return fragSetTime;
            case 2:
                return fragSetting;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return count;
    }

    public void initFragments(ReportFragment fragReport, SetTimeFragment fragSetTime, SettingFragment fragSetting){
        this.fragReport = fragReport;
        this.fragSetTime = fragSetTime;
        this.fragSetting = fragSetting;
    }
}
