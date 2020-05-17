package jobshop.solvers;

import jobshop.encodings.Task;

import java.util.List;

public class GreedyLRPT extends GreedySolver{

    @Override
    protected Task getOptimalTask(List<Task> schedulableTasks)
    {
        int minTime = -1;
        int longestJob = 0;
        for (int job = 0; job<order.instance.numJobs; job++)
        {
            if(jobPresent(job,schedulableTasks))
            {
                int timeRemaining = 0;
                for(int task = order.nextFreeSlotByJobs[job]; task < order.instance.numTasks; task++)
                {
                    timeRemaining += order.instance.duration(job,task);
                }
                //System.out.println("Il reste "+timeRemaining+" pour le job "+job);
                if (timeRemaining > minTime)
                {
                    longestJob = job;
                    minTime = timeRemaining;
                }
            }
        }

        for(Task task : schedulableTasks)
        {
            if (task.job == longestJob)
            {
                return task;
            }
        }
        System.out.println("pb dans greedyLRPT");
        return new Task(-1,-1);
    }

    private boolean jobPresent(int job, List<Task> schedulableTasks)
    {
        for(Task task : schedulableTasks)
        {
            if (job == task.job)
            {
                return true;
            }
        }
        return false;
    }
}
