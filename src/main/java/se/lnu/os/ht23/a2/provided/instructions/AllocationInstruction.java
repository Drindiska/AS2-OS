package se.lnu.os.ht23.a2.provided.instructions;

import se.lnu.os.ht23.a2.provided.abstract_.Instruction;

public class AllocationInstruction extends Instruction {
    public int getBlockId() {
        return blockId;
    }

    public int getDimension() {
        return dimension;
    }

    private final int blockId;
    private final int dimension;

    public AllocationInstruction(int blockId, int dimension) {
        this.blockId = blockId;
        this.dimension = dimension;
    }

    @Override
    public String toString() {
        return "A(" + blockId + ", " + dimension + ')';
    }
}
