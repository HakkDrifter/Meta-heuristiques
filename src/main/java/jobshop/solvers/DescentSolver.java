package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.Schedule;

import java.util.ArrayList;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
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



        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order)
        {
            Task aux = order.tasksByMachine[machine][t1];
            order.tasksByMachine[machine][t1] = order.tasksByMachine[machine][t2];
            order.tasksByMachine[machine][t2] = aux;
        }
    }

    public DescentSolver()
    {

    }

    @Override
    public Result solve(Instance instance, long deadline)
    {
        GreedySolver solver = new GreedySolver();
        Result Solution = solver.solve(instance, deadline);
        jobshop.Schedule newBestSolution = Solution.schedule;
        jobshop.Schedule currentBestSolution;
        ResourceOrder currentOrder;

        int minSpan = Integer.MAX_VALUE;
        Schedule currentBest = null;

        do
        {
            currentBestSolution = newBestSolution;
            currentOrder = new ResourceOrder(currentBestSolution);

            List<Block> blocks = blocksOfCriticalPath(currentOrder);
            List<Swap> swapList = new ArrayList<Swap>();
            List<Schedule> neighborhood = new ArrayList<Schedule>();

            for(Block bloc : blocks)
            {
                swapList.addAll(neighbors(bloc));
            }
            for(Swap swap: swapList)
            {
                ResourceOrder newNeighbor = new ResourceOrder(currentBestSolution);
                swap.applyOn(newNeighbor);
                neighborhood.add(newNeighbor.toSchedule());
            }
            for(Schedule sched : neighborhood)
            {
                if (sched.makespan() < minSpan)
                {
                    minSpan = sched.makespan();
                    currentBest = sched;
                }
            }
            newBestSolution = currentBest;
        }
        while(currentBestSolution.makespan() > newBestSolution.makespan());
        return new Result(instance, currentBestSolution, Result.ExitCause.Blocked);
    }



    int getTaskIndex(ResourceOrder ro, int machine, Task task)
    {
        for(int i = 0 ; i < ro.tasksByMachine[machine].length; i++)
        {
            if (ro.tasksByMachine[machine][i].equals(task))
            {
                return i;
            }
        }
        return -1;
    }

    /** Returns a list of all blocks of the critical path. */
    List<Block> blocksOfCriticalPath(ResourceOrder order)
    {
        int currentMachine = 0;
        int currentMachineCount = 0;
        int currentFirstTask = 0;

        List<Block> res = new ArrayList<Block>();

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
                    res.add(new Block(currentMachine, currentFirstTask, getTaskIndex(order, currentMachine, CriticalPath.get(i - 1))));
                    //System.out.println("Added block : " + currentMachine + " "+ currentFirstTask +" "+ getTaskIndex(order, currentMachine, CriticalPath.get(i - 1)));
                }
                currentMachine = sched.pb.machine(CriticalPath.get(i));
                currentFirstTask = getTaskIndex(order, currentMachine,CriticalPath.get(i));
                currentMachineCount = 1;
            }
        }
        return res;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block)
    {
        List<Swap> swaps = new ArrayList<Swap>();
        if (block.lastTask - block.firstTask + 1 == 2)
        {
            swaps.add(new Swap(block.machine, block.firstTask, block.lastTask));
            //System.out.println("Added : " + block.machine +" "+ block.firstTask +" " + block.lastTask);
        }
        else
        {
            swaps.add(new Swap(block.machine, block.firstTask, block.firstTask + 1));
            //System.out.println("Added : " + block.machine +" "+ block.firstTask +" " + (block.firstTask + 1));
            swaps.add(new Swap(block.machine, block.lastTask, block.lastTask -1 ));
            //System.out.println("Added : " + block.machine +" "+ block.lastTask +" " + (block.lastTask - 1 ));
        }
        return swaps;
    }

}
