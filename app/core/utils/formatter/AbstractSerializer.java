package core.utils.formatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import models.AbstractEntity;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Serializer的基类
 * <p>
 * Created by zephyre on 1/15/15.
 */
public abstract class AbstractSerializer<T extends AbstractEntity> extends JsonSerializer<T> {
    protected String getString(String val) {
        return (val != null && !val.isEmpty()) ? val : "";
    }

    protected String getTimestamp(Date ts) {
        return (ts != null) ? dateFormat.format(ts) : "";
    }

    protected Double getValue(Double val) {
        return (val != null) ? val : 0d;
    }

    protected Long getValue(Long val) {
        return (val != null) ? val : 0L;
    }

    protected Integer getValue(Integer val) {
        return (val != null) ? val : null;
    }

    protected Boolean getValue(Boolean val) {
        return (val != null) ? val : false;
    }

    protected String getDate(Date val) {
        DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        return (val != null) ? dayFormat.format(val) : null;
    }

    /**
     * 写入ObjectId
     *
     * @param item
     * @param jgen
     * @param serializerProvider
     * @throws IOException
     */
    protected void writeObjectId(T item, JsonGenerator jgen, SerializerProvider serializerProvider)
            throws IOException {
        if (item instanceof AbstractEntity) {
            JsonSerializer<Object> ret = serializerProvider.findValueSerializer(ObjectId.class, null);
            if (ret != null) {
                jgen.writeFieldName("id");
                ret.serialize(((AbstractEntity) item).getId(), jgen, serializerProvider);
            }
        }
    }

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }


}
