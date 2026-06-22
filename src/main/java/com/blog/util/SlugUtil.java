package com.blog.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.text.Normalizer;
import java.util.Locale;

/**
 * URL Slug 生成工具类
 *
 * <p>将中文标题转换为拼音格式的 URL 友好字符串（slug），用于文章、分类、标签的 URL 路径。</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>自动检测中文并转换为拼音</li>
 *   <li>移除特殊字符，保留字母、数字和连字符</li>
 *   <li>自动截断过长的 slug（最大 100 字符）</li>
 *   <li>空输入时生成基于时间戳的默认 slug</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * String slug = SlugUtil.generateSlug("Spring Boot 3 实战");
 * // 结果: "spring-boot-3-shi-zhan"
 *
 * String slug2 = SlugUtil.generateSlug("Hello World");
 * // 结果: "hello-world"
 * }</pre>
 *
 * @author Diamond
 * @since 1.0.0
 */
public final class SlugUtil {

    private SlugUtil() {
        // 工具类不允许实例化
    }

    /**
     * 生成 URL 友好的 slug 字符串
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>检测输入是否包含中文，如果是则转换为拼音</li>
     *   <li>进行 Unicode 标准化处理</li>
     *   <li>移除特殊字符，保留字母、数字、空格和连字符</li>
     *   <li>将空格和下划线转换为连字符</li>
     *   <li>合并连续的连字符</li>
     *   <li>截断超过 100 字符的部分</li>
     * </ol>
     *
     * @param input 原始字符串，可以是中文、英文或混合内容
     * @return URL 友好的 slug 字符串，如果输入为空则返回基于时间戳的默认值
     */
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

    /**
     * 检测字符串是否包含中文字符
     *
     * @param str 待检测的字符串
     * @return 如果包含中文字符返回 true，否则返回 false
     */
    private static boolean containsChinese(String str) {
        return str.matches(".*[\\u4e00-\\u9fa5].*");
    }

    /**
     * 将中文字符串转换为拼音
     *
     * <p>转换规则：</p>
     * <ul>
     *   <li>中文字符转换为小写拼音，多个音节取第一个</li>
     *   <li>空格、连字符、下划线转换为连字符</li>
     *   <li>其他字母和数字保持不变</li>
     *   <li>忽略无法转换的字符</li>
     * </ul>
     *
     * @param chinese 包含中文的字符串
     * @return 转换后的拼音字符串，音节之间用连字符分隔
     */
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
