package ru.openfs.druid;

import java.util.Date;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RecordProcessing extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(RecordProcessing.class);

    @Inject
    @Named("uplink")
    SimpleMap peer;

    @Inject
    @Named("protocol")
    SimpleMap protocol;

    @Override
    public void configure() throws Exception {

        from("direct:recordProc").id("NetflowProcessing")
                // process flow direction
                .process().exchange(e -> {

                    @SuppressWarnings("unchecked")
                    Map<String, String> h = (Map<String, String>) e.getIn().getBody(Map.class);

                    // validate timestamp
                    long timestamp = Long.parseLong(h.get("last")) * 1000;
                    Date date = new Date();
                    if (timestamp > date.getTime()) {
                        e.setProperty(Exchange.ROUTE_STOP, true);
                        LOG.error("Wrong timestamp:{}", timestamp);
                        return;
                    }

                    // inbound (recv) flow
                    if (peer.find(h.get("inif"))) {
                        h.put("direction", "recv");
                        h.put("uplink", peer.get(h.remove("inif")));
                        h.put("local_ip", IPUtils.ntoa(h.get("dstip")));
                        h.put("local_port", h.remove("dstport"));
                        h.put("ipaddr", h.remove("dstip"));
                        h.put("remote_ip", IPUtils.ntoa(h.remove("srcip")));
                        h.put("remote_port", h.remove("srcport"));
                        h.put("asn", h.remove("srcas"));
                    } else if (peer.find(h.get("outif"))) {
                        // outbound (sent) flow
                        h.put("direction", "sent");
                        h.put("uplink", peer.get(h.remove("outif")));
                        h.put("local_ip", IPUtils.ntoa(h.get("srcip")));
                        h.put("ipaddr", h.remove("srcip"));
                        h.put("local_port", h.remove("srcport"));
                        h.put("remote_ip", IPUtils.ntoa(h.remove("dstip")));
                        h.put("remote_port", h.remove("dstport"));
                        h.put("asn", h.remove("dstas"));
                    } else {
                        e.setProperty(Exchange.ROUTE_STOP, true);
                        LOG.error("Unknown peer for inif:{}, outif:{}", h.get("inif"), h.get("outif"));
                        return;
                    }

                    if (protocol.find(h.get("proto"))) {
                        h.put("proto", protocol.get(h.get("proto")));
                    }
                })
                // lookup customer
                .process("customer");

    }
}
