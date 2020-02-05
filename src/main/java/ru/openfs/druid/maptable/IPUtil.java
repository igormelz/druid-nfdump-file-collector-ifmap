package ru.openfs.druid.maptable;

import java.util.StringTokenizer;


public class IPUtil {

	public final static int aton(String ipaddr) throws NumberFormatException {

		// Check if the string is valid

		if (ipaddr == null || ipaddr.length() < 7 || ipaddr.length() > 15)
			return 0;

		// Check the address string, should be n.n.n.n format

		StringTokenizer token = new StringTokenizer(ipaddr, ".");
		if (token.countTokens() != 4) {
			throw new NumberFormatException("Unable to parse IP Address: '" + ipaddr + "'");
		}

		int ipInt = 0;		
		while (token.hasMoreTokens()) {

			// Get the current token and convert to an integer value
			String ipNum = token.nextToken();

			// Validate the current address part
			int ipVal = Integer.valueOf(ipNum).intValue();
			if (ipVal < 0 || ipVal > 255) {
				throw new NumberFormatException("Unable to parse IP Address: '" + ipaddr + "'");
			}

			// Add to the integer address
			ipInt = (ipInt << 8) + ipVal;

		}

		// Return the integer address
		return ipInt;
	}

	public final static String ntoa(String ipaddr) {
		return ntoa(Integer.parseUnsignedInt(ipaddr));
	}

	public final static String ntoa(int ip) {
		return ((ip >> 24) & 0xFF) + "." +

				((ip >> 16) & 0xFF) + "." +

				((ip >> 8) & 0xFF) + "." +

				(ip & 0xFF);
	}
}
