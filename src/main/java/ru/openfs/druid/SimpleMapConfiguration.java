package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SimpleMapConfiguration {

    @ConfigProperty(name="collector.protocol.spec")
    public String protocol;

    @ConfigProperty(name="collector.flowDirection.spec")
    public String flowDirection;

    @Produces
    @Named("protocol")
    public SimpleMap getProtoMap() {
        return new SimpleMap(protocol);
    }

    @Produces
    @Named("direction")
    public SimpleMap getDirectionMap() {
        return new SimpleMap(flowDirection);
    }

}