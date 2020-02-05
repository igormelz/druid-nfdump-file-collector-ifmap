package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SimpleMapConfiguration {

    @ConfigProperty(name="map.protocol.spec")
    public String protocol;

    @ConfigProperty(name="map.uplink.spec")
    public String uplink;

    @Produces
    @Named("protocol")
    public SimpleMap getProtoMap() {
        return new SimpleMap(protocol);
    }

    @Produces
    @Named("uplink")
    public SimpleMap getUplinkMap() {
        return new SimpleMap(uplink);
    }

}