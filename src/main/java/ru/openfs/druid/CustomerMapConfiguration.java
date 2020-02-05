package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ru.openfs.druid.maptable.MapTableBean;

@ApplicationScoped
public class CustomerMapConfiguration {

    @ConfigProperty(name="lookup.customer.mapFileUrl")
    public String mapFileUrl;

    @ConfigProperty(name="lookup.customer.pattern")
    public String pattern;

    @ConfigProperty(name="lookup.customer.keyColumnIndex")
    public String keyColumnIndex;

    @ConfigProperty(name="lookup.customer.dataColumnIndex")
    public String dataColumnIndex;

    @ConfigProperty(name="lookup.customer.mapKeys")
    public String mapKeys;

    @ConfigProperty(name="lookup.customer.mapValues")
    public String mapValues;

    @ConfigProperty(name="lookup.customer.valueDefaults")
    String[] valueDefaults;

    @ConfigProperty(name="lookup.customer.useHeaders", defaultValue = "false")
    boolean useHeaders;

    @ConfigProperty(name="lookup.customer.reloadInterval")
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