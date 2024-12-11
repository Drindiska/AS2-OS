package se.lnu.os.ht24.a2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import se.lnu.os.ht24.a2.provided.abstract_.Instruction;
import se.lnu.os.ht24.a2.provided.data.ProcessInterval;
import se.lnu.os.ht24.a2.provided.data.StrategyType;
import se.lnu.os.ht24.a2.provided.instructions.AllocationInstruction;
import se.lnu.os.ht24.a2.provided.instructions.CompactInstruction;
import se.lnu.os.ht24.a2.provided.instructions.DeallocationInstruction;
import se.lnu.os.ht24.a2.provided.interfaces.SimulationInstance;
import se.lnu.os.ht24.a2.required.MemoryImpl;
import se.lnu.os.ht24.a2.required.SimulationInstanceImpl;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTests {

    private static final DecimalFormat df = new DecimalFormat("0.00");

    @BeforeAll
    static void setup() {
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    }

    // This test should be the only one working before starting your implementation and might be used
    // to check if the JUnit is configured correctly
    @Test
    void dummyTest() {
        // Simulation initialization with Memory Size = 10 and Best Fit (Memory addresses go from 0 to 9)
        // The simulation contains no instructions
        SimulationInstance sim = new SimulationInstanceImpl(
                new ArrayDeque<>(),
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        // Execute all the instructions left (in this specific dummy example, it does nothing)
        sim.runAll();
        // Check that no Exceptions were thrown
        assertTrue(sim.getExceptions().isEmpty());
        // Check that the strategy that we are adopting in this Simulation Instance is indeed not Worst Fit
        Assertions.assertNotEquals(StrategyType.WORST_FIT, sim.getStrategyType());
        // Every provided and required class was made printable for your convenience
        // Feel free to adapt any .toString() override to your needs
        System.out.println(sim);
    }

    @Test
    void oneInstructionTest() {
        // The Instruction list has only a Compact Instruction
        Queue<Instruction> instr = new ArrayDeque<>();
        instr.add(new CompactInstruction());
        // Simulation initialization with Memory Size = 10 and Best Fit (Memory addresses go from 0 to 9)
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        // Before running anything, the Instruction list should still contain the Compact Instruction
        assertEquals(1, sim.getInstructions().size());
        assertInstanceOf(CompactInstruction.class, sim.getInstructions().peek());
        // Execute all the instructions left
        sim.runAll();
        // Ensure that the instruction was removed from the Instruction list after execution
        assertEquals(0, sim.getInstructions().size());
        assertNull(sim.getInstructions().peek());
        // Ensure that no exceptions were thrown
        // (Compact does not throw exceptions even when has no effect on the memory)
        assertEquals(0, sim.getExceptions().size());
    }

    @Test
    void twoInstructionsTest() {
        // The instruction list has two elements
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new DeallocationInstruction(100),
                new AllocationInstruction(1,5)
        ));
        // Simulation initialization with Memory Size = 10 and First Fit (Memory addresses go from 0 to 9)
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.FIRST_FIT);
        // Before running anything, the Instruction list should still contain both instructions
        assertEquals(2, sim.getInstructions().size());
        // Before running anything, the Instruction list should still have the Deallocation instruction at the top
        assertInstanceOf(DeallocationInstruction.class, sim.getInstructions().peek());
        // Check that the Deallocation instruction at the top targets the process with id=100
        assertEquals(100, ((DeallocationInstruction) Objects.requireNonNull(sim.getInstructions().peek())).getProcessId());
        // Run only the first instruction (the Deallocation)
        sim.run(1);
        // Check that now only one instruction is left to execute
        assertEquals(1, sim.getInstructions().size());
        // Check that the Deallocation just executed threw an Exception (memory did not contain a process with id=100)
        assertEquals(1, sim.getExceptions().size());
        // Check that the largest hole of free memory when the Exception was thrown was equal to 10 (i.e., the memory was empty)
        Assertions.assertEquals(10, sim.getExceptions().get(0).getAllocatableMemoryAtException());
        // Check that it really was a Deallocation instruction to cause that exception
        Assertions.assertEquals(DeallocationInstruction.class, sim.getExceptions().get(0).getInstructionType());
        // Check that the next instruction is an Allocation
        assertInstanceOf(AllocationInstruction.class, sim.getInstructions().peek());
        // Check that the next Allocation will try to create a process with id=1 of size 5
        assertEquals(1, ((AllocationInstruction) Objects.requireNonNull(sim.getInstructions().peek())).getProcessId());
        assertEquals(5, ((AllocationInstruction) Objects.requireNonNull(sim.getInstructions().peek())).getDimension());
        // Execute all the instructions left (only the Allocation in our case)
        sim.runAll();
        // Check that indeed all the instructions were executed (i.e., no more instructions are left)
        assertEquals(0, sim.getInstructions().size());
        assertNull(sim.getInstructions().peek());
        // Ensure that the memory does not contain any process with id=2, as it was never allocated
        assertFalse(sim.getMemory().containsProcess(2));
        // Check that the allocated process with id=1 in memory is indeed of size 5
        assertEquals(5, sim.getMemory().processSize(1));
        // Since we are using First Fit, we expect that the process was allocated between addresses 0 and 4
        assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(4, sim.getMemory().getProcessInterval(1).getHighAddress());
        // Check that the process with id=1 has no other processes around him
        // In our case, there is nothing before, as it is allocated at the beginning of the memory, and
        // there is nothing after, as there's no process allocated starting from address 5
        assertTrue(sim.getMemory().neighboringProcesses(1).isEmpty());
        // Check that all the free space left in the memory is contiguous, i.e. there is only one portion of
        // memory that is unallocated between addresses 5 and 9
        assertEquals(1, sim.getMemory().freeSlots().size());
        assertTrue(sim.getMemory().freeSlots().contains(new ProcessInterval(5, 9)));
        // Since all the free memory left is contiguous, there is no fragmentation (=0)
        // All the assertions involving fragmentation will round it to two decimal digits before the check
        // Standard rounding rules apply, check java.math.RoundingMode.HALF_UP if unsure.
        assertEquals("0.00", df.format(sim.getMemory().fragmentation()));
    }

    
    @Test
    void deleteInstructionTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,5),
                new DeallocationInstruction(1)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.WORST_FIT);
        assertEquals(2, sim.getInstructions().size());
        sim.run(1);
        assertEquals(1, sim.getInstructions().size());
        sim.run(1);
        assertEquals(0, sim.getInstructions().size());
        assertFalse(sim.getMemory().containsProcess(1));
    }

    @Test
    void compactInstructionTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,5),
                new AllocationInstruction(2,1),
                new AllocationInstruction(3,2),
                new DeallocationInstruction(2),
                new CompactInstruction()
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.FIRST_FIT);
        sim.run(3);
        assertEquals(2, sim.getInstructions().size());
        assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(4, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(5, sim.getMemory().getProcessInterval(2).getLowAddress());
        assertEquals(5, sim.getMemory().getProcessInterval(2).getHighAddress());
        assertEquals(6, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(7, sim.getMemory().getProcessInterval(3).getHighAddress());
        sim.run(2);
        assertEquals(5, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(6, sim.getMemory().getProcessInterval(3).getHighAddress());
    }

    @Test
    void noSpaceLeftTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,5),
                new AllocationInstruction(2,2),
                new AllocationInstruction(3,2),
                new DeallocationInstruction(2),
                new AllocationInstruction(4, 3)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.WORST_FIT);
        sim.runAll();
        assertTrue(sim.getMemory().containsProcess(1));
        assertTrue(sim.getMemory().containsProcess(3));
        assertFalse(sim.getMemory().containsProcess(4));
        assertFalse(sim.getMemory().containsProcess(2));
    }

    @Test
    void objectAlreadyExist() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,5),
                new AllocationInstruction(1,2)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.run(2);
        assertTrue(sim.getMemory().containsProcess(1));
        assertEquals(1, sim.getExceptions().size());
    }

    @Test
    void fragmentationemptymemory() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,5),
                new AllocationInstruction(2,5)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.WORST_FIT);
        assertEquals(0, sim.getMemory().fragmentation());
        sim.run(3);
        assertEquals(0, sim.getMemory().fragmentation());
    }

    @Test
    void freeslotstest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,2),
                new AllocationInstruction(2,2)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.run(3);
        Set<ProcessInterval> testFreeSlots = new HashSet<>();
        testFreeSlots.add(new ProcessInterval(4, 9));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    }

    @Test
    void freeslotstestWithInterval() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,2),
                new AllocationInstruction(2,4),
                new AllocationInstruction(3, 2),
                new DeallocationInstruction(2)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.runAll();
        Set<ProcessInterval> testFreeSlots = new HashSet<>();
        testFreeSlots.add(new ProcessInterval(8, 9));
        testFreeSlots.add(new ProcessInterval(2, 5));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    }

    @Test
    void neighborverification() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(5,2),
                new AllocationInstruction(2,4),
                new AllocationInstruction(7, 2)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.runAll();
        System.out.println(sim.getMemory().neighboringProcesses(2));
    }

    @Test
    void neighborverificationOneEmpty() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(5,2),
                new AllocationInstruction(2,4),
                new AllocationInstruction(6,1),
                new AllocationInstruction(7,3),
                new DeallocationInstruction(6),
                new DeallocationInstruction(5)

        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.run(5);
        System.out.println(sim.getMemory().neighboringProcesses(5));
        System.out.println(sim.getMemory().neighboringProcesses(2));
        System.out.println(sim.getMemory().neighboringProcesses(7));
        sim.run(1);
        System.out.println(sim.getMemory().neighboringProcesses(2));

    }

    

    @Test
    void ProcessIntervalTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,2),
                new AllocationInstruction(2,4),
                new AllocationInstruction(3, 2)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.runAll();
        assertEquals(5, sim.getMemory().getProcessInterval(2).getHighAddress());
        assertEquals(2, sim.getMemory().getProcessInterval(2).getLowAddress());
    }

    @Test
    void exeptionyTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new DeallocationInstruction(2),
                new AllocationInstruction(1,2),
                new AllocationInstruction(2,4),
                new AllocationInstruction(3, 2),
                new AllocationInstruction(1, 2),
                new AllocationInstruction(5, 5)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.runAll();
        assertEquals(3, sim.getExceptions().size());
        assertEquals(10, sim.getExceptions().get(0).getAllocatableMemoryAtException());
        assertEquals(2, sim.getExceptions().get(1).getAllocatableMemoryAtException());
        assertEquals(2, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    }

    @Test
    void worstFitTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,1),
                new AllocationInstruction(2,4),
                new AllocationInstruction(3, 1),
                new DeallocationInstruction(2),
                new AllocationInstruction(9, 2),
                new AllocationInstruction(10, 2)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.WORST_FIT);
        sim.runAll();
        assertEquals(2, sim.getMemory().getProcessInterval(9).getHighAddress());
        assertEquals(1, sim.getMemory().getProcessInterval(9).getLowAddress());
        assertEquals(7, sim.getMemory().getProcessInterval(10).getHighAddress());
        assertEquals(6, sim.getMemory().getProcessInterval(10).getLowAddress());
    }

    void firstFitTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,1),
                new AllocationInstruction(2,4),
                new AllocationInstruction(3, 1),
                new DeallocationInstruction(2),
                new AllocationInstruction(9, 2),
                new AllocationInstruction(10, 2)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.FIRST_FIT);
        sim.runAll();
        assertEquals(2, sim.getMemory().getProcessInterval(9).getHighAddress());
        assertEquals(1, sim.getMemory().getProcessInterval(9).getLowAddress());
        assertEquals(3, sim.getMemory().getProcessInterval(10).getHighAddress());
        assertEquals(4, sim.getMemory().getProcessInterval(10).getLowAddress());
    }

    @Test
    void bestFitTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(1,1),
                new AllocationInstruction(2,4),
                new AllocationInstruction(3, 3),
                new DeallocationInstruction(2),
                new AllocationInstruction(9, 2),
                new AllocationInstruction(10, 2)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.runAll();
        assertEquals(9, sim.getMemory().getProcessInterval(9).getHighAddress());
        assertEquals(8, sim.getMemory().getProcessInterval(9).getLowAddress());
        assertEquals(2, sim.getMemory().getProcessInterval(10).getHighAddress());
        assertEquals(1, sim.getMemory().getProcessInterval(10).getLowAddress());
    }

    /* 
    So this is the funny test for the assignment 2, they are devieded in 4 type :
    Test 1 is a simple test with several instruction, NO EXEPTION should be thrown by the simulator. We basicaly check the fragmentation, freeslots, neighbor, block size, block highest and smallest addresses.
    Test 2 is a more complexe where we work with different instruvtion that can have several exeption.
    Test 3 is basicaly to check if the simulator throw exeption as expected.
    Test 4 : well i don't kno< i didn't started it for now
    Anyway, each test category are performed 3 times for each different strategy so that we verify the strategy works fine.
    I'm pretty sur the test expected outcome are correct, if a test doesn't work, it's certainly YOUR code fault :) 
    I didn't commented everything because i'm kinda lazy sorry))
    */

