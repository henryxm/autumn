package cn.org.autumn.utils;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Phone {

    public static boolean isPhone(String phone) {
        try {
            if (StringUtils.isBlank(phone))
                return false;
            String regex = "^((13[0-9])|(14[0-9])|(15([0-9]))|(16[0-9])|(17[0-9])|(18[0-9])|(19[0-9]))\\d{8}$";
            if (phone.length() != 11) {
                return false;
            } else {
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(phone);
                return m.matches();
            }
        } catch (Exception e) {
            return false;
        }
    }
}