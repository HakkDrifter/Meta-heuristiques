package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class ResourceOrder extends Encoding {

    // for each machine m, taskByMachine[m] is an array of tasks to be
    // executed on this machine in the same order
    public final Task[][] tasksByMachine;

    // for each machine, indicate on many tasks have been initialized
    public final int[] nextFreeSlot;

    public int[] nextFreeSlotByJobs;

    public int[] dateMachineFree;

    public int[] dateEndLastTask;

    /** Creates a new empty resource order. */
    public ResourceOrder(Instance instance)
    {
        super(instance);

        // matrix of null elements (null is the default value of objects)
        tasksByMachine = new Task[instance.numMachines][instance.numJobs];

        // no task scheduled on any machine (0 is the default value)
        nextFreeSlot = new int[instance.numMachines];

        nextFreeSlotByJobs = new int[instance.numJobs];

        dateMachineFree = new int[instance.numMachines];

        dateEndLastTask = new int[instance.numJobs];

    }

    public int getDateSchedulable(Task t)
    {
        return Math.max(dateMachineFree[instance.machine(t)], dateEndLastTask[t.job]);
    }

    /** Creates a resource order from a schedule. */
    public ResourceOrder(Schedule schedule)
    {
        super(schedule.pb);
        Instance pb = schedule.pb;

        this.tasksByMachine = new Task[pb.numMachines][];
        this.nextFreeSlot = new int[instance.numMachines];

        for(int m = 0 ; m<schedule.pb.numMachines ; m++) {
            final int machine = m;

            // for thi machine, find all tasks that are executed on it and sort them by their start time
            tasksByMachine[m] =
                    IntStream.range(0, pb.numJobs) // all job numbers
                            .mapToObj(j -> new Task(j, pb.task_with_machine(j, machine))) // all tasks on this machine (one per job)
                            .sorted(Comparator.comparing(t -> schedule.startTime(t.job, t.task))) // sorted by start time
                            .toArray(Task[]::new); // as new array and store in tasksByMachine

            // indicate that all tasks have been initialized for machine m
            nextFreeSlot[m] = instance.numJobs;
        }
    }

    public boolean allScheduled()
    {
        for (int machine = 0; machine < nextFreeSlot.length; machine++)
        {
            if (nextFreeSlot[machine] < instance.numJobs)
            {
                return false;
            }
        }
        return true;
    }

    private boolean isScheduled(Task t)
    {
        for (int machine = 0; machine < tasksByMachine.length; machine++)
        {
            for(int task = 0; task < tasksByMachine[machine].length; task++)
            {
                if (tasksByMachine[machine][task] != null && tasksByMachine[machine][task].equals(t))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Task> getSchedulableTasks()
    {
        List<Task> schedulableTasks = new ArrayList<Task>();
        for (int job = 0 ; job < instance.numJobs ; job++)
        {
            int task = 0;
            while(isScheduled(new Task(job,task)) && task < instance.numTasks-1)
            {
                task++;
            }
            if (nextFreeSlotByJobs[job] < instance.numTasks)
            {
                schedulableTasks.add(new Task(job,task));
            }
        }
        return schedulableTasks;
    }

    public List<Task> getEarliestSchedulableTasks(List<Task> schedulableTasks)
    {
        List<Task> earliestSchedulableTasks = new ArrayList<Task>();
        int earliestTime = Integer.MAX_VALUE;
        for(Task task : schedulableTasks)
        {
            int dateSchedulable = Math.max(dateMachineFree[instance.machine(task)], dateEndLastTask[task.job]);
            if (dateSchedulable < earliestTime)
            {
                earliestTime = dateSchedulable;
                earliestSchedulableTasks = new ArrayList<Task>();
                earliestSchedulableTasks.add(task);
            }
            else if (dateSchedulable == earliestTime)
            {
                earliestSchedulableTasks.add(task);
            }
        }
        return earliestSchedulableTasks;
    }

    @Override
    public Schedule toSchedule() {
        // indicate for each task that have been scheduled, its start time
        int [][] startTimes = new int [instance.numJobs][instance.numTasks];

        // for each job, how many tasks have been scheduled (0 initially)
        int[] nextToScheduleByJob = new int[instance.numJobs];

        // for each machine, how many tasks have been scheduled (0 initially)
        int[] nextToScheduleByMachine = new int[instance.numMachines];

        // for each machine, earliest time at which the machine can be used
        int[] releaseTimeOfMachine = new int[instance.numMachines];


        // loop while there remains a job that has unscheduled tasks
        while(IntStream.range(0, instance.numJobs).anyMatch(m -> nextToScheduleByJob[m] < instance.numTasks)) {

            // selects a task that has noun scheduled predecessor on its job and machine :
            //  - it is the next to be schedule on a machine
            //  - it is the next to be scheduled on its job
            // if there is no such task, we have cyclic dependency and the solution is invalid
            Optional<Task> schedulable =
                    IntStream.range(0, instance.numMachines) // all machines ...
                    .filter(m -> nextToScheduleByMachine[m] < instance.numJobs) // ... with unscheduled jobs
                    .mapToObj(m -> this.tasksByMachine[m][nextToScheduleByMachine[m]]) // tasks that are next to schedule on a machine ...
                    .filter(task -> task.task == nextToScheduleByJob[task.job])  // ... and on their job
                    .findFirst(); // select the first one if any

            if(schedulable.isPresent()) {
                // we found a schedulable task, lets call it t
                Task t = schedulable.get();
                int machine = instance.machine(t.job, t.task);

                // compute the earliest start time (est) of the task
                int est = t.task == 0 ? 0 : startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
                est = Math.max(est, releaseTimeOfMachine[instance.machine(t)]);
                startTimes[t.job][t.task] = est;

                // mark the task as scheduled
                nextToScheduleByJob[t.job]++;
                nextToScheduleByMachine[machine]++;
                // increase the release time of the machine
                releaseTimeOfMachine[machine] = est + instance.duration(t.job, t.task);
            } else {
                // no tasks are schedulable, there is no solution for this resource ordering
                return null;
            }
        }
        // we exited the loop : all tasks have been scheduled successfully
        return new Schedule(instance, startTimes);
    }

    /** Creates an exact copy of this resource order. */
    public ResourceOrder copy() {
        return new ResourceOrder(this.toSchedule());
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for(int m=0; m < instance.numMachines; m++)
        {
            s.append("Machine ").append(m).append(" : ");
            for(int j=0; j<instance.numJobs; j++)
            {
                s.append(tasksByMachine[m][j]).append(" ; ");
            }
            s.append("\n");
        }

        return s.toString();
    }

}