//1) Allocate ID 100, size 1000; 2) Allocate ID 1, size 500; 3) Deallocate ID 100; 4) Allocate ID 2, size 200; 5) Compact Memory;
//6) Deallocate ID 2; 7) Allocate ID 3, size 500; 8) Deallocate ID 1; 9) Allocate ID 4, size 100; 10) Compact Memory;
    @Test
    void test1BestFit() {
        Set<ProcessInterval> testFreeSlots = new HashSet<>();
        Set<Integer> neighborSet = new HashSet<>();
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(100,1000), //one
                new AllocationInstruction(1,500), //two
                new DeallocationInstruction(100), //three
                new AllocationInstruction(2, 200), //four
                new CompactInstruction(), //five
                new DeallocationInstruction(2), //six
                new AllocationInstruction(3, 500), //seven
                new DeallocationInstruction(1), //eight
                new AllocationInstruction(4, 100), //nine
                new CompactInstruction() //ten
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(2000),
                StrategyType.BEST_FIT);
        // First test after 1 instruction
        sim.run(1);


        // Check intervals, Dimention
        assertEquals(999, sim.getMemory().getProcessInterval(100).getHighAddress());
        assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
        assertEquals(1000, sim.getMemory().processSize(100));
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));

        // Check fragmentation
        assertEquals(0 , sim.getMemory().fragmentation());

        testFreeSlots.add(new ProcessInterval(1000, 1999));

        // check free slots
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());

        sim.run(1); //two
        testFreeSlots.clear();

        testFreeSlots.add(new ProcessInterval(1500, 1999));
        neighborSet.add(100);
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
        assertEquals(999, sim.getMemory().getProcessInterval(100).getHighAddress());
        assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
        assertEquals(1499, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(1000, sim.getMemory().getProcessInterval(1).getLowAddress());

        sim.run(1); //three
        neighborSet.clear();
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
        
        assertEquals(0.33333333333333337 , sim.getMemory().fragmentation());
        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(0, 999));
        testFreeSlots.add(new ProcessInterval(1500, 1999));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());

        assertEquals(1499, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(1000, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(500, sim.getMemory().processSize(1));

        sim.run(1); //four

        neighborSet.add(2);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

        assertEquals(1500, sim.getMemory().getProcessInterval(2).getLowAddress());
        assertEquals(1699, sim.getMemory().getProcessInterval(2).getHighAddress());

        sim.run(1); //five

        neighborSet.clear();

        neighborSet.add(2);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

        assertEquals(499, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(500, sim.getMemory().getProcessInterval(2).getLowAddress());
        assertEquals(699, sim.getMemory().getProcessInterval(2).getHighAddress());
        assertEquals(0, sim.getMemory().fragmentation());
        
        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(700, 1999));

        sim.run(1); //six

        neighborSet.clear();

        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

        assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(499, sim.getMemory().getProcessInterval(1).getHighAddress());
        
        sim.run(1); //seven

        neighborSet.clear();
        neighborSet.add(3);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

        assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(499, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(500, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(999, sim.getMemory().getProcessInterval(3).getHighAddress());

        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(1000, 1999));

        assertEquals(0, sim.getMemory().fragmentation());

        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));

        sim.run(1); //eight

        assertEquals(500, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(999, sim.getMemory().getProcessInterval(3).getHighAddress());
        
        testFreeSlots.add(new ProcessInterval(0, 499));
        neighborSet.clear();
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));

        sim.run(1); //nein
        
        assertEquals(0, sim.getMemory().getProcessInterval(4).getLowAddress());
        assertEquals(99, sim.getMemory().getProcessInterval(4).getHighAddress());
        assertEquals(500, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(999, sim.getMemory().getProcessInterval(3).getHighAddress());
        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(100, 499));
        testFreeSlots.add(new ProcessInterval(1000, 1999));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(4));
        assertEquals(0.2857142857142857, sim.getMemory().fragmentation());

        sim.run(1); //ten
        
        assertEquals(0, sim.getMemory().getProcessInterval(4).getLowAddress());
        assertEquals(99, sim.getMemory().getProcessInterval(4).getHighAddress());
        assertEquals(100, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(599, sim.getMemory().getProcessInterval(3).getHighAddress());
        assertEquals(0, sim.getMemory().fragmentation());
        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(600, 1999));
        neighborSet.clear();
        neighborSet.add(4);
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
        neighborSet.clear();
        neighborSet.add(3);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(4)); 
        assertEquals(0, sim.getExceptions().size());
    }

    @Test
    void test1WorstFit() {
        Set<ProcessInterval> testFreeSlots = new HashSet<>();
        Set<Integer> neighborSet = new HashSet<>();
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new AllocationInstruction(100,1000),
                new AllocationInstruction(1,500),
                new DeallocationInstruction(100),
                new AllocationInstruction(2, 200),
                new CompactInstruction(),
                new DeallocationInstruction(2),
                new AllocationInstruction(3, 500),
                new DeallocationInstruction(1),
                new AllocationInstruction(4, 100),
                new CompactInstruction()
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(2000),
                StrategyType.WORST_FIT);
        // First test after 1 instruction
        sim.run(1); //one

        // Check intervals, Dimention
        assertEquals(999, sim.getMemory().getProcessInterval(100).getHighAddress());
        assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
        assertEquals(1000, sim.getMemory().processSize(100));

        // Check fragmentation
        assertEquals(0 , sim.getMemory().fragmentation());

        testFreeSlots.add(new ProcessInterval(1000, 1999));

        // check free slots
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());

        neighborSet.clear();
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));

        sim.run(1); //two
        testFreeSlots.clear();

        testFreeSlots.add(new ProcessInterval(1500, 1999));
        neighborSet.add(100);
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
        assertEquals(999, sim.getMemory().getProcessInterval(100).getHighAddress());
        assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
        assertEquals(1499, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(1000, sim.getMemory().getProcessInterval(1).getLowAddress());

        sim.run(1); //three
        neighborSet.clear();
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
        
        assertEquals(0.33333333333333337 , sim.getMemory().fragmentation());
        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(0, 999));
        testFreeSlots.add(new ProcessInterval(1500, 1999));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());

        assertEquals(1499, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(1000, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(500, sim.getMemory().processSize(1));

        sim.run(1); //four

        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

        assertEquals(0, sim.getMemory().getProcessInterval(2).getLowAddress());
        assertEquals(199, sim.getMemory().getProcessInterval(2).getHighAddress());

        sim.run(1); //five

        assertEquals(699, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(200, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(0, sim.getMemory().getProcessInterval(2).getLowAddress());
        assertEquals(199, sim.getMemory().getProcessInterval(2).getHighAddress());
        assertEquals(0, sim.getMemory().fragmentation());
        
        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(700, 1999));

        neighborSet.add(2);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

        sim.run(1); //six

        assertEquals(200, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(699, sim.getMemory().getProcessInterval(1).getHighAddress());
        
        neighborSet.clear();
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

        sim.run(1); //seven

        assertEquals(200, sim.getMemory().getProcessInterval(1).getLowAddress());
        assertEquals(699, sim.getMemory().getProcessInterval(1).getHighAddress());
        assertEquals(700, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(1199, sim.getMemory().getProcessInterval(3).getHighAddress());

        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(1200, 1999));

        assertEquals(0.19999999999999996, sim.getMemory().fragmentation());

        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));

        neighborSet.clear();
        neighborSet.add(3);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

        sim.run(1); //eight

        assertEquals(700, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(1199, sim.getMemory().getProcessInterval(3).getHighAddress());
        
        testFreeSlots.add(new ProcessInterval(0, 699));
        neighborSet.clear();
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));

        sim.run(1); //nein
        
        assertEquals(1200, sim.getMemory().getProcessInterval(4).getLowAddress());
        assertEquals(1299, sim.getMemory().getProcessInterval(4).getHighAddress());
        assertEquals(700, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(1199, sim.getMemory().getProcessInterval(3).getHighAddress());
        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(0, 699));
        testFreeSlots.add(new ProcessInterval(1300, 1999));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        neighborSet.add(4);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
        neighborSet.clear();
        neighborSet.add(3);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(4));
        assertEquals(0.5, sim.getMemory().fragmentation());

        sim.run(1); //ten
        
        assertEquals(500, sim.getMemory().getProcessInterval(4).getLowAddress());
        assertEquals(599, sim.getMemory().getProcessInterval(4).getHighAddress());
        assertEquals(0, sim.getMemory().getProcessInterval(3).getLowAddress());
        assertEquals(499, sim.getMemory().getProcessInterval(3).getHighAddress());
        assertEquals(0, sim.getMemory().fragmentation());
        testFreeSlots.clear();
        testFreeSlots.add(new ProcessInterval(600, 1999));
        neighborSet.clear();
        neighborSet.add(4);
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
        neighborSet.clear();
        neighborSet.add(3);
        assertEquals(neighborSet, sim.getMemory().neighboringProcesses(4)); 
        assertEquals(0, sim.getExceptions().size());
    }

