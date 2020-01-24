package ru.openfs.druid.maptable;



public final class StartsWithTester extends ColumnTester {
    final private String prefix;

    StartsWithTester(String value) {
        this.prefix = value;// .trim().toUpperCase();
    }

    @Override
    public boolean test(String data) {
        return (data != null) && (data.startsWith(prefix));
    }

    public String toString() {
        return STARTS_WITH_TESTER + prefix;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof StartsWithTester)) {
            return false;
        }
        return ((StartsWithTester)object).prefix.equals(this.prefix);
    }
	
}
