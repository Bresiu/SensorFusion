import com.google.common.eventbus.EventBus;

import java.util.List;

public class SensorDataFactory {

    private List<String> sensorDataLines;
    private EventBus bus;

    private final long SLEEP_TIME = 1000;

    public SensorDataFactory() {
        Importer importer = new Importer();
        sensorDataLines = importer.readData();
        registerBus();
        startFactory();
    }

    private void registerBus() {
        bus = BusProvider.getInstance();
    }

    private void startFactory() {
        Thread thread = new Thread() {
            public void run() {
                for (String string : sensorDataLines) {
                    SensorSingleData sensorSingleData = proccessLine(string);
                    bus.post(sensorSingleData);

                    // Simulate GPS intervals
                    // pauseThread(SLEEP_TIME);
                }
            }
        };
        thread.start();
    }

    // Generation, Timestamp, AX, AY, AZ
    private SensorSingleData proccessLine(String sensorLine) {
        String[] sensorParts = sensorLine.split(" ");
        return new SensorSingleData(Integer.valueOf(sensorParts[0]), Long.valueOf(sensorParts[1]),
                Double.valueOf(sensorParts[2]), Double.valueOf(sensorParts[3]), Double.valueOf(sensorParts[4]));
    }

    private void pauseThread(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
