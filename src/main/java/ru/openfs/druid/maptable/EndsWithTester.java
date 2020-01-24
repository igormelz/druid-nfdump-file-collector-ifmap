package ru.openfs.druid.maptable;



public final class EndsWithTester extends ColumnTester {
    final private String suffix;

    EndsWithTester(String value) {
        this.suffix = value;// .trim().toUpperCase();
    }

    public boolean test(String data) {
        return (data != null) && (data.endsWith(suffix));
    }

    public String toString() {
        return  ENDS_WITH_TESTER + suffix;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof EndsWithTester)) {
            return false;
        }
        return ((EndsWithTester)object).suffix.equals(this.suffix);
    }
}
