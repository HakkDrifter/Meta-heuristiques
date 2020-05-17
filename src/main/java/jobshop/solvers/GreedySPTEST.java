package jobshop.solvers;

import jobshop.encodings.Task;

import java.util.List;

public class GreedySPTEST extends GreedySPT{

    @Override
    protected Task getOptimalTask(List<Task> schedulableTasks)
    {
        return super.getOptimalTask(order.getEarliestSchedulableTasks(schedulableTasks));
    }
}
