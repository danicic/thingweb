package de.webthing.desc.pojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.webthing.desc.ThingDescriptionBuilder;

/**
 * Created by Johannes on 02.09.2015.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ThingDescription {
    
    @JsonProperty
    private Metadata metadata;

    @JsonProperty
    private List<InteractionDescription> interactions;
    
    @JsonCreator
    public ThingDescription(@JsonProperty("metadata") Metadata metadata, @JsonProperty("interactions") List<InteractionDescription> interactions) {
	this.metadata = metadata;
	this.interactions = interactions;
    }

    public static ThingDescriptionBuilder getBuilder() {
        return new ThingDescriptionBuilder();
    }
    
    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
	Map<String, Protocol> p = new HashMap<>();
	p.put("HTTP", new Protocol("http://example.com", 2));
	List<String> e = new ArrayList<>();
	e.add("JSON");
	e.add("XML");
	Metadata md = new Metadata("test", p, e);
	new ObjectMapper().convertValue(md, Metadata.class);
    }
}
