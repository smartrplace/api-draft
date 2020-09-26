package org.smartrplace.apps.hw.install.prop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JSONManagement {
	/** Export result of MultiEvaluationInstance to json
	 * 
	 * @param fileName
	 * @param multiResult all public fields are serialized into a JSON (recursive)
	 */
	public static String getJSON(Object multiResult) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		try {
			return mapper.writeValueAsString(multiResult);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static <M> M importFromJSON(String json, Class<M> structure) {

		JacksonNonBlockingObjectMapperFactory factory = new JacksonNonBlockingObjectMapperFactory();
		factory.setJsonDeserializers(Arrays.asList(new StdDeserializer[]{
		    // StdDeserializer, here, comes from Jackson (org.codehaus.jackson.map.deser.StdDeserializer)
		    new NumberDeserializers.ShortDeserializer(Short.class, null),
		    new NumberDeserializers.IntegerDeserializer(Integer.class, null),
		    new NumberDeserializers.CharacterDeserializer(Character.class, null),
		    new NumberDeserializers.LongDeserializer(Long.class, null),
		    new NumberDeserializers.FloatDeserializer(Float.class, null),
		    new NumberDeserializers.DoubleDeserializer(Double.class, null),
		    new NumberDeserializers.NumberDeserializer(),
		    new NumberDeserializers.BigDecimalDeserializer(),
		    new NumberDeserializers.BigIntegerDeserializer()
		    //new StdDeserializer.CalendarDeserializer()
		}));
		ObjectMapper mapper = factory.createObjectMapper();
		//ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
		mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

		mapper.setSerializationInclusion(Include.NON_NULL);
		try {
			M result = mapper.readValue(json, structure);
			return result;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
    public static class JacksonNonBlockingObjectMapperFactory {

        /**
         * Deserializer that won't block if value parsing doesn't match with target type
         * @param <T> Handled type
         */
        private static class NonBlockingDeserializer<T> extends JsonDeserializer<T> {
            private StdDeserializer<T> delegate;

            public NonBlockingDeserializer(StdDeserializer<T> _delegate){
                this.delegate = _delegate;
            }

            @Override
            public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                try {
                    return delegate.deserialize(jp, ctxt);
                }catch (JsonMappingException e){
                    // If a JSONMappingException occurs, simply returning null instead of blocking things
                    return null;
                }
            }
        }

        @SuppressWarnings("rawtypes")
		private List<StdDeserializer> jsonDeserializers = new ArrayList<StdDeserializer>();

        @SuppressWarnings({ "unchecked", "rawtypes"})
		public ObjectMapper createObjectMapper(){
            ObjectMapper objectMapper = new ObjectMapper();

            SimpleModule customJacksonModule = new SimpleModule("customJacksonModule", new Version(0, 0, 0, null, null, null));
            //SimpleModule customJacksonModule = new SimpleModule("customJacksonModule", new Version(1, 0, 0, null));
            for(StdDeserializer jsonDeserializer : jsonDeserializers){
                // Wrapping given deserializers with NonBlockingDeserializer
                customJacksonModule.addDeserializer(jsonDeserializer.handledType(), new NonBlockingDeserializer(jsonDeserializer));
            }

            objectMapper.registerModule(customJacksonModule);
            return objectMapper;
        }

        @SuppressWarnings("rawtypes")
		public JacksonNonBlockingObjectMapperFactory setJsonDeserializers(List<StdDeserializer> _jsonDeserializers){
            this.jsonDeserializers = _jsonDeserializers;
            return this;
        }
    }

}
