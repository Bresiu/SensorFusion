import com.google.common.eventbus.Subscribe;

public class SensorFusion {
    private Exporter exporter;

    public SensorFusion() {
        exporter = new Exporter();
        registerBus();
    }

    private void registerBus() {
        BusProvider.getInstance().register(this);
    }

    @Subscribe
    public void onSensorUpdate(SensorSingleData singleData) {
    }

    private void exportNewSensorData(SensorSingleData newSensorData) {
        exporter.writeData(newSensorData.toString());
    }
}
