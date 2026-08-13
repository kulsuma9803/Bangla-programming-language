package utils;

public class BanglaUtil {
    
    private static final char[] BANGLA_DIGITS = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};

    /**
     * Converts an integer to a string using Bangla numerals.
     */
    public static String toBanglaNum(long value) {
        return toBanglaNum(String.valueOf(value));
    }

    /**
     * Replaces all ASCII digits in a string with Bangla numerals.
     */
    public static String toBanglaNum(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(BANGLA_DIGITS[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
