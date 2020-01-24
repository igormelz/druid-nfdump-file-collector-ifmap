package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;


@ApplicationScoped
public class CollectorRoute extends RouteBuilder {

    @ConfigProperty(name = "collector.sshKeyFile")
    String keyFile;

    @ConfigProperty(name = "collector.destination")
    String destination;

    @ConfigProperty(name = "collector.source")
    String source;

    @ConfigProperty(name="collector.nfdumpCmd")
    String nfdumpCmd;

    @ConfigProperty(name="collector.nfdumpArgs")
    String nfdumpArgs;
    
    @ConfigProperty(name="collector.druidIndexer")
    String druidIndexer;

    @Override
    public void configure() throws Exception {

        from("direct:nfdump").id("nfdump")
            .setHeader("CamelExecCommandArgs")
            .simple(nfdumpArgs + " ${date:now-5m:yyyy/MM/dd}/nfcapd.${date:now-5m:yyyyMMddHHmm}")
            .log("starting export nfdump ${header.CamelExecCommandArgs}")
            .toF("exec:%s",nfdumpCmd)
            .log("finished export");

        fromF("sftp://%s?privateKeyFile=%s&localWorkDirectory=tmp/&delete=true&sortBy=file:name", source, keyFile).id("collector")
            .onCompletion()    
                .setBody().simple("resource:classpath:index.json")
                .setHeader("Content-Type").constant("application/json")
                .toF("netty-http:http://%s:8081/druid/indexer/v1/task?httpMethod=POST&copyHeaders=false&mapHttpMessageHeaders=false",druidIndexer)
                .log("post druid index: ${body}")
            .end()
            .log("setting next file to:${file:name}")
            .process("collector")
            .marshal().gzipDeflater()
            .toF("sftp://%s?fileName=${file:name}.gz&privateKeyFile=%s&tempPrefix=tmp/", destination, keyFile)
            .log("wrote ${header.CamelFileNameProduced}")
            ;

        from("direct:mapCustomer").id("customerLookup")
            .process("customer");

    }
}
