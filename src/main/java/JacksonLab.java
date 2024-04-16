import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.JsonRecyclerPools;
import com.fasterxml.jackson.core.util.RecyclerPool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class JacksonLab {
    public static void main(String[] args) throws Exception {
        System.out.println("starting demo");
        ObjectMapper mapper = new ObjectMapper();

//        Field headField = RecyclerPool.LockFreePoolBase.class.getDeclaredField("head");
        JsonRecyclerPools.LockFreePool pool = (JsonRecyclerPools.LockFreePool) mapper.getJsonFactory()._getRecyclerPool();
//        Class<?> nodeClass = Class.forName("com.fasterxml.jackson.core.util.RecyclerPool$LockFreePoolBase$Node");
//        Field nextField = nodeClass.getDeclaredField("next");
//        Field valueField = nodeClass.getDeclaredField("value");
//        setAccessible(headField, nextField, valueField);

        for (int i = 0; i < 1000; i++) {
            startThread(mapper, i);
        }
        int[] depthes = new int[10];
        int index = 0;
        while (true) {
            Thread.sleep(100);
            int depth = pool.size();
//            AtomicReference head = (AtomicReference) headField.get(pool);
//            Object next = head.get();
//            while (next != null && depth < 1000) {
//                depth++;
//                next = nextField.get(next);
//            }
            depthes[index++] = depth;
            if (index == depthes.length) {
                index = 0;
                Arrays.sort(depthes);
                String summary = IntStream.of(depthes).summaryStatistics().toString().replace("IntSummaryStatistics", "");
                System.out.println("depth: " + summary + ". pct10: " + depthes[1] + ", pct90: " + depthes[8] + ", free_memory: " + (Runtime.getRuntime().freeMemory() / 1024) + "k, total_memory: " + (Runtime.getRuntime().totalMemory() / 1024) + "k");
//                PushMetricsToMyGrafana.sendMetric("depth", "", summary.getAverage());
//                PushMetricsToMyGrafana.sendMetric("jvm_runtime_free_memory", "host=my_mac,app=JacksonLab", Runtime.getRuntime().freeMemory());
            }
        }
    }

    private static void setAccessible(Field... fields) {
        for (Field field : fields) {
            field.setAccessible(true);
        }
    }

    private static void startThread(ObjectMapper mapper, int i) {
        new Thread(() -> {
            List<String> data = new ArrayList<>();
            int x = 0;
            for (int j = 0; j < 20; j++) {
                StringBuilder s = new StringBuilder("{");
                int n = 100;
                for (int k = 0; k < n; k++) {
                    s.append("\"x").append(k).append("\":").append((i * 1000_000_000 + j * n + k));
                    if (k < n - 1) {
                        s.append(",");
                    }
                }
                s.append("}");
                data.add(s.toString());
            }
            System.out.println("data generated for thread " + i);
            for (long j = 0; j < Long.MAX_VALUE; j++) {
                try {
                    JsonNode jsonNode = mapper.readTree(new SlowStringReader(data.get((int) (j % data.size()))));
                    String s = mapper.writeValueAsString(jsonNode);
                    x += s.length();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println(x);
        }).start();
    }

    private static class SlowStringReader extends StringReader {
        private boolean slowed;

        public SlowStringReader(String s) {
            super(s);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
//            if (!slowed) {
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                slowed = true;
//            }
            return super.read(cbuf, off, len);
        }
    }
}
