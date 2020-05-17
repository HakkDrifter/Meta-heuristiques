package jobshop.solvers;

import jobshop.encodings.Task;

import java.util.List;

public class GreedyLRPTEST extends GreedyLRPT{

    @Override
    protected Task getOptimalTask(List<Task> schedulableTasks)
    {
        return super.getOptimalTask(order.getEarliestSchedulableTasks(schedulableTasks));
    }
}
