package ru.openfs.druid.maptable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.CamelExchangeException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Singleton
@RegisterForReflection
public class MapTableBean extends ServiceSupport implements Processor, Predicate, CamelContextAware {
	private static final Logger log = LoggerFactory.getLogger(MapTableBean.class);

	public static final String MAP_RELOAD = "MapTable.Reload";
	public static final String MAP_KEYS = "MapTable.MapKeys";
	public static final String MAP_VALUES = "MapTable.MapValues";

	String mapFileURL;
	private URL mapURL;
	Pattern pattern;
	int[] keyColumnIndex;
	int[] dataColumnIndex;
	private Pattern skip;
	String[] mapKeys;
	String[] mapValues;
	String[] valueDefaults;
	boolean useHeaders = false;
	long reloadInterval;

	private MapTableTree tree;
	private Map<Integer, Object> cache = new ConcurrentHashMap<>(10000);
	private CamelContext camelContext;
	private ScheduledExecutorService timeoutCheckerExecutorService;
	private boolean shutdownTimeoutCheckerExecutorService;

	public MapTableBean() {
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();

		// get body 
		@SuppressWarnings("unchecked")
		Map<String, Object> body = isUseHeaders() ? in.getHeaders() : in.getMandatoryBody(Map.class);

		// fill keys
		String[] keys = null; 
		if (exchange.getIn().getHeader(MAP_KEYS) != null) {
			String[] headerKeys = exchange.getIn().removeHeader(MAP_KEYS).toString().split(",");
			if (headerKeys.length != keyColumnIndex.length) {
				throw new CamelExchangeException("Number keys in header MapTableKeys is not equal keyIndex", exchange);
			}
			keys = prepareKeys(body, headerKeys);
		} else {
			keys = prepareKeys(body, mapKeys);
		}
		
		// lookup
		String[] ret = null;
		// lookup in cache
		int hashKey = Arrays.hashCode(keys);
		if (cache.containsKey(hashKey)) {
			ret = (String[]) cache.get(hashKey);
		} else {
			// lookup in tree
			ret = this.tree.get(keys);
			if (ret != null && ret.length != mapValues.length) {
				throw new CamelExchangeException("Number keys in header MapTableKeys is not equal keyIndex", exchange);
			}
			// set default values
			if (ret == null && valueDefaults != null) {
				ret = valueDefaults;
			}
			// store to cache
			cache.put(hashKey, ret);
		}

		// response
		if (in.getHeader(MAP_VALUES) != null) {
			String[] headerValues = in.removeHeader(MAP_VALUES).toString().split(",");
			mapResult(body, headerValues, ret);
		} else {
			mapResult(body, mapValues, ret);
		}
	}

	@Override
	public boolean matches(Exchange exchange) {
		Message in = exchange.getIn();
		if (!(in.getBody() instanceof Map)) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> body = in.getBody(Map.class);
		String[] keys = prepareKeys(body, mapKeys);
		return tree.get(keys) != null;
	}


	private String[] prepareKeys(Map<String, Object> body, String[] mapKeyNames) {
		String[] keys = new String[keyColumnIndex.length];
		for (int i = 0; i < mapKeyNames.length; i++) {
			if (body.get(mapKeyNames[i]) != null) {
				keys[i] = body.get(mapKeyNames[i]).toString();
				log.debug("lookupkey[{}]={}", i, keys[i]);
			} else {
				log.warn("Defined key {} not found in body", mapKeyNames[i]);
			}
		}
		return keys;
	}

	private void mapResult(Map<String, Object> body, String[] mapValueNames, String[] result) {
		for (int i = 0; i < result.length; i++) {
			body.put(mapValueNames[i], result[i]);
			log.debug("result:{}={}", mapValueNames[i], result[i]);
		}
	}

	private String[] populateArray(int[] k, String[] entry) {
		String[] answer = new String[k.length];
		for (int i = 0; i < k.length; i++) {
			answer[i] = entry[k[i]];
		}
		return answer;
	}

