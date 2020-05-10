package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.DescentSolver;
import jobshop.Solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabouSolver implements Solver{

    /*static class Block {
        *//** machine on which the block is identified *//*
        final int machine;
        *//** index of the first task of the block *//*
        final int firstTask;
        *//** index of the last task of the block *//*
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        *//** Apply this swap on the given resource order, transforming it into a new solution. *//*
        public void applyOn(ResourceOrder order)
        {
            Task aux = order.tasksByMachine[machine][t1];
            order.tasksByMachine[machine][t1] = order.tasksByMachine[machine][t2];
            order.tasksByMachine[machine][t2] = aux;
        }
    }*/

    static class SchedSwap
    {
        Schedule solution;
        DescentSolver.Swap swap;

        public SchedSwap(Schedule sol, DescentSolver.Swap s){
            solution = sol;
            swap = s;
        }
    }

    int[][] tabous;

    public TabouSolver()
    {

    }

    public void initMatrix()
    {
        for (int i = 0 ; i < tabous.length; i++)
        {
            Arrays.fill(tabous[i], 0);
        }
    }



    public Result solve(Instance instance, long deadline)
    {
        int maxIter = 20;
        int k = 0;
        int dureeTabou = 5;
        tabous = new int[instance.numTasks][instance.numTasks];
        initMatrix();

        GreedySolver solver = new GreedySolver();
        Result Solution = solver.solve(instance, deadline);
        jobshop.Schedule newBestSolution = Solution.schedule;
        jobshop.Schedule currentBestSolution;
        ResourceOrder currentOrder;

        int minSpan = Integer.MAX_VALUE;
        Schedule bestInNeighbors = null;

        do
        {
            currentBestSolution = newBestSolution;
            currentOrder = new ResourceOrder(currentBestSolution);

            List<DescentSolver.Block> blocks = blocksOfCriticalPath(currentOrder);
            List<DescentSolver.Swap> swapList = new ArrayList<DescentSolver.Swap>();
            List<SchedSwap> neighborhood = new ArrayList<SchedSwap>();

            for(DescentSolver.Block bloc : blocks)
            {
                swapList.addAll(neighbors(bloc));
            }
            for(DescentSolver.Swap swap: swapList)
            {
                ResourceOrder newNeighbor = new ResourceOrder(currentBestSolution);
                swap.applyOn(newNeighbor);
                neighborhood.add(new SchedSwap(newNeighbor.toSchedule(), swap));
            }
            for(SchedSwap sol : neighborhood)
            {
                if (sol.solution.makespan() < minSpan && tabous[sol.swap.t1][sol.swap.t2] <= k)
                {
                    minSpan = sol.solution.makespan();
                    bestInNeighbors = sol.solution;
                    tabous[sol.swap.t2][sol.swap.t2] = k + dureeTabou;
                }
            }
            newBestSolution = bestInNeighbors;
            k++;
        }
        while(k < maxIter);
        return new Result(instance, currentBestSolution, Result.ExitCause.Blocked);
    }

    int getTaskIndex(ResourceOrder ro, int machine, Task task)
    {
        for(int i = 0 ; i < ro.tasksByMachine[machine].length; i++)
        {
            if (ro.tasksByMachine[machine][i] == task)
            {
                return i;
            }
        }
        return -1;
    }

    /** Returns a list of all blocks of the critical path. */
    List<DescentSolver.Block> blocksOfCriticalPath(ResourceOrder order)
    {
        int currentMachine;
        int currentMachineCount = 0;
        int currentFirstTask;

        List<DescentSolver.Block> res = new ArrayList<DescentSolver.Block>();

        Schedule sched = order.toSchedule();
        List<Task> CriticalPath = sched.criticalPath();

        currentMachine = sched.pb.machine(CriticalPath.get(0));
        currentMachineCount = 1;
        currentFirstTask = getTaskIndex(order, currentMachine, CriticalPath.get(0));
        for(int i = 1; i < CriticalPath.size(); i++)
        {
            if (sched.pb.machine(CriticalPath.get(i)) == currentMachine){
                currentMachineCount++;
            }
            else
            {
                if (currentMachineCount > 1)
                {
                    res.add(new DescentSolver.Block(currentMachine, currentFirstTask, getTaskIndex(order, currentMachine, CriticalPath.get(i - 1))));
                }
                currentMachine = sched.pb.machine(CriticalPath.get(i));
                currentFirstTask = getTaskIndex(order, currentMachine,CriticalPath.get(i));
                currentMachineCount = 1;
            }
        }
        return res;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<DescentSolver.Swap> neighbors(DescentSolver.Block block)
    {
        List<DescentSolver.Swap> swaps = new ArrayList<DescentSolver.Swap>();
        if (block.lastTask - block.firstTask + 1 == 2)
        {
            swaps.add(new DescentSolver.Swap(block.machine, block.firstTask, block.lastTask));
        }
        else
        {
            swaps.add(new DescentSolver.Swap(block.machine, block.firstTask, block.firstTask + 1));
            swaps.add(new DescentSolver.Swap(block.machine, block.lastTask, block.lastTask + 1));
        }
        return swaps;
    }
}
