import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.JsonRecyclerPools;
import com.fasterxml.jackson.core.util.RecyclerPool;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class RecyclerPoolSizeCalculator {
    private final Field headField;
    private final Field nextField;
    private final RecyclerPool<BufferRecycler> pool;

    public RecyclerPoolSizeCalculator(ObjectMapper mapper) {
        try {
            headField = RecyclerPool.LockFreePoolBase.class.getDeclaredField("head");
            pool = mapper.getJsonFactory()._getRecyclerPool();
            Class<?> nodeClass = Class.forName("com.fasterxml.jackson.core.util.RecyclerPool$LockFreePoolBase$Node");
            nextField = nodeClass.getDeclaredField("next");
            Field valueField = nodeClass.getDeclaredField("value");
            setAccessible(headField, nextField, valueField);
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setAccessible(Field... fields) {
        for (Field field : fields) {
            field.setAccessible(true);
        }
    }

    public int calculateSize() {
        if (true) {
            return pool.size();
        }
        try {
            int depth = 0;
            AtomicReference head = (AtomicReference) headField.get(pool);
            Object next = head.get();
            while (next != null) {
                depth++;
                next = nextField.get(next);
            }
            return depth;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
