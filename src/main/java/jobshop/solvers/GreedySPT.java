package jobshop.solvers;

import jobshop.encodings.Task;

import java.util.List;

public class GreedySPT extends GreedySolver{

    @Override
    protected Task getOptimalTask(List<Task> schedulableTasks)
    {
        int minTime = Integer.MAX_VALUE;
        Task optim = new Task(0,0);
        for (Task task : schedulableTasks)
        {
            if (order.instance.duration(task) < minTime)
            {
                optim = task;
                minTime = order.instance.duration(task);
            }
        }
        return optim;
    }
}
