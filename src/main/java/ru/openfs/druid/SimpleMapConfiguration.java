package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SimpleMapConfiguration {

    @ConfigProperty(name="map.uplink.spec")
    public String uplink;

    @ConfigProperty(name="map.protocol.spec")
    public String protocol;

    @Produces
    @Named("uplink")
    public SimpleMap getPeerMap() {
        return new SimpleMap(uplink);
    }

    @Produces
    @Named("protocol")
    public SimpleMap getProtoMap() {
        return new SimpleMap(protocol);
    }

}