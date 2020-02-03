package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import org.apache.camel.ProducerTemplate;

@ApplicationScoped
public class CollectorScheduler {

    @Inject
    ProducerTemplate producer;

    @Scheduled(cron="45 0/5 * * * ?")
    public void export(ScheduledExecution execution) {
        producer.sendBody("direct:nfdump",null);
    }

    // @Scheduled(every = "15m")
    // public void loadMap() {
    //     String sql = "select concat('IPADDR@',network_net),concat(replace(trim(int_title),'\"',''),' (',trim(weblogin),')'),serv_status,net_vlan from customer_user_internet";
    //     producer.sendBody("direct:loadMap",sql);
    // }
}