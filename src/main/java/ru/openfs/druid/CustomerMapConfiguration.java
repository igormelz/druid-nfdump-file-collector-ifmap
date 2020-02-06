package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ru.openfs.druid.maptable.MapTableBean;

@ApplicationScoped
public class CustomerMapConfiguration {

    @ConfigProperty(name="collector.customer.mapFileUrl")
    public String mapFileUrl;

    @ConfigProperty(name="collector.customer.pattern")
    public String pattern;

    @ConfigProperty(name="collector.customer.keyColumnIndex")
    public String keyColumnIndex;

    @ConfigProperty(name="collector.customer.dataColumnIndex")
    public String dataColumnIndex;

    @ConfigProperty(name="collector.customer.mapKeys")
    public String mapKeys;

    @ConfigProperty(name="collector.customer.mapValues")
    public String mapValues;

    @ConfigProperty(name="collector.customer.valueDefaults")
    String[] valueDefaults;

    @ConfigProperty(name="collector.customer.useHeaders", defaultValue = "false")
    boolean useHeaders;

    @ConfigProperty(name="collector.customer.reloadInterval")
    long reloadInterval;

    @Produces
    @Named("lookupCustomer")
    public MapTableBean getTable() {
        MapTableBean map = new MapTableBean();
        map.setMapFileURL(mapFileUrl);
        map.setKeyColumnIndex(keyColumnIndex);
        map.setDataColumnIndex(dataColumnIndex);
        map.setPattern(pattern);
        map.setMapKeys(mapKeys);
        map.setMapValues(mapValues);
        map.setValueDefaults(valueDefaults);
        map.setReloadInterval(reloadInterval);
        map.setUseHeaders(useHeaders);
        return map;
    }
}