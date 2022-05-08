package com.stickercamera.app.camera;

import com.stickercamera.app.camera.effect.FilterEffect;
import com.stickercamera.app.camera.util.GPUImageFilterTools;

import java.util.ArrayList;
import java.util.List;


public class EffectService {

    private static EffectService mInstance;

    public static EffectService getInst() {
        if (mInstance == null) {
            synchronized (EffectService.class) {
                if (mInstance == null)
                    mInstance = new EffectService();
            }
        }
        return mInstance;
    }

    private EffectService() {
    }

    public List<FilterEffect> getLocalFilters() {
        List<FilterEffect> filters = new ArrayList<FilterEffect>();
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.NORMAL, 0));

        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_AIMEI, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_DANLAN, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_DANHUANG, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_FUGU, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_GAOLENG, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_HUAIJIU, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_JIAOPIAN, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_KEAI, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_LOMO, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_MORENJIAQIANG, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_NUANXIN, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_QINGXIN, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_RIXI, 0));
        filters.add(new FilterEffect("", GPUImageFilterTools.FilterType.ACV_WENNUAN, 0));

        /*filters.add(new FilterEffect("original", GPUImageFilterTools.FilterType.NORMAL, 0));

        filters.add(new FilterEffect("ambiguous", GPUImageFilterTools.FilterType.ACV_AIMEI, 0));
        filters.add(new FilterEffect("Light blue", GPUImageFilterTools.FilterType.ACV_DANLAN, 0));
        filters.add(new FilterEffect("yolk", GPUImageFilterTools.FilterType.ACV_DANHUANG, 0));
        filters.add(new FilterEffect("Retro", GPUImageFilterTools.FilterType.ACV_FUGU, 0));
        filters.add(new FilterEffect("High cold", GPUImageFilterTools.FilterType.ACV_GAOLENG, 0));
        filters.add(new FilterEffect("Nostalgia", GPUImageFilterTools.FilterType.ACV_HUAIJIU, 0));
        filters.add(new FilterEffect("film", GPUImageFilterTools.FilterType.ACV_JIAOPIAN, 0));
        filters.add(new FilterEffect("lovely", GPUImageFilterTools.FilterType.ACV_KEAI, 0));
        filters.add(new FilterEffect("lonely", GPUImageFilterTools.FilterType.ACV_LOMO, 0));
        filters.add(new FilterEffect("strengthen", GPUImageFilterTools.FilterType.ACV_MORENJIAQIANG, 0));
        filters.add(new FilterEffect("Warm heart", GPUImageFilterTools.FilterType.ACV_NUANXIN, 0));
        filters.add(new FilterEffect("Fresh", GPUImageFilterTools.FilterType.ACV_QINGXIN, 0));
        filters.add(new FilterEffect("Japanese", GPUImageFilterTools.FilterType.ACV_RIXI, 0));
        filters.add(new FilterEffect("warm", GPUImageFilterTools.FilterType.ACV_WENNUAN, 0));*/

        return filters;
    }

}
