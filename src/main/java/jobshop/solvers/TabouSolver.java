package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabouSolver implements Solver{

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

    static class orderSwap
    {
        ResourceOrder solution;
        Swap swap;

        public orderSwap(ResourceOrder sol, Swap s){
            solution = sol;
            swap = s;
        }
    }

    static class tabouSwap
    {
        Swap swap;
        int kOK;
        public tabouSwap(Swap swap, int kOK){
            this.swap = swap;
            this.kOK = kOK;
        }
    }

    List<orderSwap> orderSwaps = new ArrayList<orderSwap>();

    List<tabouSwap> tabous;

    protected void printList(String arg, List<ResourceOrder> tasks)
    {
        for(ResourceOrder order : tasks)
        {
            System.out.println(arg+":"+order+":"+arg);
        }
    }

    public Result solve(Instance instance, long deadline)
    {
        int maxIter = 500;
        int k = 0;
        int dureeTabou = 10;
        tabous = new ArrayList<tabouSwap>();

        Schedule startSolution = new GreedyLRPTEST().solve(instance, deadline).schedule;

        ResourceOrder currentSolution = new ResourceOrder(startSolution);
        List<Block> blocksOfCriticalPath;
        List<ResourceOrder> neighborhood;
        ResourceOrder bestNeighbor = currentSolution;
        Swap swap;
        long startTime = System.nanoTime();
        do
        {
            currentSolution = bestNeighbor;
           // System.out.println(currentSolution);
            blocksOfCriticalPath = blocksOfCriticalPath(currentSolution);

            orderSwaps = new ArrayList<orderSwap>();
            neighborhood = generateNeighborhood(currentSolution, blocksOfCriticalPath, k);
           // for(orderSwap os : orderSwaps)
            //{
             //   System.out.println("solution makespan "+os.solution.toSchedule().makespan()+", swap machine " + os.swap.machine+" "+os.swap.t1+" "+os.swap.t2);
            //}
            if(neighborhood.size() == 0)
            {
                break;
            }
            bestNeighbor = getMinMakeSpan(neighborhood);
           // System.out.println("Salut"+bestNeighbor);
            swap = findSwapByResourceOrder(bestNeighbor);
           // System.out.println("this swap is now tabou machine " + swap.machine+" "+swap.t1+" "+swap.t2);
            tabous.add(new tabouSwap(swap, k+dureeTabou));
           // for(tabouSwap ts : tabous)
            //{
              //  System.out.println("Swap machine " + ts.swap.machine+" "+ts.swap.t1+" "+ts.swap.t2+" available at "+ts.kOK);
            //}
            k++;
        }
        while(System.nanoTime() - startTime < 2000000000);
        return new Result(instance, currentSolution.toSchedule(), Result.ExitCause.Blocked);
    }

    ResourceOrder getMinMakeSpan(List<ResourceOrder> neighborhood)
    {
        int minSpan = Integer.MAX_VALUE;
        ResourceOrder bestOrder = null;
        for(ResourceOrder order : neighborhood)
        {
            if (order.toSchedule().makespan() < minSpan)
            {
                minSpan = order.toSchedule().makespan();
                bestOrder = order;
            }
        }
        return bestOrder;
    }

    int getTaskIndex(ResourceOrder order, int machine, Task task)
    {
        for(int i = 0 ; i < order.tasksByMachine[machine].length; i++)
        {
            if (order.tasksByMachine[machine][i].equals(task))
            {
                return i;
            }
        }
        return -1;
    }

    List<Block> blocksOfCriticalPath(ResourceOrder order)
    {
        int currentMachine;
        int currentMachineCount = 1;
        int currentFirstTaskIndex;

        Task currentFirstTask;

        Schedule sched = order.toSchedule();

        List<Block> res = new ArrayList<Block>();
        List<Task> criticalPath = sched.criticalPath();
       // for(Task task : criticalPath)
        //{
          //  System.out.println(task+" "+order.instance.machine(task));
       // }
        currentFirstTask = criticalPath.get(0);
        currentMachine = order.instance.machine(criticalPath.get(0));
        currentFirstTaskIndex = getTaskIndex(order, currentMachine, currentFirstTask);

        for(int i = 1; i<criticalPath.size(); i++)
        {
            if (order.instance.machine(criticalPath.get(i)) == currentMachine)
            {
                currentMachineCount++;
            }
            if(order.instance.machine(criticalPath.get(i)) != currentMachine)
            {
                if (currentMachineCount > 1) {
                    //System.out.println("adding block machine "+currentMachine+" "+currentFirstTaskIndex+" "+getTaskIndex(order, currentMachine, criticalPath.get(i - 1)));
                    res.add(new Block(currentMachine, currentFirstTaskIndex, getTaskIndex(order, currentMachine, criticalPath.get(i - 1))));
                }
                currentFirstTask = criticalPath.get(i);
                currentMachine = order.instance.machine(currentFirstTask);
                currentFirstTaskIndex = getTaskIndex(order, currentMachine, currentFirstTask);
                currentMachineCount = 1;
            }
            if(i == criticalPath.size() - 1)
            {
                if (currentMachineCount > 1) {
                    //System.out.println("adding block machine "+currentMachine+" "+currentFirstTaskIndex+" "+getTaskIndex(order, currentMachine, criticalPath.get(i - 1)));
                    res.add(new Block(currentMachine, currentFirstTaskIndex, getTaskIndex(order, currentMachine, criticalPath.get(i))));
                }
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
        }
        else
        {
            swaps.add(new Swap(block.machine, block.firstTask, block.firstTask + 1));
            swaps.add(new Swap(block.machine, block.lastTask, block.lastTask - 1));
        }
        return swaps;
    }

    List<ResourceOrder> generateNeighborhood(ResourceOrder order,List<Block> blocksOfCriticalPath, int iter)
    {
        List<ResourceOrder> neighborhood = new ArrayList<ResourceOrder>();
        List<Swap> allSwaps = new ArrayList<Swap>();
        for(Block block : blocksOfCriticalPath)
        {
            allSwaps.addAll(neighbors(block));
        }
        for(Swap swap : allSwaps)
        {
           // System.out.println("On est au swap machine " + swap.machine+" "+swap.t1+" "+swap.t2+" available at "+getIterOKBySwap(swap));
            if (getIterOKBySwap(swap) <= iter || getIterOKBySwap(swap) == -1)
            {
                ResourceOrder newNeighbor = order.copy();
                swap.applyOn(newNeighbor);
                neighborhood.add(newNeighbor);
                orderSwaps.add(new orderSwap(newNeighbor, swap));
            }
        }
        return neighborhood;
    }

    int getIterOKBySwap(Swap swap)
    {
        for(tabouSwap ts : tabous)
        {
            if (ts.swap.machine == swap.machine && ts.swap.t1 == swap.t1 && ts.swap.t2 == swap.t2)
            {
                return ts.kOK;
            }
        }
        return -1;
    }

    Swap findSwapByResourceOrder(ResourceOrder order)
    {
        for(orderSwap ords : orderSwaps)
        {
            if (ords.solution.equals(order))
            {
                return ords.swap;
            }
        }
        return null;
    }
}
