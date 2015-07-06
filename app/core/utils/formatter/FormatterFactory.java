package core.utils.formatter;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.Map;

public class FormatterFactory {

    private static final FormatterFactory instance = new FormatterFactory();

    @SuppressWarnings("rawtypes")
    private Map<String, AbstractFormatter> mapHolder = new Hashtable<>();

    private FormatterFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractFormatter> T getInstance(Class<T> classOf, Object... args) {

        try {
            Class<?>[] signature = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                signature[i] = args[i].getClass();
            }
            Constructor constructor = classOf.getConstructor(signature);
            return (T) constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
