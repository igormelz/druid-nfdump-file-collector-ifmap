package ru.openfs.druid.maptable;


public final class TrueTester extends ColumnTester {

    TrueTester() {
    }

    public boolean test(String data) {
       return true;
    }

    public String toString() {
        return TRUE_TESTER;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof TrueTester)) {
            return false;
        }
        return true;
    }
}
