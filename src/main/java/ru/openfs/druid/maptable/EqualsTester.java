package ru.openfs.druid.maptable;


public final class EqualsTester extends ColumnTester {
    final private String eqValue;

    EqualsTester(String value) {
        eqValue = value;
    }

    public boolean test(String data) {
        return (data != null) && (data.equals(eqValue));
    }

    public String toString() {
        return EQUALS_TESTER + eqValue;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof EqualsTester)) {
            return false;
        }
        return ((EqualsTester)object).eqValue.equals(this.eqValue);
    }
}