@Test
void test1First_Fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,1000),
            new AllocationInstruction(1,500),
            new DeallocationInstruction(100),
            new AllocationInstruction(2, 200),
            new CompactInstruction(),
            new DeallocationInstruction(2),
            new AllocationInstruction(3, 500),
            new DeallocationInstruction(1),
            new AllocationInstruction(4, 100),
            new CompactInstruction()
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(2000),
            StrategyType.FIRST_FIT);
    // First test after 1 instruction
    sim.run(1); //one

    // Check intervals, Dimention
    assertEquals(999, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(1000, sim.getMemory().processSize(100));

    // Check fragmentation
    assertEquals(0 , sim.getMemory().fragmentation());

    testFreeSlots.add(new ProcessInterval(1000, 1999));

    // check free slots
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));

    sim.run(1); //two

    testFreeSlots.clear();

    testFreeSlots.add(new ProcessInterval(1500, 1999));
    neighborSet.add(100);
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    neighborSet.clear();
    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
    assertEquals(999, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(1499, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(1000, sim.getMemory().getProcessInterval(1).getLowAddress());

    sim.run(1); //three
    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    
    assertEquals(0.33333333333333337 , sim.getMemory().fragmentation());
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 999));
    testFreeSlots.add(new ProcessInterval(1500, 1999));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    assertEquals(1499, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(1000, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(500, sim.getMemory().processSize(1));

    sim.run(1); //four

    
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

    assertEquals(0, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(2).getHighAddress());

    sim.run(1); //five

    assertEquals(699, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(0, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(0, sim.getMemory().fragmentation());
    
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(700, 1999));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    neighborSet.clear();
    neighborSet.add(2);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

    sim.run(1); //six

    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

    assertEquals(200, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(699, sim.getMemory().getProcessInterval(1).getHighAddress());
    
    sim.run(1); //seven


    assertEquals(200, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(699, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(700, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(1199, sim.getMemory().getProcessInterval(3).getHighAddress());

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(1200, 1999));
    testFreeSlots.add(new ProcessInterval(0, 199));

    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
    neighborSet.clear();
    neighborSet.add(3);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));

    assertEquals(0.19999999999999996, sim.getMemory().fragmentation());

    neighborSet.clear();
    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));

    sim.run(1); //eight

    assertEquals(700, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(1199, sim.getMemory().getProcessInterval(3).getHighAddress());
    
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(1200, 1999));
    testFreeSlots.add(new ProcessInterval(0, 699));
    neighborSet.clear();
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));

    sim.run(1); //nein
    
    assertEquals(700, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(1199, sim.getMemory().getProcessInterval(3).getHighAddress());
    assertEquals(0, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(4).getHighAddress());
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(100, 699));
    testFreeSlots.add(new ProcessInterval(1200, 1999));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(4));
    assertEquals(0.4285714285714286, sim.getMemory().fragmentation());

    sim.run(1); //ten
    
    assertEquals(0, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(4).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(599, sim.getMemory().getProcessInterval(3).getHighAddress());
    assertEquals(0, sim.getMemory().fragmentation());
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(600, 1999));
    neighborSet.clear();
    neighborSet.add(4);
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
    neighborSet.clear();
    neighborSet.add(3);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(4)); 
    assertEquals(0, sim.getExceptions().size());
    }

