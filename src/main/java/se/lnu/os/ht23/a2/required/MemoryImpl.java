package se.lnu.os.ht23.a2.required;

import se.lnu.os.ht23.a2.provided.data.BlockInterval;
import se.lnu.os.ht23.a2.provided.interfaces.Memory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class MemoryImpl implements Memory {

    private final int size;
    private Dictionary<Integer, Integer> memory = new Hashtable<>();
    private Dictionary<Integer, Integer> blockListNoneAllocated = new Hashtable<>();
    private Dictionary<Integer, Integer> blockListAllocated = new Hashtable<>();

    public MemoryImpl(int size){
        /* TODO
            Structure your memory how you like and initialize it here. This is the only constructor allowed and
            should create an empty memory of the given size. Feel free to add any variable or method you see
            fit for your implementation in this class
         */

        int counter = 0;
        while (counter != size) {
            memory.put(counter, null);
            counter = counter + 1;
        }
        this.size = size;
    }

    public void AddBlock(int idBlock, int dimension) {
        blockListNoneAllocated.put(idBlock, dimension);
    }

    public void AllocateBlock(int idBlock, int dimension) {
        // memoryAvaible is a counter to count the size of the block avaible.
        //CurrentMemory is the current block selected
        int memoryAvaible = 0;
        ArrayList<Integer> currentMemory = new ArrayList<Integer>();
        
        // We will go throug all the memory slots.
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            // get the current memory.
            int key = id.nextElement();
            // if memory not avaible else if avaible.
            if (memory.get(key) != 0) {
                memoryAvaible = 0;
            } else if (memory.get(key) == null) {
                memoryAvaible = memoryAvaible + 1;
            }

            // if the memory is enough to put the instruction.
            if (memoryAvaible == dimension) {
                // we set the memory to taken.
                for (int i: currentMemory) {
                    memory.put(i, idBlock);
                }
                // we add the block to the dictionay allocated and delete it from the unassigned block.
                blockListAllocated.put(idBlock, dimension);
                blockListNoneAllocated.remove(idBlock);
            }
        }
    }

    @Override
    public boolean containsBlock(int blockId) {
        // TODO Replace this return statement with the method that checks if blockId is allocated in the memory

        Enumeration<Integer> id = blockListAllocated.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (key == blockId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Integer> blocks() {
        /* TODO
            Replace this return statement with the list of blockIds of the currently allocated blocks
            in the memory. If the memory is empty, return an empty List.
         */

        List<Integer> listIds = new ArrayList<Integer>();

        Enumeration<Integer> id = blockListAllocated.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            listIds.add(key);
        }
        return listIds;
    }

    @Override
    public int blockDimension(int blockId) {
        /* TODO
            Replace this return statement with the method that returns the dimension of the block with blockId
            in the memory, 0 if it is not allocated.
         */
        Enumeration<Integer> id = blockListAllocated.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (key == blockId) {
                return blockListAllocated.get(key);
            }
        }
        return 0;
    }

    @Override
    public BlockInterval getBlockInterval(int blockId) {
        int lowAddress = 0;
        int highAddress= 0;
        boolean allocated = false;
        /* TODO
            Replace this return statement with the method that returns a BlockInterval instance containing the
            lower and upper address in memory of the block with blockId. Return null if the block is not allocated
         */
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (memory.get(key) == blockId) {
                if (allocated) {
                    highAddress = key;
                } else {
                    lowAddress = key;
                    allocated = true;
                }
            }
        }
        if (allocated) {
            return new BlockInterval(lowAddress, highAddress);
        } else {
            return null;
        }
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
