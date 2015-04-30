package models;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by topy on 2014/10/14.
 */
@Entity
public class Sequence extends AbstractEntity {

    @Transient
    public static String GROUPID = "GroupID";
    public String column;
    public Long count;

}
