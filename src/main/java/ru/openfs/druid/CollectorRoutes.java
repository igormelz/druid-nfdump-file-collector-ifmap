package ru.openfs.druid;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;


import static org.apache.camel.builder.PredicateBuilder.and;


@ApplicationScoped
public class CollectorRoutes extends RouteBuilder {

    @ConfigProperty(name = "sshKeyFile")
    String keyFile;

    @ConfigProperty(name = "destination")
    String destination;

    @ConfigProperty(name = "destination.dir")
    String baseDir;

    @ConfigProperty(name = "source")
    String source;

    @ConfigProperty(name = "indexJson", defaultValue = "resource:classpath:index.json")
    String indexTaskTemplate;

    @ConfigProperty(name = "nfdump.cmd")
    String nfdumpCmd;

    @ConfigProperty(name = "nfdump.args")
    String nfdumpArgs;

    @ConfigProperty(name = "coordinator.url", defaultValue = "http://localhost:8081")
    String druid;

    @Override
    public void configure() throws Exception {

        // scheduled 5 min call nfdump to dump netflow file in pipe format
        from("direct:nfdump").id("CommandFileSource")
            
            // format agrs 
            .setHeader("CamelExecCommandArgs")
            .simple(nfdumpArgs + " ${date:now-5m:yyyy/MM/dd}/nfcapd.${date:now-5m:yyyyMMddHHmm}")
            
            // call commend to export file
            .log("starting export nfdump")
            .toF("exec:%s", nfdumpCmd)
            .log("export done");

        // collect netflow files from nfdump server
        fromF("sftp://%s?privateKeyFile=%s&localWorkDirectory=nfdump&delete=true&sortBy=file:name", source, keyFile)
            .streamCaching("true").id("FileCollector")
            
            // process netflow file records
            .log("starting process file:${file:name}")
            .process("fileProc")
            .log("processed ${header.AggregatedCount} records in ${header.AggregatedMillis} ms")

            // compress output file 
            .marshal().gzipDeflater()
            
            // store to local dir 
            .to("file:out?fileName=${file:name.noext}.${date:now:yyyyMMddHHmmss}.gz&tempPrefix=tmp/")
            .log("wrote processed file to:${header.CamelFileNameProduced}");

        // delivery to druid cluster
        from("file:out?delete=true").autoStartup(true).id("Datastore")
            .onCompletion()
                // parse index.json 
                .setHeader("baseDir",constant(baseDir))
                .setBody(simple(indexTaskTemplate))
                
                // post index task
                .setHeader("Content-Type",constant("application/json"))
                .toF("netty-http:%s/druid/indexer/v1/task?httpMethod=POST&copyHeaders=false&mapHttpMessageHeaders=false",
                        druid)
                
                // parse task id
                .setHeader("task").jsonpath("$.task")
                .log("posted index task: ${header.task}")
                
                // monitor task status
                .to("seda:monitorTaskStatus")
            .end()
            
            // load processed file to druid cluster
            .log("setting next file to:${file:name}")
            .toF("sftp://%s?privateKeyFile=%s&tempPrefix=tmp/", destination, keyFile)
            .log("delivered to:${header.CamelFileNameProduced}");

        // monitor task processing status 
        from("seda:monitorTaskStatus").id("DruidTaskMonitor")
            .loopDoWhile(and(header("task").isNotNull(),
                    header("status").isNotEqualTo("SUCCESS")))
            // get status
            .setHeader("CamelHttpPath",simple("/druid/indexer/v1/task/${header.task}/status"))
            .setBody(constant(null))
            .toF("netty-http:%s", druid)
            // parse status 
            .setHeader("status").jsonpath("$.status.status")
            .setHeader("task").jsonpath("$.status.id")
            .log("${header.status} -- ${header.task}")
            // wait 15 seconds
            .delay(15000)
        .end();

        // process record 
        from("direct:recordProc").id("NetflowRecordProcessor")
            .process("netflowRecord").process("lookupCustomer");

    }
}
