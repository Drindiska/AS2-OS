package se.lnu.os.ht23.a2;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import se.lnu.os.ht23.a2.provided.abstract_.Instruction;
import se.lnu.os.ht23.a2.provided.data.BlockInterval;
import se.lnu.os.ht23.a2.provided.data.StrategyType;
import se.lnu.os.ht23.a2.provided.instructions.AllocationInstruction;
import se.lnu.os.ht23.a2.provided.instructions.CompactInstruction;
import se.lnu.os.ht23.a2.provided.instructions.DeallocationInstruction;
import se.lnu.os.ht23.a2.provided.interfaces.SimulationInstance;
import se.lnu.os.ht23.a2.required.MemoryImpl;
import se.lnu.os.ht23.a2.required.SimulationInstanceImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTests {
    @Test // Should be the only one working before starting your implementation
    void dummyTest() {
        SimulationInstance sim = new SimulationInstanceImpl(
                new ArrayDeque<>(),
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        sim.runAll();
        assertTrue(sim.getExceptions().isEmpty());
        assertNotEquals(StrategyType.WORST_FIT, sim.getStrategyType());
        System.out.println(sim);
    }

    @Test
    void oneInstructionTest() {
        Queue<Instruction> instr = new ArrayDeque<>();
        instr.add(new CompactInstruction());
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.BEST_FIT);
        assertEquals(1, sim.getInstructions().size());
        assertInstanceOf(CompactInstruction.class, sim.getInstructions().peek());
        sim.runAll();
        assertEquals(0, sim.getInstructions().size());
        assertNull(sim.getInstructions().peek());
    }

    @Test
    void twoInstructionsTest() {
        Queue<Instruction> instr = new ArrayDeque<>(Arrays.asList(
                new DeallocationInstruction(100),
                new AllocationInstruction(1,5)
        ));
        SimulationInstance sim = new SimulationInstanceImpl(
                instr,
                new MemoryImpl(10),
                StrategyType.WORST_FIT);
        assertEquals(2, sim.getInstructions().size());
        assertInstanceOf(DeallocationInstruction.class, sim.getInstructions().peek());
        assertEquals(100, ((DeallocationInstruction) Objects.requireNonNull(sim.getInstructions().peek())).getBlockId());
        sim.run(1);
        assertEquals(1, sim.getInstructions().size());
        assertEquals(1, sim.getExceptions().size());
        assertEquals(10, sim.getExceptions().get(0).getAllocatableMemoryAtException());
        assertEquals(DeallocationInstruction.class, sim.getExceptions().get(0).getInstructionType());
        assertInstanceOf(AllocationInstruction.class, sim.getInstructions().peek());
        assertEquals(1, ((AllocationInstruction) Objects.requireNonNull(sim.getInstructions().peek())).getBlockId());
        assertEquals(5, ((AllocationInstruction) Objects.requireNonNull(sim.getInstructions().peek())).getDimension());
        sim.runAll();
        assertEquals(0, sim.getInstructions().size());
        assertNull(sim.getInstructions().peek());
        assertFalse(sim.getMemory().containsBlock(2));
        assertEquals(5, sim.getMemory().blockDimension(1));
        assertEquals(0, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(4, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertTrue(sim.getMemory().neighboringBlocks(1).isEmpty());
        assertEquals(1, sim.getMemory().freeSlots().size());
        assertTrue(sim.getMemory().freeSlots().contains(new BlockInterval(5, 9)));
        assertEquals(0, sim.getMemory().fragmentation());
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
        assertFalse(sim.getMemory().containsBlock(1));
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
        assertEquals(0, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(4, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(5, sim.getMemory().getBlockInterval(2).getLowAddress());
        assertEquals(5, sim.getMemory().getBlockInterval(2).getHighAddress());
        assertEquals(6, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(7, sim.getMemory().getBlockInterval(3).getHighAddress());
        sim.run(2);
        assertEquals(5, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(6, sim.getMemory().getBlockInterval(3).getHighAddress());
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
        assertTrue(sim.getMemory().containsBlock(1));
        assertTrue(sim.getMemory().containsBlock(3));
        assertFalse(sim.getMemory().containsBlock(4));
        assertFalse(sim.getMemory().containsBlock(2));
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
        assertTrue(sim.getMemory().containsBlock(1));
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
        Set<BlockInterval> testFreeSlots = new HashSet<>();
        testFreeSlots.add(new BlockInterval(4, 9));
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
        Set<BlockInterval> testFreeSlots = new HashSet<>();
        testFreeSlots.add(new BlockInterval(8, 9));
        testFreeSlots.add(new BlockInterval(2, 5));
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
        System.out.println(sim.getMemory().neighboringBlocks(2));
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
        System.out.println(sim.getMemory().neighboringBlocks(5));
        System.out.println(sim.getMemory().neighboringBlocks(2));
        System.out.println(sim.getMemory().neighboringBlocks(7));
        sim.run(1);
        System.out.println(sim.getMemory().neighboringBlocks(2));

    }

    @Test
    void blockIntervalTest() {
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
        assertEquals(5, sim.getMemory().getBlockInterval(2).getHighAddress());
        assertEquals(2, sim.getMemory().getBlockInterval(2).getLowAddress());
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
        assertEquals(2, sim.getMemory().getBlockInterval(9).getHighAddress());
        assertEquals(1, sim.getMemory().getBlockInterval(9).getLowAddress());
        assertEquals(7, sim.getMemory().getBlockInterval(10).getHighAddress());
        assertEquals(6, sim.getMemory().getBlockInterval(10).getLowAddress());
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
        assertEquals(2, sim.getMemory().getBlockInterval(9).getHighAddress());
        assertEquals(1, sim.getMemory().getBlockInterval(9).getLowAddress());
        assertEquals(3, sim.getMemory().getBlockInterval(10).getHighAddress());
        assertEquals(4, sim.getMemory().getBlockInterval(10).getLowAddress());
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
        assertEquals(9, sim.getMemory().getBlockInterval(9).getHighAddress());
        assertEquals(8, sim.getMemory().getBlockInterval(9).getLowAddress());
        assertEquals(2, sim.getMemory().getBlockInterval(10).getHighAddress());
        assertEquals(1, sim.getMemory().getBlockInterval(10).getLowAddress());
    }

//1) Allocate ID 100, size 1000; 2) Allocate ID 1, size 500; 3) Deallocate ID 100; 4) Allocate ID 2, size 200; 5) Compact Memory;
//6) Deallocate ID 2; 7) Allocate ID 3, size 500; 8) Deallocate ID 1; 9) Allocate ID 4, size 100; 10) Compact Memory;
    @Test
    void test1BestFit() {
        Set<BlockInterval> testFreeSlots = new HashSet<>();
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
                StrategyType.BEST_FIT);
        // First test after 1 instruction
        sim.run(1);

        // Check intervals, Dimention
        assertEquals(999, sim.getMemory().getBlockInterval(100).getHighAddress());
        assertEquals(0, sim.getMemory().getBlockInterval(100).getLowAddress());
        assertEquals(1000, sim.getMemory().blockDimension(100));

        // Check fragmentation
        assertEquals(0 , sim.getMemory().fragmentation());

        testFreeSlots.add(new BlockInterval(1000, 1999));

        // check free slots
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());

        sim.run(1);
        testFreeSlots.clear();

        testFreeSlots.add(new BlockInterval(1500, 1999));
        neighborSet.add(100);
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(1));
        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(100));
        assertEquals(999, sim.getMemory().getBlockInterval(100).getHighAddress());
        assertEquals(0, sim.getMemory().getBlockInterval(100).getLowAddress());
        assertEquals(1499, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(1000, sim.getMemory().getBlockInterval(1).getLowAddress());

        sim.run(1);
        neighborSet.clear();
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(1));
        
        assertEquals(0.33333333333333337 , sim.getMemory().fragmentation());
        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(0, 999));
        testFreeSlots.add(new BlockInterval(1500, 1999));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());

        assertEquals(1499, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(1000, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(500, sim.getMemory().blockDimension(1));

        sim.run(1);

        assertEquals(1500, sim.getMemory().getBlockInterval(2).getLowAddress());
        assertEquals(1699, sim.getMemory().getBlockInterval(2).getHighAddress());

        sim.run(1);

        assertEquals(499, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(0, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(500, sim.getMemory().getBlockInterval(2).getLowAddress());
        assertEquals(699, sim.getMemory().getBlockInterval(2).getHighAddress());
        assertEquals(0, sim.getMemory().fragmentation());
        
        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(700, 1999));

        sim.run(1);

        assertEquals(0, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(499, sim.getMemory().getBlockInterval(1).getHighAddress());
        
        sim.run(1);

        assertEquals(0, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(499, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(500, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(999, sim.getMemory().getBlockInterval(3).getHighAddress());

        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(1000, 1999));

        assertEquals(0, sim.getMemory().fragmentation());

        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));
        sim.run(1);

        assertEquals(500, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(999, sim.getMemory().getBlockInterval(3).getHighAddress());
        
        testFreeSlots.add(new BlockInterval(0, 499));
        neighborSet.clear();
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));

        sim.run(1);
        
        assertEquals(0, sim.getMemory().getBlockInterval(4).getLowAddress());
        assertEquals(99, sim.getMemory().getBlockInterval(4).getHighAddress());
        assertEquals(500, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(999, sim.getMemory().getBlockInterval(3).getHighAddress());
        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(100, 499));
        testFreeSlots.add(new BlockInterval(1000, 1999));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(4));
        assertEquals(0.2857142857142857, sim.getMemory().fragmentation());

        sim.run(1);
        
        assertEquals(0, sim.getMemory().getBlockInterval(4).getLowAddress());
        assertEquals(99, sim.getMemory().getBlockInterval(4).getHighAddress());
        assertEquals(100, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(599, sim.getMemory().getBlockInterval(3).getHighAddress());
        assertEquals(0, sim.getMemory().fragmentation());
        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(600, 1999));
        neighborSet.clear();
        neighborSet.add(4);
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));
        neighborSet.clear();
        neighborSet.add(3);
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(4)); 
        assertEquals(0, sim.getExceptions().size());
    }

    @Test
    void test1WorstFit() {
        Set<BlockInterval> testFreeSlots = new HashSet<>();
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
        sim.run(1);

        // Check intervals, Dimention
        assertEquals(999, sim.getMemory().getBlockInterval(100).getHighAddress());
        assertEquals(0, sim.getMemory().getBlockInterval(100).getLowAddress());
        assertEquals(1000, sim.getMemory().blockDimension(100));

        // Check fragmentation
        assertEquals(0 , sim.getMemory().fragmentation());

        testFreeSlots.add(new BlockInterval(1000, 1999));

        // check free slots
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());

        sim.run(1);
        testFreeSlots.clear();

        testFreeSlots.add(new BlockInterval(1500, 1999));
        neighborSet.add(100);
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(1));
        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(100));
        assertEquals(999, sim.getMemory().getBlockInterval(100).getHighAddress());
        assertEquals(0, sim.getMemory().getBlockInterval(100).getLowAddress());
        assertEquals(1499, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(1000, sim.getMemory().getBlockInterval(1).getLowAddress());

        sim.run(1);
        neighborSet.clear();
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(1));
        
        assertEquals(0.33333333333333337 , sim.getMemory().fragmentation());
        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(0, 999));
        testFreeSlots.add(new BlockInterval(1500, 1999));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());

        assertEquals(1499, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(1000, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(500, sim.getMemory().blockDimension(1));

        sim.run(1);

        assertEquals(0, sim.getMemory().getBlockInterval(2).getLowAddress());
        assertEquals(199, sim.getMemory().getBlockInterval(2).getHighAddress());

        sim.run(1);

        assertEquals(699, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(200, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(0, sim.getMemory().getBlockInterval(2).getLowAddress());
        assertEquals(199, sim.getMemory().getBlockInterval(2).getHighAddress());
        assertEquals(0, sim.getMemory().fragmentation());
        
        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(700, 1999));

        sim.run(1);

        assertEquals(200, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(699, sim.getMemory().getBlockInterval(1).getHighAddress());
        
        sim.run(1);

        assertEquals(200, sim.getMemory().getBlockInterval(1).getLowAddress());
        assertEquals(699, sim.getMemory().getBlockInterval(1).getHighAddress());
        assertEquals(700, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(1199, sim.getMemory().getBlockInterval(3).getHighAddress());

        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(1200, 1999));

        assertEquals(0.19999999999999996, sim.getMemory().fragmentation());

        neighborSet.clear();
        neighborSet.add(1);
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));

        sim.run(1);

        assertEquals(700, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(1199, sim.getMemory().getBlockInterval(3).getHighAddress());
        
        testFreeSlots.add(new BlockInterval(0, 699));
        neighborSet.clear();
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));

        sim.run(1);
        
        assertEquals(1200, sim.getMemory().getBlockInterval(4).getLowAddress());
        assertEquals(1299, sim.getMemory().getBlockInterval(4).getHighAddress());
        assertEquals(700, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(1199, sim.getMemory().getBlockInterval(3).getHighAddress());
        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(0, 699));
        testFreeSlots.add(new BlockInterval(1300, 1999));
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        neighborSet.add(4);
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));
        neighborSet.clear();
        neighborSet.add(3);
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(4));
        assertEquals(0.5, sim.getMemory().fragmentation());

        sim.run(1);
        
        assertEquals(500, sim.getMemory().getBlockInterval(4).getLowAddress());
        assertEquals(599, sim.getMemory().getBlockInterval(4).getHighAddress());
        assertEquals(0, sim.getMemory().getBlockInterval(3).getLowAddress());
        assertEquals(499, sim.getMemory().getBlockInterval(3).getHighAddress());
        assertEquals(0, sim.getMemory().fragmentation());
        testFreeSlots.clear();
        testFreeSlots.add(new BlockInterval(600, 1999));
        neighborSet.clear();
        neighborSet.add(4);
        assertEquals(testFreeSlots, sim.getMemory().freeSlots());
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));
        neighborSet.clear();
        neighborSet.add(3);
        assertEquals(neighborSet, sim.getMemory().neighboringBlocks(4)); 
        assertEquals(0, sim.getExceptions().size());
    }

