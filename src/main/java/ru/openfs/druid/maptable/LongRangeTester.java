package ru.openfs.druid.maptable;

public final class LongRangeTester extends ColumnTester {
	final private long start;
    final private long end;

    LongRangeTester(long start, long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean test(String data) {
        if (data == null) {
            return false;
        }
        long value = 0L;
        try {
            value = Long.parseLong(data);
        } catch (NumberFormatException parseException) {
            return false;
        }
        return (value >= start && value <= end);
    }

    public String toString() {
        return RANGE_TESTER + start + ":" + end;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof LongRangeTester)) {
            return false;
        }
        return (((LongRangeTester)object).start == this.start && ((LongRangeTester)object).end == this.end);
    }
}
