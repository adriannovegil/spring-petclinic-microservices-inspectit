package org.springframework.samples.petclinic.opencensus;

import io.opencensus.trace.*;

public class TracerWrapper extends Tracer{

    private static TracerWrapper tracer;

    public static TracerWrapper getInstance(){
        if(tracer == null){
            tracer = new TracerWrapper();
        }
        return tracer;
    }

    private TracerWrapper(){

    }

    @Override
    public SpanBuilder spanBuilderWithExplicitParent(String spanName, Span parent) {
        return new SpanBuilderWrapper(Tracing.getTracer().spanBuilderWithExplicitParent(spanName,parent));
    }

    @Override
    public SpanBuilder spanBuilderWithRemoteParent(String spanName, SpanContext remoteParentSpanContext) {
        return new SpanBuilderWrapper(Tracing.getTracer().spanBuilderWithRemoteParent(spanName, remoteParentSpanContext));
    }

    public SpanBuilder getSpanBuilder(String spanName) {
        return new SpanBuilderWrapper(Tracing.getTracer().spanBuilder(spanName));
    }

}
