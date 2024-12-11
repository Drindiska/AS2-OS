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
}
