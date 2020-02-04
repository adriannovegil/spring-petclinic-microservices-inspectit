package org.springframework.samples.petclinic.opencensus;

import io.opencensus.implcore.tags.TagContextImpl;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tags;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Sampler;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanBuilder;

import java.util.List;
import java.util.Map;

public class SpanBuilderWrapper extends SpanBuilder{

    private final SpanBuilder builder;

    public SpanBuilderWrapper(SpanBuilder builder) {
        this.builder = builder;
    }

    @Override
    public SpanBuilder setParentLinks(List<Span> parentLinks) {
        return builder.setParentLinks(parentLinks);
    }

    @Override
    public SpanBuilder setRecordEvents(boolean recordEvents) {
        return builder.setRecordEvents(recordEvents);
    }

    @Override
    public SpanBuilder setSampler(Sampler sampler) {
        return builder.setSampler(sampler);
    }

    @Override
    public SpanBuilder setSpanKind(Span.Kind spanKind) {
        return builder.setSpanKind(spanKind);
    }

    @Override
    public Span startSpan() {
        Span span = builder.startSpan();
        TagContextImpl tagContext = (TagContextImpl)Tags.getTagger().getCurrentTagContext();
        for(Map.Entry<TagKey,TagValue> tag : tagContext.getTags().entrySet()){
            span.putAttribute(tag.getKey().getName(), AttributeValue.stringAttributeValue(tag.getValue().asString()));
        }
    
        return span;
    }




}