@Test
void test1First_Fit() {
    Set<BlockInterval> testFreeSlots = new HashSet<>();
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
    sim.run(1);

    // Check intervals, Dimention
    assertEquals(999, sim.getMemory().getBlockInterval(100).getHighAddress());
    assertEquals(0, sim.getMemory().getBlockInterval(100).getLowAddress());
    assertEquals(1000, sim.getMemory().blockDimension(100));

    // Check fragmentation
    assertEquals(0 , sim.getMemory().fragmentation());

    testFreeSlots.add(new BlockInterval(1000, 1999));

    // check free slots
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    sim.run(1);
    testFreeSlots.clear();

    testFreeSlots.add(new BlockInterval(1500, 1999));
    neighborSet.add(100);
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(1));
    neighborSet.clear();
    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(100));
    assertEquals(999, sim.getMemory().getBlockInterval(100).getHighAddress());
    assertEquals(0, sim.getMemory().getBlockInterval(100).getLowAddress());
    assertEquals(1499, sim.getMemory().getBlockInterval(1).getHighAddress());
    assertEquals(1000, sim.getMemory().getBlockInterval(1).getLowAddress());

    sim.run(1);
    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(1));
    
    assertEquals(0.33333333333333337 , sim.getMemory().fragmentation());
    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(0, 999));
    testFreeSlots.add(new BlockInterval(1500, 1999));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    assertEquals(1499, sim.getMemory().getBlockInterval(1).getHighAddress());
    assertEquals(1000, sim.getMemory().getBlockInterval(1).getLowAddress());
    assertEquals(500, sim.getMemory().blockDimension(1));

    sim.run(1);

    assertEquals(0, sim.getMemory().getBlockInterval(2).getLowAddress());
    assertEquals(199, sim.getMemory().getBlockInterval(2).getHighAddress());

    sim.run(1);

    assertEquals(699, sim.getMemory().getBlockInterval(1).getHighAddress());
    assertEquals(200, sim.getMemory().getBlockInterval(1).getLowAddress());
    assertEquals(0, sim.getMemory().getBlockInterval(2).getLowAddress());
    assertEquals(199, sim.getMemory().getBlockInterval(2).getHighAddress());
    assertEquals(0, sim.getMemory().fragmentation());
    
    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(700, 1999));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    sim.run(1);

    assertEquals(200, sim.getMemory().getBlockInterval(1).getLowAddress());
    assertEquals(699, sim.getMemory().getBlockInterval(1).getHighAddress());
    
    sim.run(1);

    assertEquals(200, sim.getMemory().getBlockInterval(1).getLowAddress());
    assertEquals(699, sim.getMemory().getBlockInterval(1).getHighAddress());
    assertEquals(700, sim.getMemory().getBlockInterval(3).getLowAddress());
    assertEquals(1199, sim.getMemory().getBlockInterval(3).getHighAddress());

    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(1200, 1999));
    testFreeSlots.add(new BlockInterval(0, 199));

    assertEquals(0.19999999999999996, sim.getMemory().fragmentation());

    neighborSet.clear();
    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));

    sim.run(1);

    assertEquals(700, sim.getMemory().getBlockInterval(3).getLowAddress());
    assertEquals(1199, sim.getMemory().getBlockInterval(3).getHighAddress());
    
    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(1200, 1999));
    testFreeSlots.add(new BlockInterval(0, 699));
    neighborSet.clear();
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));

    sim.run(1);
    
    assertEquals(700, sim.getMemory().getBlockInterval(3).getLowAddress());
    assertEquals(1199, sim.getMemory().getBlockInterval(3).getHighAddress());
    assertEquals(0, sim.getMemory().getBlockInterval(4).getLowAddress());
    assertEquals(99, sim.getMemory().getBlockInterval(4).getHighAddress());
    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(100, 699));
    testFreeSlots.add(new BlockInterval(1200, 1999));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(4));
    assertEquals(0.4285714285714286, sim.getMemory().fragmentation());

    sim.run(1);
    
    assertEquals(0, sim.getMemory().getBlockInterval(4).getLowAddress());
    assertEquals(99, sim.getMemory().getBlockInterval(4).getHighAddress());
    assertEquals(100, sim.getMemory().getBlockInterval(3).getLowAddress());
    assertEquals(599, sim.getMemory().getBlockInterval(3).getHighAddress());
    assertEquals(0, sim.getMemory().fragmentation());
    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(600, 1999));
    neighborSet.clear();
    neighborSet.add(4);
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(3));
    neighborSet.clear();
    neighborSet.add(3);
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(4)); 
    assertEquals(0, sim.getExceptions().size());
    }

