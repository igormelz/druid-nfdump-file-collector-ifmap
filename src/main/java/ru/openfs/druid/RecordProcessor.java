package ru.openfs.druid;

import java.util.Date;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.openfs.druid.maptable.IPUtil;

@ApplicationScoped
@Named("netflowRecord")
public class RecordProcessor implements Processor {
    private static final Logger LOG = LoggerFactory.getLogger(RecordProcessor.class);

    @Inject
    @Named("protocol")
    SimpleMap protocol;

    @Inject
    @Named("direction")
    SimpleMap flowDirection;

    @Override
    public void process(Exchange e) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> record = (Map<String, String>) e.getIn().getMandatoryBody(Map.class);

        // validate timestamp
        long timestamp = Long.parseLong(record.get("last")) * 1000;
        Date date = new Date();
        if (timestamp > date.getTime()) {
            e.setProperty(Exchange.ROUTE_STOP, true);
            LOG.warn("Future timestamp:{} for now:{}", timestamp, date);
            return;
        }
        // resolve netflow direction
        if (flowDirection.find(record.get("inif"))) {
            // inbound (recv) flow
            record.put("direction", "recv");
            record.put("uplink", flowDirection.get(record.get("inif")));
            record.put("downlink", record.remove("outif"));
            record.put("local_ip", IPUtil.ntoa(record.get("dstip")));
            record.put("local_port", record.remove("dstport"));
            record.put("ipaddr", record.remove("dstip"));
            record.put("remote_ip", IPUtil.ntoa(record.remove("srcip")));
            record.put("remote_port", record.remove("srcport"));
            record.put("asn", record.remove("srcas"));
        } else if (flowDirection.find(record.get("outif"))) {
            // outbound (sent) flow
            record.put("direction", "sent");
            record.put("uplink", flowDirection.get(record.remove("outif")));
            record.put("downlink", record.remove("inif"));
            record.put("local_ip", IPUtil.ntoa(record.get("srcip")));
            record.put("ipaddr", record.remove("srcip"));
            record.put("local_port", record.remove("srcport"));
            record.put("remote_ip", IPUtil.ntoa(record.remove("dstip")));
            record.put("remote_port", record.remove("dstport"));
            record.put("asn", record.remove("dstas"));
        } else {
            LOG.warn("Wrong direction for in:{}, out:{}. Check uplink map.", record.get("inif"), record.get("outif"));
            e.setProperty(Exchange.ROUTE_STOP, true);
            return;
        }
        
        // mapping proto to protocol name
        record.put("proto", protocol.get(record.get("proto")));

        // format tcp,udp port as "port/proto"
        if (record.get("proto").equalsIgnoreCase("udp") || record.get("proto").equalsIgnoreCase("tcp")) {
            record.put("local_port", record.get("local_port") + "/" + record.get("proto"));
            record.put("remote_port", record.get("remote_port") + "/" + record.get("proto"));
        } else {
            // set default value 
            record.put("local_port","0");
            record.put("remote_port","0");
        }
    }

}