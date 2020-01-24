package ru.openfs.druid.maptable;

import ru.openfs.druid.IPUtils;

public class IpAddrTester extends ColumnTester {
	private final int network;
	private final int netmask;
	private final String subnet;

	/**
	 * @param subnet as network/mask or ipaddr
	 * @throws IllegalArgumentException
	 */
	public IpAddrTester(String subnet) throws IllegalArgumentException {
		this.subnet = subnet;
		try {
			int i = subnet.indexOf("/");
			if (i == -1) {
				this.network = IPUtils.aton(subnet);
				this.netmask = -1;
			} else {
				int ipAddress = IPUtils.aton(subnet.substring(0, i));
				int j = Integer.parseInt(subnet.substring(i + 1));
				this.netmask = (-1 << 32 - j);
				this.network = ipAddress & this.netmask;
			}
		} catch (NumberFormatException localNumberFormatException) {
			throw new IllegalArgumentException("Illegal pattern format: " + subnet);
		}
	}

	public boolean test(String ipaddr) {
		if (ipaddr == null) {
			return false;
		}

		int ipv4 = 0;
		if (ipaddr.indexOf(".") > 0) {
			try {
				ipv4 = IPUtils.aton(ipaddr);
			} catch (NumberFormatException localNumberFormatException) {
				return false;
			}
		} else {
			try {
				ipv4 = Integer.parseUnsignedInt(ipaddr);
			} catch (NumberFormatException parseException) {
				return false;
			}
		}
		return test(ipv4);
	}

	public boolean test(int ipaddr) {
		return (ipaddr & netmask) == network;
	}

	@Override
	public boolean equals(Object net) {
		if (net == this) {
			return true;
		}
		if (!(net instanceof IpAddrTester)) {
			return false;
		}
		return (this.network == ((IpAddrTester) net).network) && (this.netmask == ((IpAddrTester) net).netmask);
	}

	@Override
	public String toString() {
		return IPADDR_TESTER + this.subnet;
	}

	@Override
	public int hashCode() {
		return network;
	}
}
