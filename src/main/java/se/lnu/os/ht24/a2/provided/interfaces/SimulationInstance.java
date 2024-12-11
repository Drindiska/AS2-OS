package se.lnu.os.ht24.a2.provided.interfaces;

import se.lnu.os.ht24.a2.provided.abstract_.Instruction;
import se.lnu.os.ht24.a2.provided.data.StrategyType;
import se.lnu.os.ht24.a2.provided.exceptions.InstructionException;

import java.util.List;
import java.util.Queue;

public interface SimulationInstance {
    void runAll();
    void run(int steps);
    Memory getMemory();
    Queue<Instruction> getInstructions();
    StrategyType getStrategyType();
    List<InstructionException> getExceptions();
}
