package org.springframework.samples.petclinic.opencensus;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpRequest;

import io.opencensus.implcore.tags.TagContextImpl;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;

public class TagContextTextSerializer {

    private static final String TAGS_HEADER = "octags";

    public void inject(TagContext context, HttpRequest request){
        TagContextImpl contextImpl = (TagContextImpl)context;
        String result = "";
        for(Map.Entry<TagKey,TagValue> entry : contextImpl.getTags().entrySet()){
            if(!result.isEmpty()){
                result += ',';
            }
            result += entry.getKey().getName();
            result += '=';
            result += entry.getValue().asString();
        }

        request.getHeaders().add(TAGS_HEADER, result);
    }

    public TagContext deserialize(HttpServletRequest request, Tagger tagger){
        String contextStr = request.getHeader(TAGS_HEADER);
        if(contextStr != null){
            TagContextBuilder builder = tagger.emptyBuilder();
            String [] tags = contextStr.split(",");
            for(int i = 0; i< tags.length; i++){
                String [] keyValue = tags[i].split("=");
                builder.put(TagKey.create(keyValue[0]), TagValue.create(keyValue[1]));
            }
            return builder.build();
        }else{
            return tagger.empty();
        }

    }
}
