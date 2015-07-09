package core.utils.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import models.AbstractEntiry;
import org.bson.types.ObjectId;

import java.util.*;

/**
 * Created by zephyre on 1/20/15.
 */
public abstract class AbstractFormatter<T extends AbstractEntiry> {

    public String format(List<T> itemList) {
        try {
            return mapper.writeValueAsString(itemList);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public String format(T item) {
        try {
            return mapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public JsonNode formatNode(List<T> itemList) {
        return mapper.valueToTree(itemList);
    }

    public JsonNode formatNode(T item) {
        return mapper.valueToTree(item);
    }

    protected ObjectMapper mapper;

    protected Set<String> filteredFields = new HashSet<>();

    protected SimpleModule module = new SimpleModule();

    protected int imageWidth;


    public Set<String> getFilteredFields() {
        return filteredFields;
    }

    public <T2> void registerSerializer(Class<? extends T2> cls, JsonSerializer<T2> serializer) {
        module.addSerializer(cls, serializer);
    }

    protected ObjectMapper initObjectMapper(Map<String, PropertyFilter> filterMap) {
        mapper = new ObjectMapper();

        if (filterMap == null)
            filterMap = new HashMap<>();

        // 添加ObjectId的序列化
        registerSerializer(ObjectId.class, new ObjectIdSerializer());



        mapper.registerModule(module);

        // 添加Location的序列化
        SimpleFilterProvider filters = new SimpleFilterProvider();

        for (Map.Entry<String, PropertyFilter> entry : filterMap.entrySet())
            filters.addFilter(entry.getKey(), entry.getValue());

        mapper.setFilters(filters);

        return mapper;
    }
}
