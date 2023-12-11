package se.lnu.os.ht23.a2.provided.instructions;

import se.lnu.os.ht23.a2.provided.abstract_.Instruction;

public class DeallocationInstruction extends Instruction {
    public int getBlockId() {
        return blockId;
    }

    private final int blockId;

    public DeallocationInstruction(int blockId) {
        this.blockId = blockId;
    }

    @Override
    public String toString() {
        return "D(" + blockId + ')';
    }
}
