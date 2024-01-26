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

}