//1) Allocate ID 100, size 600; 2) Allocate ID 1, size 200; 3) Allocate ID 2, size 200; 4) Deallocate ID 1; 5) Deallocate ID 10;
//6) Allocate ID 3, size 100; 7) Allocate ID 4, size 100; 8) Compact Memory; 9) Deallocate ID 3; 10) Allocate ID 5, size 200;
//11) Deallocate ID 5; 12) Deallocate ID 4; 13) Allocate ID 6, size 200;
@Test
void test2best_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,600),
            new AllocationInstruction(1,200),
            new AllocationInstruction(2, 200),
            new DeallocationInstruction(1),
            new DeallocationInstruction(10),
            new AllocationInstruction(3, 100),
            new AllocationInstruction(4, 100),
            new CompactInstruction(),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 200),
            new DeallocationInstruction(5),
            new DeallocationInstruction(4),
            new AllocationInstruction(6, 200)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(500),
            StrategyType.BEST_FIT);
    List<Integer> listIds = new ArrayList<Integer>();
    assertEquals(listIds, sim.getMemory().processes());
    sim.run(1);
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(0, sim.getMemory().processSize(100));
    sim.run(1);
    assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(1).getHighAddress());
    testFreeSlots.add(new ProcessInterval(200, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0, sim.getMemory().fragmentation());
    sim.run(1);
    assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(0, sim.getMemory().fragmentation());
    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    neighborSet.clear();
    neighborSet.add(2);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    sim.run(1);
    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(false, sim.getMemory().containsProcess(1));

    sim.run(1);
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(2, sim.getExceptions().size());

    sim.run(1);
    assertEquals(400, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(499, sim.getMemory().getProcessInterval(3).getHighAddress());

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 199));

    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    
    sim.run(1);

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(100, 199));

    assertEquals(400, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(499, sim.getMemory().getProcessInterval(3).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(0, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(4).getHighAddress());

    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1);

    neighborSet.clear();
    neighborSet.add(4);
    neighborSet.add(3);

    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(2, sim.getExceptions().size());

    assertEquals(300, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(3).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(299, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(0, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(4).getHighAddress());

    sim.run(2);

    testFreeSlots.clear();

    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    neighborSet.clear();
    neighborSet.add(4);
    neighborSet.add(5);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());

    sim.run(2);

    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 99));
    testFreeSlots.add(new ProcessInterval(300, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    sim.run(1);
    
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 99));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(299, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(300, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(499, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(2, sim.getExceptions().size());
    }

//1) Allocate ID 100, size 600; 2) Allocate ID 1, size 200; 3) Allocate ID 2, size 200; 4) Deallocate ID 1; 5) Deallocate ID 10;
//6) Allocate ID 3, size 100; 7) Allocate ID 4, size 100; 8) Compact Memory; 9) Deallocate ID 3; 10) Allocate ID 5, size 200;
//11) Deallocate ID 5; 12) Deallocate ID 4; 13) Allocate ID 6, size 200;
@Test
void test2worst_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,600),
            new AllocationInstruction(1,200),
            new AllocationInstruction(2, 200),
            new DeallocationInstruction(1),
            new DeallocationInstruction(10),
            new AllocationInstruction(3, 100),
            new AllocationInstruction(4, 100),
            new CompactInstruction(),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 200),
            new DeallocationInstruction(5),
            new DeallocationInstruction(4),
            new AllocationInstruction(6, 200)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(500),
            StrategyType.WORST_FIT);
    List<Integer> listIds = new ArrayList<Integer>();
    assertEquals(listIds, sim.getMemory().processes());

    sim.run(1); //First
    testFreeSlots.add(new ProcessInterval(0, 499));

    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1); //Second

    testFreeSlots.clear();
    assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(1).getHighAddress());
    testFreeSlots.add(new ProcessInterval(200, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1); //third

    assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(0, sim.getMemory().fragmentation());
    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    neighborSet.clear();
    neighborSet.add(2);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1); // fourth

    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(false, sim.getMemory().containsProcess(1));
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(1, sim.getExceptions().size());
    

    sim.run(1);//fith

    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(2, sim.getExceptions().size());

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 199));
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(0.33333333333333337, sim.getMemory().fragmentation());
    

    sim.run(1); //six

    assertEquals(0, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(3).getHighAddress());

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(100, 199));
    testFreeSlots.add(new ProcessInterval(400, 499));
    neighborSet.clear();
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

    sim.run(1); //seven

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(400, 499));

    assertEquals(0, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(3).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(4).getHighAddress());

    assertEquals(0, sim.getMemory().fragmentation());

    neighborSet.clear();
    neighborSet.add(3);
    neighborSet.add(2);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(4));

    sim.run(1); //eight

    neighborSet.clear();
    neighborSet.add(4);

    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(2, sim.getExceptions().size());

    assertEquals(0, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(3).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(4).getHighAddress());

    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1); //nine
    assertEquals(2, sim.getExceptions().size());

    assertEquals(false, sim.getMemory().containsProcess(3));
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(4).getHighAddress());
    neighborSet.clear();
    neighborSet.add(4);

    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

    sim.run(1); //Ten
    assertEquals(3, sim.getExceptions().size());
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 99));
    testFreeSlots.add(new ProcessInterval(400, 499));

    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    neighborSet.clear();
    neighborSet.add(4);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0.5, sim.getMemory().fragmentation());

    sim.run(1); //eleven

    assertEquals(4, sim.getExceptions().size());
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(3).getAllocatableMemoryAtException());

    

    sim.run(1); //twelve

    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 199));
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(4, sim.getExceptions().size());
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(3).getAllocatableMemoryAtException());
    sim.run(1); //treize
    
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(0, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(4, sim.getExceptions().size());
    }