	private void reload() {
		MapTableTree newTree = new MapTableTree();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(mapURL.openStream(), Charset.forName("UTF-8")))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (skip != null && skip.matcher(line).matches()) {
					continue;
				}
				Matcher m = pattern.matcher(line);
				if (!m.matches()) {
					log.warn("Skip pattern not match:{}", line);
				} else {
					String[] parsed = new String[m.groupCount()];
					for (int i = 1; i <= m.groupCount(); i++) {
						parsed[i - 1] = m.group(i);
						log.debug("Parsed group:{} :{}", i, m.group(i));
					}

					if (parsed.length >= keyColumnIndex.length + dataColumnIndex.length) {
						newTree.put(populateArray(keyColumnIndex, parsed), populateArray(dataColumnIndex, parsed));
					} else {
						log.warn("Skip too short:{}", line);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.tree = newTree;
		log.info("Loaded {} rows from: {}", this.tree.size(), mapFileURL);
		// clear cache ?
		cache.clear();
		if (log.isDebugEnabled()) {
			log.debug(tree.toString());
		}
	}

	public String toString() {
		return "Map source [" + mapFileURL + "]";
	}

	private final class MapTableReloadTask implements Runnable {

		@Override
		public void run() {
			reload();
		}

	}

	@Override
	protected void doStart() throws Exception {

		if (mapKeys.length != keyColumnIndex.length) {
			throw new IllegalArgumentException("Not equal mapKeys and keyIndex");
		}

		if (mapValues.length != dataColumnIndex.length) {
			throw new IllegalArgumentException("Not equal mapValues and valueIndex");
		}

		if (this.tree == null) {
			this.mapURL = ResourceHelper.resolveMandatoryResourceAsUrl(camelContext.getClassResolver(),
					getMapFileURL());
		}

		// load map from resource
		reload();

		if (getReloadInterval() > 0) {
			log.info("Using ReloadInterval to run every {} seconds.", getReloadInterval());
			if (getTimeoutCheckerExecutorService() == null) {
				setTimeoutCheckerExecutorService(camelContext.getExecutorServiceManager().newScheduledThreadPool(this,
						"MapLookupServiceReload", 1));
				shutdownTimeoutCheckerExecutorService = true;
			}
			// trigger completion based on interval
			getTimeoutCheckerExecutorService().scheduleAtFixedRate(new MapTableReloadTask(), getReloadInterval(),
					getReloadInterval(), TimeUnit.SECONDS);
		}

	}

	@Override
	protected void doStop() throws Exception {
		if (shutdownTimeoutCheckerExecutorService && timeoutCheckerExecutorService != null) {
			camelContext.getExecutorServiceManager().shutdown(timeoutCheckerExecutorService);
			timeoutCheckerExecutorService = null;
			shutdownTimeoutCheckerExecutorService = false;
		}
	}

	public void setTimeoutCheckerExecutorService(ScheduledExecutorService timeoutCheckerExecutorService) {
		this.timeoutCheckerExecutorService = timeoutCheckerExecutorService;
	}

	public ScheduledExecutorService getTimeoutCheckerExecutorService() {
		return timeoutCheckerExecutorService;
	}

	public long getReloadInterval() {
		return reloadInterval;
	}

	public void setReloadInterval(long reloadInterval) {
		this.reloadInterval = reloadInterval;
	}

	@Override
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
	}

	@Override
	public CamelContext getCamelContext() {
		return this.camelContext;
	}

	public String[] getMapKeys() {
		return mapKeys;
	}

	public void setMapKeys(String mapKeys) {
		StringHelper.notEmpty(mapKeys, "mapKeys");
		this.mapKeys = mapKeys.split(",");
	}

	public void setMapKeys(String... mapKeys) {
		this.mapKeys = mapKeys;
	}

	public String[] getMapValues() {
		return mapValues;
	}

	public void setMapValues(String mapValues) {
		StringHelper.notEmpty(mapValues, "mapValues");
		this.mapValues = mapValues.split(",");
	}

	public void setMapValues(String... mapValues) {
		this.mapValues = mapValues;
	}

	public void setSkip(String skip) {
		this.skip = Pattern.compile(skip);
	}

	public void setKeyColumnIndex(String keyColumnIndex) {
		String[] k = keyColumnIndex.split(",");
		this.keyColumnIndex = new int[k.length];
		for (int i = 0; i < k.length; i++) {
			this.keyColumnIndex[i] = Integer.parseInt(k[i]) - 1;
		}
	}

	public void setKeyColumnIndex(int... keyIndex) {
		this.keyColumnIndex = new int[keyIndex.length];
		for (int i = 0; i < keyIndex.length; i++) {
			this.keyColumnIndex[i] = keyIndex[i] - 1;
		}
	}

	public void setDataColumnIndex(String dataColumnIndex) {
		String v[] = dataColumnIndex.split(",");
		this.dataColumnIndex = new int[v.length];
		for (int i = 0; i < v.length; i++) {
			this.dataColumnIndex[i] = Integer.parseInt(v[i]) - 1;
		}
	}

	public void setDataColumnIndex(int... dataIndex) {
		this.dataColumnIndex = new int[dataIndex.length];
		for (int i = 0; i < dataIndex.length; i++) {
			this.dataColumnIndex[i] = dataIndex[i] - 1;
		}
	}

	public void setMapFileURL(String mapFileUrl) {
		this.mapFileURL = mapFileUrl;
	}

	public String getMapFileURL() {
		return this.mapFileURL;
	}

	public void setPattern(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	public String[] getValueDefaults() {
		return valueDefaults;
	}

	public void setValueDefaults(String... valueDefaults) {
		this.valueDefaults = valueDefaults;
	}
	
	public void setValueDefaults(String valueDefaults) {
		this.valueDefaults = valueDefaults.split(",");
	}

	public boolean isUseHeaders() {
		return useHeaders;
	}

	public void setUseHeaders(boolean useHeaders) {
		this.useHeaders = useHeaders;
	}
}
