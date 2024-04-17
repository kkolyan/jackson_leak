import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.IntStream;

public class JacksonLab {
    private static final Writer logWriter;

    static {
        try {
            String fileName = "current.log";
            new File(fileName).delete();
            logWriter = new FileWriter(fileName, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void log(Object s) {
        System.out.println(s);
        try {
            logWriter.append(s + "\n");
            logWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public static void main(String[] args) throws Exception {
        log("starting demo");
        ObjectMapper mapper = new ObjectMapper();
//        new ObjectMapper(new JsonFactory(new JsonFactoryBuilder().recyclerPool(JsonRecyclerPools.threadLocalPool())));

        RecyclerPoolSizeCalculator poolSizeCalculator = new RecyclerPoolSizeCalculator(mapper);

        for (int i = 0; i < 1000; i++) {
            startThread(mapper, i);
        }
        int[] depthes = new int[50];
        int index = 0;
        while (true) {
            Thread.sleep(20);
            depthes[index++] = poolSizeCalculator.calculateSize();
            if (index == depthes.length) {
                index = 0;
                Arrays.sort(depthes);
                IntSummaryStatistics summary = IntStream.of(depthes).summaryStatistics();
                String freeMem = (Runtime.getRuntime().freeMemory() / 1024) + "k";
                String totalMem = (Runtime.getRuntime().totalMemory() / 1024) + "k";
                log("depth: " + summary + ". pct10: " + depthes[1] + ", pct90: " + depthes[8] + ", free_memory: " + freeMem + ", total_memory: " + totalMem);
            }
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
            log("data generated for thread " + i);
            for (long j = 0; j < Long.MAX_VALUE; j++) {
                try {
                    JsonNode jsonNode = mapper.readTree(new SlowStringReader(data.get((int) (j % data.size()))));
                    String s = mapper.writeValueAsString(jsonNode);
                    x += s.length();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Thread.yield();
            }
            log(x);
        }).start();
    }

    private static class SlowStringReader extends StringReader {
        private boolean slowed;

        public SlowStringReader(String s) {
            super(s);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (!slowed) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                slowed = true;
            }
            return super.read(cbuf, off, len);
        }
    }
}
