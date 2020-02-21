package org.springframework.samples.petclinic.opencensus;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpRequest;

import io.opencensus.common.Scope;
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter;
import io.opencensus.implcore.tags.TagContextImpl;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.BucketBoundaries;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.propagation.TextFormat;
import io.opencensus.trace.samplers.Samplers;
import io.prometheus.client.exporter.HTTPServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenCensusService {

    /**
     * OpenCensus Stuff
     * *************************************************
     */
    // The latency in milliseconds
    private static final Measure.MeasureDouble M_LATENCY_MS = Measure.MeasureDouble.create("service_response_time", "The latency in milliseconds", "ms");

    private final TagContextTextSerializer tagContextSerializer = new TagContextTextSerializer();

    private final Tagger tagger = Tags.getTagger();
    private final TracerWrapper tracer = TracerWrapper.getInstance();

    private final TextFormat textFormat = Tracing.getPropagationComponent().getB3Format();
    public final TextFormat.Setter<HttpRequest> httpRequestSetter = new TextFormat.Setter<HttpRequest>() {
        public void put(HttpRequest carrier, String key, String value) {
            carrier.getHeaders().add(key, value);
        }
    };

    public final TextFormat.Getter<HttpServletRequest> httpRequestGetter = new TextFormat.Getter<HttpServletRequest>() {
        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }
    };

    private final StatsRecorder statsRecorder = Stats.getStatsRecorder();
    // The tags
    public static final TagKey KEY_SERVICE = TagKey.create("service");
    public static final TagKey KEY_APPLICATION = TagKey.create("application");
    public static final TagKey KEY_NODE = TagKey.create("node");
    public static final TagKey KEY_SERVICE_ORIG = TagKey.create("orig_service");
    public static final TagKey KEY_APPLICATION_ORIG = TagKey.create("orig_application");
    public static final TagKey KEY_NODE_ORIG = TagKey.create("orig_node");
    public static final TagKey KEY_BT = TagKey.create("bt");

    private String application = "_";
    private String service = "_";
    private String node = "_";

    private OpenCensusService() {
    }

    private void registerAllViews() {
        // Defining the distribution aggregations
        Aggregation latencyDistribution = Aggregation.Distribution.create(BucketBoundaries.create(
            Arrays.asList(
                // [>=0ms, >=25ms, >=50ms, >=75ms, >=100ms, >=200ms, >=400ms, >=600ms, >=800ms, >=1s, >=2s, >=4s, >=6s]
                0.0, 25.0, 50.0, 75.0, 100.0, 200.0, 400.0, 600.0, 800.0, 1000.0, 2000.0, 4000.0, 6000.0, 10000.0)
        ));

        // So tagKeys
        List<TagKey> keys = new ArrayList<>();
        keys.add(KEY_APPLICATION);
        keys.add(KEY_SERVICE);
        keys.add(KEY_NODE);
        keys.add(KEY_APPLICATION_ORIG);
        keys.add(KEY_SERVICE_ORIG);
        keys.add(KEY_NODE_ORIG);
        keys.add(KEY_BT);

        // Create the view manager
        Stats.getViewManager().registerView(View.create(View.Name.create("PetClinic/response_time"),
            "The distribution of latencies", M_LATENCY_MS, latencyDistribution, keys));
    }

    private void setupOpenCensus() throws IOException {
        registerAllViews();
        // Setup Metrics exporter
        PrometheusStatsCollector.createAndRegister();
        String strPort = System.getProperty("promPort");
        int port = strPort== null? 9091:Integer.parseInt(strPort);

        HTTPServer server =
            new HTTPServer(port,true);

        // Setup tracing exporter
        TraceConfig traceConfig = Tracing.getTraceConfig();
        traceConfig.updateActiveTraceParams(
            traceConfig.getActiveTraceParams().toBuilder().setSampler(Samplers.alwaysSample()).build());
        String jaegerService= System.getProperty("jaegerService");
        JaegerTraceExporter.createAndRegister("http://"+jaegerService+"/api/traces", this.service);
    }

    public void registerContext(String application, String service, String node) {
        try {
            this.application = application;
            this.service = service;
            this.node = node;
            setupOpenCensus();
        } catch (Exception e) {
            log.info("RegisterContext failed");
        }
    }

    public Tagger getTagger() {
        return tagger;
    }

    public TracerWrapper getTracer() {
        return tracer;
    }

    public void injectHttpHeaders(HttpRequest request){
        textFormat.inject(tracer.getCurrentSpan().getContext(), request, httpRequestSetter);
        tagContextSerializer.inject(tagger.getCurrentTagContext(),request);
    }

    public Scope createSpanFromIncomingRequest(HttpServletRequest request){
        try {
            SpanContext spanContext = textFormat.extract(request, httpRequestGetter);
            return tracer.spanBuilderWithRemoteParent(request.getMethod(), spanContext).startScopedSpan();
        } catch(Exception e){
            log.info("CreateSpanFromIncomingRequest failed");
        }

        return OpenCensusService.getInstance().getTracer().spanBuilder(request.getMethod()).startScopedSpan();
    }

    public Scope createTagContextFromIncomingRequest(HttpServletRequest request){
        TagContextImpl remoteTagContext = (TagContextImpl)tagContextSerializer.deserialize(request, tagger);
        TagContextBuilder tagCtxBuilder = OpenCensusService.getInstance().getTagger().emptyBuilder();
        tagCtxBuilder.put(KEY_APPLICATION, TagValue.create(this.application))
            .put(KEY_SERVICE, TagValue.create(this.service))
            .put(KEY_NODE, TagValue.create(this.node));
        if(remoteTagContext != null && !remoteTagContext.getTags().isEmpty()){
            TagValue origApplication = remoteTagContext.getTags().getOrDefault(KEY_APPLICATION, TagValue.create("_"));
            TagValue origService = remoteTagContext.getTags().getOrDefault(KEY_SERVICE, TagValue.create("_"));
            TagValue origNode = remoteTagContext.getTags().getOrDefault(KEY_NODE, TagValue.create("_"));
            TagValue bt = remoteTagContext.getTags().getOrDefault(KEY_BT, TagValue.create("Unknown"));
            tagCtxBuilder.put(KEY_APPLICATION_ORIG, origApplication)
                .put(KEY_SERVICE_ORIG, origService)
                .put(KEY_NODE_ORIG, origNode)
                .put(KEY_BT, bt);
        }else {
            String businessTransactionName = detectBT(request);
            TagValue bt = remoteTagContext.getTags().getOrDefault(KEY_BT, TagValue.create(businessTransactionName));
            tagCtxBuilder.put(KEY_BT, bt);
        }

        return tagCtxBuilder.buildScoped();
    }

     public String detectBT(HttpServletRequest request){
        String method = request.getMethod();
        String url = request.getRequestURL().toString().substring(7);
        
        int idxPath = url.indexOf('/');
        if(idxPath>=0){
            String path = url.substring(idxPath);
             if(method.equalsIgnoreCase("GET") && this.service == "api-gateway" && path.startsWith("/owners/")){
                return "Owner-Details";
            }else if(method.equalsIgnoreCase("POST") && this.service == "customers-service" && path.matches("/owners/.*/pets")){
                return "Add Pet";
            }else if(method.equalsIgnoreCase("GET") && this.service == "customers-service" && path.matches("/owners/?")){
                return "All Owners";
            }else if(method.equalsIgnoreCase("GET") && this.service == "customers-service" && path.matches("/owners/.+")){
                return "Get Owner";
            }else if(method.equalsIgnoreCase("PUT") && this.service == "customers-service" && path.matches("/owners/.+")){
                return "Update Owner";
            }else if(method.equalsIgnoreCase("POST") && this.service == "customers-service" && path.startsWith("/owners")){
                return "Create Owner";
            }else if(method.equalsIgnoreCase("GET") && this.service == "customers-service" && path.startsWith("/petTypes")){
                return "GetPetTypes";
            }else if(method.equalsIgnoreCase("GET") && this.service == "visits-service" && path.matches("/owners/.*/pets/.*/visits")){
                return "Show Visits";
            }else if(method.equalsIgnoreCase("POST") && this.service == "visits-service" && path.matches("/owners/.*/pets/.*/visits")) {
                return "Add Visit";
            }else if(method.equalsIgnoreCase("GET") && this.service == "api-gateway" && path.startsWith("/index")){
                return "Home";
            }
        }else{
            return "Home";
        }

        return "Unknown";
    }

    public void writeMetric(double durationMs){
        statsRecorder.newMeasureMap().put(M_LATENCY_MS, durationMs).record();
    }

    private static OpenCensusService instance = null;

    public static OpenCensusService getInstance() {
        if(instance == null){
            instance = new OpenCensusService();
        }

        return instance;
    }
}
