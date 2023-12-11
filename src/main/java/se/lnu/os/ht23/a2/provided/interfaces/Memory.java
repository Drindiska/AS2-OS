package se.lnu.os.ht23.a2.provided.interfaces;

import se.lnu.os.ht23.a2.provided.data.BlockInterval;

import java.util.List;
import java.util.Set;

public interface Memory {
    boolean containsBlock(int blockId);
    List<Integer> blocks();
    int blockDimension(int blockId);
    BlockInterval getBlockInterval(int blockId);
    Set<Integer> neighboringBlocks(int blockId);
    double fragmentation();
    Set<BlockInterval> freeSlots();
}