// VISIBLY THE FIRST_FIT AND WORST_FIT STRATEGY IN THE TEST 2 HAVE THE SAME OUTCOME))

//1) Allocate ID 100, size 600; 2) Allocate ID 1, size 200; 3) Allocate ID 2, size 200; 4) Deallocate ID 1; 5) Deallocate ID 10;
//6) Allocate ID 3, size 100; 7) Allocate ID 4, size 100; 8) Compact Memory; 9) Deallocate ID 3; 10) Allocate ID 5, size 200;
//11) Deallocate ID 5; 12) Deallocate ID 4; 13) Allocate ID 6, size 200;
@Test
void test2wfirst_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,600),
            new AllocationInstruction(1,200),
            new AllocationInstruction(2, 200),
            new DeallocationInstruction(1),
            new DeallocationInstruction(10),
            new AllocationInstruction(3, 100),
            new AllocationInstruction(4, 100),
            new CompactInstruction(),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 200),
            new DeallocationInstruction(5),
            new DeallocationInstruction(4),
            new AllocationInstruction(6, 200)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(500),
            StrategyType.FIRST_FIT);
    List<Integer> listIds = new ArrayList<Integer>();
    assertEquals(listIds, sim.getMemory().processes());

    sim.run(1); //First
    testFreeSlots.add(new ProcessInterval(0, 499));

    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1); //Second

    testFreeSlots.clear();
    assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(1).getHighAddress());
    testFreeSlots.add(new ProcessInterval(200, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1); //third

    assertEquals(0, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(0, sim.getMemory().fragmentation());
    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    neighborSet.clear();
    neighborSet.add(2);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1); // fourth

    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(false, sim.getMemory().containsProcess(1));
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(1, sim.getExceptions().size());
    

    sim.run(1);//fith

    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(2, sim.getExceptions().size());

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 199));
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(0.33333333333333337, sim.getMemory().fragmentation());
    

    sim.run(1); //six

    assertEquals(0, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(3).getHighAddress());

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(100, 199));
    testFreeSlots.add(new ProcessInterval(400, 499));
    neighborSet.clear();
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(3));
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

    sim.run(1); //seven

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(400, 499));

    assertEquals(0, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(3).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(4).getHighAddress());

    assertEquals(0, sim.getMemory().fragmentation());

    neighborSet.clear();
    neighborSet.add(3);
    neighborSet.add(2);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(4));

    sim.run(1); //eight

    neighborSet.clear();
    neighborSet.add(4);

    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(2, sim.getExceptions().size());

    assertEquals(0, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(3).getHighAddress());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(4).getHighAddress());

    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1); //nine
    assertEquals(2, sim.getExceptions().size());

    assertEquals(false, sim.getMemory().containsProcess(3));
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(4).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(4).getHighAddress());
    neighborSet.clear();
    neighborSet.add(4);

    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

    sim.run(1); //Ten
    assertEquals(3, sim.getExceptions().size());
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 99));
    testFreeSlots.add(new ProcessInterval(400, 499));

    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    neighborSet.clear();
    neighborSet.add(4);
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0.5, sim.getMemory().fragmentation());

    sim.run(1); //eleven

    assertEquals(4, sim.getExceptions().size());
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(3).getAllocatableMemoryAtException());

    

    sim.run(1); //twelve

    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));

    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(0, 199));
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(4, sim.getExceptions().size());
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(100, sim.getExceptions().get(3).getAllocatableMemoryAtException());
    sim.run(1); //treize
    
    testFreeSlots.clear();
    testFreeSlots.add(new ProcessInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(200, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(0, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(4, sim.getExceptions().size());
    }

    @Test
void test3first_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,600),
            new AllocationInstruction(1,200),
            new AllocationInstruction(2, 200),
            new DeallocationInstruction(1),
            new DeallocationInstruction(10),
            new AllocationInstruction(3, 100),
            new AllocationInstruction(4, 100),
            new CompactInstruction(),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 200),
            new DeallocationInstruction(5),
            new DeallocationInstruction(4),
            new AllocationInstruction(6, 200)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(50),
            StrategyType.FIRST_FIT); 
    
    assertEquals(0, sim.getExceptions().size());
    assertEquals(0, sim.getMemory().fragmentation());
    sim.runAll(); 
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(12, sim.getExceptions().size());
    assertEquals(50, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(3).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(4).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(5).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(6).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(7).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(8).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(9).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(10).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(11).getAllocatableMemoryAtException());

    System.out.println(sim.getExceptions().get(0).getInstructionType().getName());
    }

    @Test
