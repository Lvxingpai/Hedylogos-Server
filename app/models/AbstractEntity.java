package models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by zephyre on 4/23/15.
 */
@Entity
public class AbstractEntity {

    @Transient
    public static String FD_ID = "id";

    @Id
    protected ObjectId id;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
}
