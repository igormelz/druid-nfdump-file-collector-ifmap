package ru.openfs.druid.maptable;

import java.util.regex.Pattern;


public final class RegexTester extends ColumnTester {
    final private Pattern pattern;

    RegexTester(String value) {
        pattern = Pattern.compile(value);
    }

    public boolean test(String data) {
        return pattern.matcher(data).matches();
    }

    public String toString() {
        return REGEX_TESTER + pattern.toString();
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof RegexTester)) {
            return false;
        }
        return ((RegexTester)object).pattern.equals(this.pattern);
    }
}