void test3best_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,600),
            new AllocationInstruction(1,200),
            new AllocationInstruction(2, 200),
            new DeallocationInstruction(1),
            new DeallocationInstruction(10),
            new AllocationInstruction(3, 100),
            new AllocationInstruction(4, 100),
            new CompactInstruction(),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 200),
            new DeallocationInstruction(5),
            new DeallocationInstruction(4),
            new AllocationInstruction(6, 200)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(50),
            StrategyType.BEST_FIT); 
    

    assertEquals(0, sim.getMemory().fragmentation());
    sim.runAll(); 
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(12, sim.getExceptions().size());
    assertEquals(50, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(3).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(4).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(5).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(6).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(7).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(8).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(9).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(10).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(11).getAllocatableMemoryAtException());

    System.out.println(sim.getExceptions().get(0).getInstructionType().getName());
    }

    @Test
void test3worst_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,600),
            new AllocationInstruction(1,200),
            new AllocationInstruction(2, 200),
            new DeallocationInstruction(1),
            new DeallocationInstruction(10),
            new AllocationInstruction(3, 100),
            new AllocationInstruction(4, 100),
            new CompactInstruction(),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 200),
            new DeallocationInstruction(5),
            new DeallocationInstruction(4),
            new AllocationInstruction(6, 200)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(50),
            StrategyType.WORST_FIT); 
    
    assertEquals(0, sim.getMemory().fragmentation());
    sim.runAll();
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(12, sim.getExceptions().size());
    assertEquals(50, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(3).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(4).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(5).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(6).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(7).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(8).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(9).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(10).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(11).getAllocatableMemoryAtException());

    System.out.println(sim.getExceptions().get(0).getInstructionType().getName());
    }

    /*Test 4. (18 Instructions) -> Memory size is 200.

1) Allocate ID 100, size 60; 2) Allocate ID 1, size 40; 3) Allocate ID 2, size 30; 4) Allocate ID 3, size 30; 5) Allocate ID 4, size 60;

6) Deallocate ID 100; 7) Deallocate ID 3; 8) Allocate ID 5, size 20; 9) Allocate ID 6, size 40; 10) Deallocate ID 4;

11) Allocate ID 7, size 10; 12) Allocate ID 8, size 60; 13) Allocate ID 9, size 40; 14) Compact Memory; 15) Deallocate ID 8;

16) Allocate ID 10, size 30; 17) Deallocate ID 6; 18) Deallocate ID 9; */
    @Test
