package se.lnu.os.ht24.a2.provided.instructions;

import se.lnu.os.ht24.a2.provided.abstract_.Instruction;

public class AllocationInstruction extends Instruction {
    public int getProcessId() {
        return processId;
    }

    public int getDimension() {
        return dimension;
    }

    private final int processId;
    private final int dimension;

    public AllocationInstruction(int processId, int dimension) {
        this.processId = processId;
        this.dimension = dimension;
    }

    @Override
    public String toString() {
        return "A(" + processId + ", " + dimension + ')';
    }
}
