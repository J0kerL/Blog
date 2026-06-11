package com.blog.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {

    private SlugUtil() {}

    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "post-" + System.currentTimeMillis() % 1000000;
        }

        // 将中文转换为拼音
        String processed = containsChinese(input) ? toPinyin(input) : input;

        String slug = Normalizer.normalize(processed, Normalizer.Form.NFD)
                .replaceAll("[^\\w\\s-]", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[\\s_]+", "-")
                .replaceAll("-+", "-");

        // 截断过长的 slug
        if (slug.length() > 100) {
            slug = slug.substring(0, 100).replaceAll("-+$", "");
        }

        return slug.isEmpty() ? "post-" + System.currentTimeMillis() % 1000000 : slug;
    }

    private static boolean containsChinese(String str) {
        return str.matches(".*[\\u4e00-\\u9fa5].*");
    }

    private static String toPinyin(String chinese) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);

        StringBuilder pinyin = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        if (!pinyin.isEmpty()) {
                            pinyin.append("-");
                        }
                        pinyin.append(pinyinArray[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // ignore
                }
            } else if (c == ' ' || c == '-' || c == '_') {
                if (!pinyin.isEmpty() && pinyin.charAt(pinyin.length() - 1) != '-') {
                    pinyin.append("-");
                }
            } else if (Character.isLetterOrDigit(c)) {
                pinyin.append(c);
            }
        }
        return pinyin.toString();
    }
}