void test4best_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,60),
            new AllocationInstruction(1,40),
            new AllocationInstruction(2, 30),
            new AllocationInstruction(3, 30),
            new AllocationInstruction(4, 60),
            new DeallocationInstruction(100),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 20),
            new AllocationInstruction(6, 40),
            new DeallocationInstruction(4),
            new AllocationInstruction(7, 10),
            new AllocationInstruction(8, 60),
            new AllocationInstruction(9, 40),
            new CompactInstruction(),
            new DeallocationInstruction(8),
            new AllocationInstruction(10, 30),
            new DeallocationInstruction(6),
            new DeallocationInstruction(9)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(200),
            StrategyType.BEST_FIT); 
    sim.run(1); //first

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    testFreeSlots.add(new ProcessInterval(60, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //second
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    testFreeSlots.add(new ProcessInterval(100, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //third
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(100);
    neighborSet.add(2);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //fourth
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //fith
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //six
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(false, sim.getMemory().containsProcess(100));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(0, 59));
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0.4, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //seven
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);

    assertEquals(false, sim.getMemory().containsProcess(100));
    assertEquals(false, sim.getMemory().containsProcess(3));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(0, 59));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0.46153846153846156, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //eight
    testFreeSlots.clear();
    neighborSet.clear();

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(20, 59));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(5));
    assertEquals(0.36363636363636365, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //neuf
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(6));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());

    sim.run(1); //dix
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(6));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());

    sim.run(1); //onze
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    testFreeSlots.add(new ProcessInterval(140, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(7));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());

    sim.run(1); //douze
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);
    neighborSet.add(8);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(8).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(8).getHighAddress());
    assertEquals(60, sim.getMemory().processSize(8));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(7));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());

    sim.run(1); //treize
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(7);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(8).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(8).getHighAddress());
    assertEquals(60, sim.getMemory().processSize(8));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(8));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 14th test and step
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(8).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(8).getHighAddress());
    assertEquals(60, sim.getMemory().processSize(8));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(6));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 15th test and step
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(false, sim.getMemory().containsProcess(8));
    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    testFreeSlots.add(new ProcessInterval(140, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(6));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 16th test and step
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(10);
    neighborSet.add(2);

    assertEquals(false, sim.getMemory().containsProcess(8));
    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(10).getLowAddress());
    assertEquals(169, sim.getMemory().getProcessInterval(10).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(10));
    testFreeSlots.add(new ProcessInterval(170, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(7));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 17th test and step
    testFreeSlots.clear();
    neighborSet.clear();

    assertEquals(false, sim.getMemory().containsProcess(6));
    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(10).getLowAddress());
    assertEquals(169, sim.getMemory().getProcessInterval(10).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(10));
    testFreeSlots.add(new ProcessInterval(20, 59));
    testFreeSlots.add(new ProcessInterval(170, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(5));
    assertEquals(0.4285714285714286, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 18th test and step
    testFreeSlots.clear();
    neighborSet.clear();

    assertEquals(false, sim.getMemory().containsProcess(6));
    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(10).getLowAddress());
    assertEquals(169, sim.getMemory().getProcessInterval(10).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(10));
    testFreeSlots.add(new ProcessInterval(20, 59));
    testFreeSlots.add(new ProcessInterval(170, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(5));
    assertEquals(0.4285714285714286, sim.getMemory().fragmentation());
    assertEquals(4, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(40, sim.getExceptions().get(3).getAllocatableMemoryAtException());
    }

/*Test 4. (18 Instructions) -> Memory size is 200.

1) Allocate ID 100, size 60; 2) Allocate ID 1, size 40; 3) Allocate ID 2, size 30; 4) Allocate ID 3, size 30; 5) Allocate ID 4, size 60;

6) Deallocate ID 100; 7) Deallocate ID 3; 8) Allocate ID 5, size 20; 9) Allocate ID 6, size 40; 10) Deallocate ID 4;

11) Allocate ID 7, size 10; 12) Allocate ID 8, size 60; 13) Allocate ID 9, size 40; 14) Compact Memory; 15) Deallocate ID 8;

16) Allocate ID 10, size 30; 17) Deallocate ID 6; 18) Deallocate ID 9; */
@Test
void test4first_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,60),
            new AllocationInstruction(1,40),
            new AllocationInstruction(2, 30),
            new AllocationInstruction(3, 30),
            new AllocationInstruction(4, 60),
            new DeallocationInstruction(100),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 20),
            new AllocationInstruction(6, 40),
            new DeallocationInstruction(4),
            new AllocationInstruction(7, 10),
            new AllocationInstruction(8, 60),
            new AllocationInstruction(9, 40),
            new CompactInstruction(),
            new DeallocationInstruction(8),
            new AllocationInstruction(10, 30),
            new DeallocationInstruction(6),
            new DeallocationInstruction(9)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(200),
            StrategyType.FIRST_FIT); 
    sim.run(1); //first

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    testFreeSlots.add(new ProcessInterval(60, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //second
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    testFreeSlots.add(new ProcessInterval(100, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //third
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(100);
    neighborSet.add(2);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //fourth
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //fith
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //six
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(false, sim.getMemory().containsProcess(100));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(0, 59));
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0.4, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //seven
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);

    assertEquals(false, sim.getMemory().containsProcess(100));
    assertEquals(false, sim.getMemory().containsProcess(3));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(0, 59));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0.46153846153846156, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //eight
    testFreeSlots.clear();
    neighborSet.clear();

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(20, 59));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(5));
    assertEquals(0.36363636363636365, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //neuf
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(6));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());

    sim.run(1); //dix
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(6));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());

    sim.run(1); //onze
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    testFreeSlots.add(new ProcessInterval(140, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(7));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());

    sim.run(1); //douze
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);
    neighborSet.add(8);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(8).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(8).getHighAddress());
    assertEquals(60, sim.getMemory().processSize(8));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(7));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());

    sim.run(1); //treize
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(7);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(8).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(8).getHighAddress());
    assertEquals(60, sim.getMemory().processSize(8));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(8));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 14th test and step
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(8).getLowAddress());
    assertEquals(199, sim.getMemory().getProcessInterval(8).getHighAddress());
    assertEquals(60, sim.getMemory().processSize(8));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(6));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 15th test and step
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(false, sim.getMemory().containsProcess(8));
    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    testFreeSlots.add(new ProcessInterval(140, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(6));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 16th test and step
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(10);
    neighborSet.add(2);

    assertEquals(false, sim.getMemory().containsProcess(8));
    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(20, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(10).getLowAddress());
    assertEquals(169, sim.getMemory().getProcessInterval(10).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(10));
    testFreeSlots.add(new ProcessInterval(170, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(7));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 17th test and step
    testFreeSlots.clear();
    neighborSet.clear();

    assertEquals(false, sim.getMemory().containsProcess(6));
    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(10).getLowAddress());
    assertEquals(169, sim.getMemory().getProcessInterval(10).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(10));
    testFreeSlots.add(new ProcessInterval(20, 59));
    testFreeSlots.add(new ProcessInterval(170, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(5));
    assertEquals(0.4285714285714286, sim.getMemory().fragmentation());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());

    sim.run(1); // 18th test and step
    testFreeSlots.clear();
    neighborSet.clear();

    assertEquals(false, sim.getMemory().containsProcess(6));
    assertEquals(0, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(19, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    assertEquals(130, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(139, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(10, sim.getMemory().processSize(7));
    assertEquals(140, sim.getMemory().getProcessInterval(10).getLowAddress());
    assertEquals(169, sim.getMemory().getProcessInterval(10).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(10));
    testFreeSlots.add(new ProcessInterval(20, 59));
    testFreeSlots.add(new ProcessInterval(170, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(5));
    assertEquals(0.4285714285714286, sim.getMemory().fragmentation());
    assertEquals(4, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(70, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(0, sim.getExceptions().get(2).getAllocatableMemoryAtException());
    assertEquals(40, sim.getExceptions().get(3).getAllocatableMemoryAtException());
    }


/*Test 4. (18 Instructions) -> Memory size is 200.

1) Allocate ID 100, size 60; 2) Allocate ID 1, size 40; 3) Allocate ID 2, size 30; 4) Allocate ID 3, size 30; 5) Allocate ID 4, size 60;

6) Deallocate ID 100; 7) Deallocate ID 3; 8) Allocate ID 5, size 20; 9) Allocate ID 6, size 40; 10) Deallocate ID 4;

11) Allocate ID 7, size 10; 12) Allocate ID 8, size 60; 13) Allocate ID 9, size 40; 14) Compact Memory; 15) Deallocate ID 8;

16) Allocate ID 10, size 30; 17) Deallocate ID 6; 18) Deallocate ID 9; */
@Test
void test4worst_fit() {
    Set<ProcessInterval> testFreeSlots = new HashSet<>();
    Set<Integer> neighborSet = new HashSet<>();
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(100,60),
            new AllocationInstruction(1,40),
            new AllocationInstruction(2, 30),
            new AllocationInstruction(3, 30),
            new AllocationInstruction(4, 60),
            new DeallocationInstruction(100),
            new DeallocationInstruction(3),
            new AllocationInstruction(5, 20),
            new AllocationInstruction(6, 40),
            new DeallocationInstruction(4),
            new AllocationInstruction(7, 10),
            new AllocationInstruction(8, 60),
            new AllocationInstruction(9, 40),
            new CompactInstruction(),
            new DeallocationInstruction(8),
            new AllocationInstruction(10, 30),
            new DeallocationInstruction(6),
            new DeallocationInstruction(9)
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(200),
            StrategyType.WORST_FIT); 
    sim.run(1); //first

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    testFreeSlots.add(new ProcessInterval(60, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //second
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    testFreeSlots.add(new ProcessInterval(100, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(100));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //third
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(100);
    neighborSet.add(2);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //fourth
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(0, sim.getExceptions().size());

    sim.run(1); //fith
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(0, sim.getMemory().getProcessInterval(100).getLowAddress());
    assertEquals(59, sim.getMemory().getProcessInterval(100).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //six
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(1);
    neighborSet.add(3);

    assertEquals(false, sim.getMemory().containsProcess(100));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(130, sim.getMemory().getProcessInterval(3).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(3).getHighAddress());
    testFreeSlots.add(new ProcessInterval(0, 59));
    testFreeSlots.add(new ProcessInterval(160, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0.4, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //seven
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);

    assertEquals(false, sim.getMemory().containsProcess(100));
    assertEquals(false, sim.getMemory().containsProcess(3));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(0, 59));
    testFreeSlots.add(new ProcessInterval(130, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(1));
    assertEquals(0.46153846153846156, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //eight
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);


    assertEquals(130, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(149, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    testFreeSlots.add(new ProcessInterval(0, 59));
    testFreeSlots.add(new ProcessInterval(150, 199));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(5));
    assertEquals(0.4545454545454546, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());

    sim.run(1); //neuf
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(130, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(149, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(0, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(39, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    testFreeSlots.add(new ProcessInterval(150, 199));
    testFreeSlots.add(new ProcessInterval(40, 59));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0.2857142857142857, sim.getMemory().fragmentation());
    assertEquals(1, sim.getExceptions().size());

    sim.run(1); //dix
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(5);
    neighborSet.add(1);

    assertEquals(130, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(149, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(0, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(39, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(30, sim.getMemory().processSize(2));
    testFreeSlots.add(new ProcessInterval(150, 199));
    testFreeSlots.add(new ProcessInterval(40, 59));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringProcesses(2));
    assertEquals(0.2857142857142857, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());
    assertEquals(50, sim.getExceptions().get(1).getAllocatableMemoryAtException());

    sim.run(1); //onze
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);

    assertEquals(130, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(149, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(0, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(39, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(150, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(2, sim.getExceptions().size());

    sim.run(1); //douze
    testFreeSlots.clear();
    neighborSet.clear();
    neighborSet.add(2);
    neighborSet.add(8);

    assertEquals(130, sim.getMemory().getProcessInterval(5).getLowAddress());
    assertEquals(149, sim.getMemory().getProcessInterval(5).getHighAddress());
    assertEquals(20, sim.getMemory().processSize(5));
    assertEquals(0, sim.getMemory().getProcessInterval(6).getLowAddress());
    assertEquals(39, sim.getMemory().getProcessInterval(6).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(6));
    assertEquals(60, sim.getMemory().getProcessInterval(1).getLowAddress());
    assertEquals(99, sim.getMemory().getProcessInterval(1).getHighAddress());
    assertEquals(40, sim.getMemory().processSize(1));
    assertEquals(100, sim.getMemory().getProcessInterval(2).getLowAddress());
    assertEquals(129, sim.getMemory().getProcessInterval(2).getHighAddress());
    assertEquals(150, sim.getMemory().getProcessInterval(7).getLowAddress());
    assertEquals(159, sim.getMemory().getProcessInterval(7).getHighAddress());
    assertEquals(3, sim.getExceptions().size());
    assertEquals(40, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(50, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(40, sim.getExceptions().get(2).getAllocatableMemoryAtException());
}

//-----------------------------------------------------------------------
// NEW TEST 


@Test
void equalityTest() {
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(1,5),
            new AllocationInstruction(2,3),
            new AllocationInstruction(3,8),
            new AllocationInstruction(4,4),
            new AllocationInstruction(5,2),
            new DeallocationInstruction(2),
            new DeallocationInstruction(4),
            new AllocationInstruction(2,2)

    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(25),
            StrategyType.WORST_FIT);

    Queue<Instruction> instr2 = new ArrayDeque<>(Arrays.asList(
        new AllocationInstruction(1,5),
        new AllocationInstruction(2,3),
        new AllocationInstruction(3,8),
        new AllocationInstruction(4,4),
        new AllocationInstruction(5,2),
        new DeallocationInstruction(2),
        new DeallocationInstruction(4),
        new AllocationInstruction(2,2)
    ));
    SimulationInstance sim2 = new SimulationInstanceImpl(
            instr2,
            new MemoryImpl(25),
            StrategyType.WORST_FIT);    
    sim.runAll();
    sim.getMemory().fragmentation();
    sim2.runAll();
    sim2.getMemory().fragmentation();
    assertTrue(sim.getMemory().equals(sim2.getMemory()));
}

@Test
void customTest() {
    Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
            new AllocationInstruction(1,5),
            new AllocationInstruction(2,3),
            new AllocationInstruction(3,8),
            new AllocationInstruction(4,4),
            new AllocationInstruction(5,2),
            new DeallocationInstruction(2),
            new DeallocationInstruction(4),
            new AllocationInstruction(2,2),
            new CompactInstruction(),
            new AllocationInstruction(2,5),
            new AllocationInstruction(6,10),
            new DeallocationInstruction(6)//,
            //new CompactInstruction()
    ));
    SimulationInstance sim = new SimulationInstanceImpl(
            instr,
            new MemoryImpl(25),
            StrategyType.BEST_FIT);

    int step = 0; 
    double fragmentation = 0;
    for (Instruction instruction : instr) {
            sim.run(1);
            step++;
            if (step == 7 || step == 11)   
            {
                    fragmentation = sim.getMemory().fragmentation();
            }
    }
    Set<Integer> a = sim.getMemory().neighboringProcesses(3);
    sim.getMemory().fragmentation();
}
}

