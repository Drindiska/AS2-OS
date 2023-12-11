package se.lnu.os.ht23.a2.required;

import se.lnu.os.ht23.a2.provided.data.BlockInterval;
import se.lnu.os.ht23.a2.provided.interfaces.Memory;

import java.util.List;
import java.util.Set;

public class MemoryImpl implements Memory {

    private final int size;

    public MemoryImpl(int size){
        /* TODO
            Structure your memory how you like and initialize it here. This is the only constructor allowed and
            should create an empty memory of the given size. Feel free to add any variable or method you see
            fit for your implementation in this class
         */
        this.size = size;
    }

    @Override
    public boolean containsBlock(int blockId) {
        // TODO Replace this return statement with the method that checks if blockId is allocated in the memory
        return false;
    }

    @Override
    public List<Integer> blocks() {
        /* TODO
            Replace this return statement with the list of blockIds of the currently allocated blocks
            in the memory. If the memory is empty, return an empty List.
         */
        return null;
    }

    @Override
    public int blockDimension(int blockId) {
        /* TODO
            Replace this return statement with the method that returns the dimension of the block with blockId
            in the memory, 0 if it is not allocated.
         */
        return 0;
    }

    @Override
    public BlockInterval getBlockInterval(int blockId) {
        /* TODO
            Replace this return statement with the method that returns a BlockInterval instance containing the
            lower and upper address in memory of the block with blockId. Return null if the block is not allocated
         */
        return null;
    }

    @Override
    public Set<Integer> neighboringBlocks(int blockId) {
        /* TODO
            Replace this return statement with the method that returns the Set containing the ids of all the
            contiguous blocks to the one that has blockId (min. 0 if the block is between two free portions of
            memory and max. 2 if the block is surrounded both left and right by other blocks). For no neighboring
            blocks, return an empty Set.
         */
        return null;
    }

    @Override
    public double fragmentation() {
        /* TODO
            Replace this return statement with the method that returns the memory fragmentation value. There is
            no need to round decimals, as the Tests will do it before checking.
         */
        return 0;
    }

    @Override
    public Set<BlockInterval> freeSlots() {
        /* TODO
            Replace this return statement with the method that returns the set of BlockInterval instances
            corresponding to the free slots of the memory. Return exactly one BlockInterval per slot, make sure
            that you don't split any slot in two different intervals (e.g. if slot 0-199 is free, adding 0-99
            and 100-199 will be considered an error, while adding 0-199 is the only correct solution). If the
            memory is full, return an empty Set.
         */
        return null;
    }

    @Override
    public boolean equals(Object o) {
        /* TODO     -----> FOR EXCELLENT ONLY <-----
            Replace this return statement with the method that confronts two Memory objects. It is used by the tests
            whenever AssertEquals is called and should return true only when the Memories are structured exactly in
            the same way (same dimension, blocks and disposition), regardless of the Simulation they come from.
         */
        return false;
    }

    @Override
    public String toString() {
        StringBuilder retStr = new StringBuilder("Memory Size = " + size + "\n");
        if(blocks() != null) {
            for (int blockId : blocks()) {
                BlockInterval inter = getBlockInterval(blockId);
                retStr.append("(").append(inter.getLowAddress()).append("-").append(inter.getHighAddress()).append(")")
                        .append(" --> ").append("ID ").append(blockId).append("\n");
            }
        }
        if(freeSlots() != null) {
            for (BlockInterval bi : freeSlots()) {
                retStr.append("(").append(bi.getLowAddress()).append("-").append(bi.getHighAddress()).append(")")
                        .append(" --> ").append("EMPTY").append("\n");
            }
        }
        return retStr.toString();
    }
}
