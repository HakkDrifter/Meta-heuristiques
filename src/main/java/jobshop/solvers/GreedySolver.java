package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.Task;
import jobshop.encodings.ResourceOrder;

import java.util.ArrayList;
import java.util.List;

public class GreedySolver implements Solver{

    protected ResourceOrder order;

    public GreedySolver(){}

    public Result solve(Instance instance, long deadline)
    {
        order = new ResourceOrder(instance);
        while (!order.allScheduled())
        {
            List<Task> schedulableTasks = order.getSchedulableTasks();
            //printList("Schedulable", schedulableTasks);
            Task optim = getOptimalTask(schedulableTasks);
            //System.out.println("optim:"+optim+":optim");
            int machine = instance.machine(optim);
            order.tasksByMachine[machine][order.nextFreeSlot[machine]] = optim;
            order.dateMachineFree[machine] = order.getDateSchedulable(optim)  + order.instance.duration(optim);
            order.dateEndLastTask[optim.job] = order.getDateSchedulable(optim) + order.instance.duration(optim);
            order.nextFreeSlotByJobs[optim.job]++;
            order.nextFreeSlot[machine]++;
        }
        return new Result(instance, order.toSchedule(), Result.ExitCause.Blocked);
    }

    protected Task getOptimalTask(List<Task> schedulableTasks)
    {
        return new Task(-1,-1);
    }

    protected void printList(String arg, List<Task> tasks)
    {
        for(Task task : tasks)
        {
            System.out.println(arg+":"+task+":"+arg);
        }
    }
}



