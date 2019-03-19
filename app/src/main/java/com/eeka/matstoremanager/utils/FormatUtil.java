package com.eeka.matstoremanager.utils;

import android.text.TextUtils;

/**
 * 数据转换类，做转换异常处理
 * Created by Lenovo on 2017/8/31.
 */

public class FormatUtil {

    /**
     * 字符串转float，以小数点开始或结尾时补0计算
     */
    public static float strToFloat(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        if (str.startsWith(".")) {
            str = "0" + str;
        }
        if (str.endsWith(".")) {
            str = str + "0";
        }
        try {
            return Float.valueOf(str);
        } catch (NumberFormatException e) {
            Logger.w(e.toString());
        }
        return 0;
    }

}
