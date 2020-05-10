package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.Task;
import jobshop.encodings.ResourceOrder;

import java.util.ArrayList;
import java.util.List;

public class GreedySolver implements Solver{

    //for each job, indicate how many tasks are scheduled
    private int[] achievments;

    public GreedySolver()
    {
    }

    private boolean finished(int numTask)
    {
        for (int i = 0; i < achievments.length; i++)
        {
            if (achievments[i] < numTask)
            {
                return false;
            }
        }
        return true;
    }

    private void reinit()
    {
        for (int i = 0 ; i < achievments.length ; i++)
        {
            achievments[i] = 0;
        }
    }

    public Result solve(Instance instance, long deadline)
    {
        return solveSPT(instance, deadline);
    }

    private int getRemainingTime(Instance instance, int job)
    {
        int res = 0;
        for(int i = achievments[job] ; i <= instance.numTasks ; i++)
        {
            res += instance.duration(job, i);
        }
        return res;
    }

    private int getLRPT(Instance instance)
    {
        int max = -1;
        int res = -1;
        for (int i = 1 ; i <= achievments.length ; i++)
        {
            if(getRemainingTime(instance, i) > max)
            {
                max = getRemainingTime(instance, i);
                res = i;
            }
        }
        return res;
    }

    /////////////////////////////////////////////////////// Recherche gloutonne non améliorée ///////////////////////////////////////////////////////////////////////////////

    public Result solveSPT(Instance instance, long deadline)
    {
        ResourceOrder sol = new ResourceOrder(instance);
        achievments = new int[instance.numJobs];
        reinit();
        Task t;
        do
        {
            t = findShortest_EST(instance);
            int m = instance.machine(t);
            sol.tasksByMachine[m][sol.nextFreeSlot[m]] = t;
            sol.nextFreeSlot[m]++;
            achievments[t.job]++;
        }
        while(!finished(instance.numTasks));
        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }

    private Task findShortest_EST(Instance instance)
    {

        int shortest = Integer.MAX_VALUE;
        Task res = new Task(-1,-1);
        //On lance findEarliestTask et la boucle suivante est éffectuée sur la liste retournée
        for (int i = 0 ; i < instance.numJobs ; i++)
        {
            if (achievments[i] < instance.numTasks && instance.duration(i,achievments[i]) < shortest)
            {
                shortest = instance.duration(i,achievments[i]);
                res = new Task(i,achievments[i]);
            }
        }
        return res;
    }

    public Result solveLRPT(Instance instance, long deadline)
    {
        ResourceOrder sol = new ResourceOrder(instance);
        achievments = new int[instance.numJobs];
        reinit();
        Task t;
        int job;
        do
        {
            job = getLRPT(instance);
            t = new Task(job, achievments[job]);
            int m = instance.machine(t);
            sol.tasksByMachine[m][sol.nextFreeSlot[m]] = t;
            sol.nextFreeSlot[m]++;
            achievments[t.job]++;
        }
        while(t.job != -1);
        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////// Recherche gloutonne améliorée EST ////////////////////////////////////////////////////////////////////////////////


    private int findEarliestStartDate(Task t, ResourceOrder order)
    {
        Schedule sched = order.toSchedule();
        Instance inst = sched.pb;
        int machine = inst.machine(t);
        return Math.max(sched.endTime(new Task(t.job, t.task-1)),sched.endTime(order.tasksByMachine[machine][order.nextFreeSlot[machine]-1]));
    }

    private List<Task> findEarliestTasks(ResourceOrder order)
    {
        List<Task> res = new ArrayList<Task>();
        Schedule sched = order.toSchedule();
        int min = Integer.MAX_VALUE;
        for (int i  = 0 ; i < achievments.length ; i++)
        {
            if (findEarliestStartDate(new Task(i,achievments[i]), order) == min)
            {
                min = findEarliestStartDate(new Task(i,achievments[i]), order);
                res.add(new Task(i, achievments[i]));
            }
            else if (findEarliestStartDate(new Task(i,achievments[i]), order) < min)
            {
                min = findEarliestStartDate(new Task(i,achievments[i]), order);
                res.clear();
                res.add(new Task(i, achievments[i]));
            }
        }
        return res;
    }



}
