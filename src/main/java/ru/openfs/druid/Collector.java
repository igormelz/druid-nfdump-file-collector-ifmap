package ru.openfs.druid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.SynchronizationAdapter;
import org.apache.camel.util.FileUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named("collector")
public class Collector implements Processor {
    private static final Logger LOG = LoggerFactory.getLogger(Collector.class);
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    @ConfigProperty(name = "output.delimiter", defaultValue = ";")
    String outputDelimiter;

    @ConfigProperty(name = "output.tmpdir", defaultValue = ".tmp")
    String outputDir;

    @ConfigProperty(name = "output.header", defaultValue = "true")
    boolean printHeader;

    @ConfigProperty(name = "output.spec")
    String[] outputSpec;

    @ConfigProperty(name = "parser.spec")
    String[] parserSpec;

    @ConfigProperty(name = "parser.delimiter", defaultValue = "\\|")
    String parserDelimiter;

    @Inject
    protected ProducerTemplate producer;

    @Override
    public void process(Exchange exchange) throws Exception {

        // reset counter records
        int count = 0;
        long startProc = System.currentTimeMillis();

        // create temp output file
        File output = FileUtil.createTempFile("camel", ".tmp", new File(FileUtil.normalizePath(outputDir)));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Output file:{}", output.getAbsolutePath());
        }

        // do process
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getIn().getMandatoryBody(InputStream.class), UTF8_CHARSET));
                BufferedWriter writer = Files.newBufferedWriter(output.toPath(), StandardOpenOption.APPEND)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (count == 0 && printHeader) {
                    // print Header
                    writer.write(String.join(outputDelimiter, outputSpec));
                    writer.newLine();
                    count++;
                } else {
                    // parse line
                    Map<String, String> columns = parse(line);
                    // process parsed record
                    producer.sendBody("direct:recordProc", columns);
                    if(columns.containsKey("customer")) {
                        columns.put("ref",exchange.getIn().getHeader("CamelFileName", String.class));
                        // store record
                        writeRecord(columns, writer);
                        count++;
                    } else {
                        LOG.error("Skip record:{}", line);
                    }
                }
            }
        } catch (IOException x) {
            LOG.error("IOException:{}", x);
            return;
        } catch (CamelExecutionException e) {
            LOG.error("CamelException:{}", e);
            return;
        }
        LOG.info("Aggregated {} records in {} ms", count, System.currentTimeMillis() - startProc);
        // return
        exchange.getIn().setBody(output, File.class);
        exchange.getIn().setHeader("OutputCounter", count);
        exchange.addOnCompletion(new CleanupTempFile(output));
    }

    private Map<String, String> parse(String line) {
        String[] columns = line.split(parserDelimiter);
        if (columns.length != parserSpec.length) {
            LOG.warn("Skipping parsed columns");
        }
        Map<String, String> colParsed = new LinkedHashMap<String, String>(columns.length);
        for (int c = 0; c < parserSpec.length; c++) {
            if (!parserSpec[c].equalsIgnoreCase("%skip")) {
                colParsed.put(parserSpec[c], columns[c]);
            }
        }
        return colParsed;
    }

    private void writeRecord(Map<String, String> columns, BufferedWriter writer) throws IOException {

        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < outputSpec.length; i++) {
            if (columns.containsKey(outputSpec[i])) {
                answer.append(columns.get(outputSpec[i]));
            } else {
                answer.append("");
            }
            if (i < outputSpec.length - 1)
                answer.append(outputDelimiter);
        }
        writer.write(answer.toString(), 0, answer.length());
        writer.newLine();
    }

    private class CleanupTempFile extends SynchronizationAdapter {
        private File cleanupFile;

        public CleanupTempFile(File fileToCleanup) {
            cleanupFile = fileToCleanup;
        }

        @Override
        public void onDone(Exchange exchange) {
            FileUtil.deleteFile(this.cleanupFile);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Deleting temp file on completion: {} ", this.cleanupFile);
            }
        }
    }
}