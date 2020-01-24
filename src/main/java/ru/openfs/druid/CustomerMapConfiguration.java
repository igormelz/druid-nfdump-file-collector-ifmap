package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import ru.openfs.druid.maptable.MapTableBean;

@ApplicationScoped
public class CustomerMapConfiguration {

    @ConfigProperty(name="customer.mapFileUrl")
    String mapFileUrl;

    @ConfigProperty(name="customer.pattern")
    String pattern;

    @ConfigProperty(name="customer.keyColumnIndex")
    String keyColumnIndex;

    @ConfigProperty(name="customer.dataColumnIndex")
    String dataColumnIndex;

    @ConfigProperty(name="customer.mapKeys")
    String mapKeys;

    @ConfigProperty(name="customer.mapValues")
    String mapValues;

    @ConfigProperty(name="customer.valueDefaults")
    String[] valueDefaults;

    @ConfigProperty(name="customer.useHeaders", defaultValue = "false")
    boolean useHeaders;

    @ConfigProperty(name="customer.reloadInterval")
    long reloadInterval;

    @Produces
    @Named("customer")
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