import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MeanFilter {
    // The size of the mean filters rolling window.
    private int filterWindow = 30;

    private boolean dataInit;

    private ArrayList<LinkedList<Number>> dataLists;

    public MeanFilter() {
        dataLists = new ArrayList<LinkedList<Number>>();
        dataInit = false;
    }

    public float[] filterFloat(float[] data) {
        for (int i = 0; i < data.length; i++) {
            // Initialize the data structures for the data set.
            if (!dataInit) {
                dataLists.add(new LinkedList<Number>());
            }

            dataLists.get(i).addLast(data[i]);

            if (dataLists.get(i).size() > filterWindow) {
                dataLists.get(i).removeFirst();
            }
        }

        dataInit = true;

        float[] means = new float[dataLists.size()];

        for (int i = 0; i < dataLists.size(); i++) {
            means[i] = getMean(dataLists.get(i));
        }

        return means;
    }

    private float getMean(List<Number> data) {
        float m = 0;
        float count = 0;

        for (Number aData : data) {
            m += aData.floatValue();
            count++;
        }

        if (count != 0) {
            m = m / count;
        }

        return m;
    }

    public void setWindowSize(int size) {
        this.filterWindow = size;
    }
}
