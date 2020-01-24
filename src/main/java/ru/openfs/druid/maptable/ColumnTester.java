package ru.openfs.druid.maptable;

public abstract class ColumnTester implements Tester {

	public static String IPADDR_TESTER = "IPADDR@";
    public static String STARTS_WITH_TESTER = "STARTS@";
    public static String ENDS_WITH_TESTER = "ENDS@";
    public static String RANGE_TESTER = "RANGE@";
    public static String EQUALS_TESTER = "EQ@";
    public static String REGEX_TESTER = "REGEX@";
    public static String TRUE_TESTER = "TRUE@";
	
	@Override
	public boolean test(String dataToTest) {
		if (dataToTest != null && !dataToTest.isEmpty()) {
			return test(dataToTest);
		}
		return false;
	}

	public static Tester of(String key) {
		if (key.isEmpty()) {
			return new TrueTester();
		}
		if (key.startsWith(TRUE_TESTER)) {
			return new TrueTester();
		}
		if (key.startsWith(IPADDR_TESTER)) {
			return new IpAddrTester(key.substring(IPADDR_TESTER.length()));
		}
		if (key.startsWith(STARTS_WITH_TESTER)) {
			return new StartsWithTester(key.substring(STARTS_WITH_TESTER.length()));
		}
		if (key.startsWith(ENDS_WITH_TESTER)) {
			return new EndsWithTester(key.substring(ENDS_WITH_TESTER.length()));
		}
		if (key.startsWith(REGEX_TESTER)) {
			return new RegexTester(key.substring(REGEX_TESTER.length()));
		}
		if (key.startsWith(RANGE_TESTER)) {
			String[] p = key.substring(RANGE_TESTER.length()).split(":");
			if (p.length == 2) {
				return new LongRangeTester(Long.parseLong(p[0]), Long.parseLong(p[1]));
			}
		}
		if (key.startsWith(EQUALS_TESTER)) {
			return new EqualsTester(key.substring(EQUALS_TESTER.length()));
		}
		return new EqualsTester(key);
	}

}
