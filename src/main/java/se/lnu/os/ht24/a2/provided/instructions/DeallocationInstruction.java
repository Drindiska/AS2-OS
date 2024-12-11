package se.lnu.os.ht24.a2.provided.instructions;

import se.lnu.os.ht24.a2.provided.abstract_.Instruction;

public class DeallocationInstruction extends Instruction {
    public int getProcessId() {
        return processId;
    }

    private final int processId;

    public DeallocationInstruction(int blockId) {
        this.processId = blockId;
    }

    @Override
    public String toString() {
        return "D(" + processId + ')';
    }
}