//1) Allocate ID 100, size 600; 2) Allocate ID 1, size 200; 3) Allocate ID 2, size 200; 4) Deallocate ID 1; 5) Deallocate ID 10;
//6) Allocate ID 3, size 100; 7) Allocate ID 4, size 100; 8) Compact Memory; 9) Deallocate ID 3; 10) Allocate ID 5, size 200;
//11) Deallocate ID 5; 12) Deallocate ID 4; 13) Allocate ID 6, size 200;
@Test
void test2best_fit() {
    Set<BlockInterval> testFreeSlots = new HashSet<>();
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
    assertEquals(listIds, sim.getMemory().blocks());
    sim.run(1);
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(1, sim.getExceptions().size());
    sim.run(1);
    assertEquals(0, sim.getMemory().getBlockInterval(1).getLowAddress());
    assertEquals(199, sim.getMemory().getBlockInterval(1).getHighAddress());
    testFreeSlots.add(new BlockInterval(200, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(1));
    assertEquals(0, sim.getMemory().fragmentation());
    sim.run(1);
    assertEquals(0, sim.getMemory().getBlockInterval(1).getLowAddress());
    assertEquals(199, sim.getMemory().getBlockInterval(1).getHighAddress());
    assertEquals(200, sim.getMemory().getBlockInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getBlockInterval(2).getHighAddress());
    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(400, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(0, sim.getMemory().fragmentation());
    neighborSet.add(1);
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(2));
    neighborSet.clear();
    neighborSet.add(2);
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(1));
    sim.run(1);
    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(2));
    assertEquals(false, sim.getMemory().containsBlock(1));

    sim.run(1);
    assertEquals(500, sim.getExceptions().get(0).getAllocatableMemoryAtException());
    assertEquals(200, sim.getExceptions().get(1).getAllocatableMemoryAtException());
    assertEquals(2, sim.getExceptions().size());

    sim.run(1);
    assertEquals(400, sim.getMemory().getBlockInterval(3).getLowAddress());
    assertEquals(499, sim.getMemory().getBlockInterval(3).getHighAddress());

    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(0, 199));

    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    
    sim.run(1);

    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(100, 199));

    assertEquals(400, sim.getMemory().getBlockInterval(3).getLowAddress());
    assertEquals(499, sim.getMemory().getBlockInterval(3).getHighAddress());
    assertEquals(200, sim.getMemory().getBlockInterval(2).getLowAddress());
    assertEquals(399, sim.getMemory().getBlockInterval(2).getHighAddress());
    assertEquals(0, sim.getMemory().getBlockInterval(4).getLowAddress());
    assertEquals(99, sim.getMemory().getBlockInterval(4).getHighAddress());

    assertEquals(0, sim.getMemory().fragmentation());

    sim.run(1);

    neighborSet.clear();
    neighborSet.add(4);
    neighborSet.add(3);

    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(2));
    assertEquals(2, sim.getExceptions().size());

    assertEquals(300, sim.getMemory().getBlockInterval(3).getLowAddress());
    assertEquals(399, sim.getMemory().getBlockInterval(3).getHighAddress());
    assertEquals(100, sim.getMemory().getBlockInterval(2).getLowAddress());
    assertEquals(299, sim.getMemory().getBlockInterval(2).getHighAddress());
    assertEquals(0, sim.getMemory().getBlockInterval(4).getLowAddress());
    assertEquals(99, sim.getMemory().getBlockInterval(4).getHighAddress());

    sim.run(2);

    testFreeSlots.clear();

    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    neighborSet.clear();
    neighborSet.add(4);
    neighborSet.add(5);
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(2));
    assertEquals(0, sim.getMemory().fragmentation());
    assertEquals(2, sim.getExceptions().size());

    sim.run(2);

    neighborSet.clear();
    assertEquals(neighborSet, sim.getMemory().neighboringBlocks(2));

    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(0, 99));
    testFreeSlots.add(new BlockInterval(300, 499));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());

    sim.run(1);
    
    testFreeSlots.clear();
    testFreeSlots.add(new BlockInterval(0, 99));
    assertEquals(testFreeSlots, sim.getMemory().freeSlots());
    assertEquals(100, sim.getMemory().getBlockInterval(2).getLowAddress());
    assertEquals(299, sim.getMemory().getBlockInterval(2).getHighAddress());
    assertEquals(300, sim.getMemory().getBlockInterval(6).getLowAddress());
    assertEquals(499, sim.getMemory().getBlockInterval(6).getHighAddress());
    }
}