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

}