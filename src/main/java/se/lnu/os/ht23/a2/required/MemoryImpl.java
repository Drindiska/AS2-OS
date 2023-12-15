package se.lnu.os.ht23.a2.required;

import se.lnu.os.ht23.a2.provided.data.BlockInterval;
import se.lnu.os.ht23.a2.provided.interfaces.Memory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class MemoryImpl implements Memory {

    private final int size;
    private Dictionary<Integer, Integer> memory = new Hashtable<Integer, Integer>();
    private Dictionary<Integer, Integer> blockListNoneAllocated = new Hashtable<Integer, Integer>();
    private Dictionary<Integer, Integer> blockListAllocated = new Hashtable<Integer, Integer>();

    public MemoryImpl(int size){
        /* TODO
            Structure your memory how you like and initialize it here. This is the only constructor allowed and
            should create an empty memory of the given size. Feel free to add any variable or method you see
            fit for your implementation in this class
         */

        this.size = size;
        int counter = size - 1;
        while (0 <= counter) {
            System.out.print(counter);
            memory.put(counter, -1);
            counter = counter - 1;
        }
    }

    public void AddBlock(int idBlock, int dimension) {
        blockListNoneAllocated.put(idBlock, dimension);
    }

    public boolean AllocateBlock(int idBlock, int dimension) {
        // memoryAvaible is a counter to count the size of the block avaible.
        //CurrentMemory is the current block selected
        int memoryAvaible = 0;
        ArrayList<Integer> currentMemory = new ArrayList<Integer>();

        blockListNoneAllocated.put(idBlock, dimension);
        
        // We will go throug all the memory slots.
        Enumeration<Integer> id = memory.keys();
        List<Integer> list = new ArrayList<>();

        while (id.hasMoreElements()) {
            int key = id.nextElement();
            list.add(key);
        }
        ArrayList<Integer> revArrayList = new ArrayList<Integer>();
        for (int i = list.size() - 1; i >= 0; i--) {
 
            // Append the elements in reverse order
            revArrayList.add(list.get(i));
        }

        for (int key : revArrayList) {
            // get the current memory.
            System.out.println(key);
            // if memory not avaible else if avaible.
            if (memory.get(key) != -1) {
                memoryAvaible = 0;
                currentMemory.clear();
            } else if (memory.get(key) == -1) {
                memoryAvaible = memoryAvaible + 1;
                currentMemory.add(key);
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
                return true;
            }
        }
        if (dimension > memory.size()) {
            blockListNoneAllocated.remove(idBlock);
            return false;
        } else {
            AddBlock(idBlock, dimension);
        }
        return true;
    }

    public boolean unAllocate(int idBlock) {


        if (((Hashtable<Integer, Integer>) blockListAllocated).containsKey(idBlock) == false || ((Hashtable<Integer, Integer>) blockListNoneAllocated).containsKey(idBlock) == false) {
            return false;
        }
        
        // We will go throug all the memory slots.
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            // get the current memory.
            int key = id.nextElement();
            // if memory not avaible else if avaible.
            if (memory.get(key) == idBlock) {
                memory.put(key, -1);
            }
        }
        blockListAllocated.remove(idBlock);
        return true;
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
                return blockListAllocated.get(blockId);
            }
        }
        Enumeration<Integer> id2 = blockListNoneAllocated.keys();
        while (id2.hasMoreElements()) {
            int key = id2.nextElement();
            if (key == blockId) {
                return blockListNoneAllocated.get(blockId);
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
            System.out.println(lowAddress + " - " + highAddress);
            return new BlockInterval(highAddress, lowAddress);
        } else {
            return null;
        }
    }

    public void compact() {
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            memory.put(key, -1);
        }
        Enumeration<Integer> id2 = blockListAllocated.keys();
        while (id2.hasMoreElements()) {
            int key = id2.nextElement();
            int dimmension = blockListAllocated.get(key);
            blockListAllocated.remove(key);
            AddBlock(key, dimmension);
        }
        Enumeration<Integer> id3 = blockListNoneAllocated.keys();
        while (id3.hasMoreElements()) {
            int key = id3.nextElement();
            int dimmension = blockListNoneAllocated.get(key);
            blockListNoneAllocated.remove(key);
            AddBlock(key, dimmension);
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
        
        Set<Integer> neighbor = new HashSet<>(); 
        
        boolean lower = true;
        boolean high = true;
        int lowBlockId = 0;
        int highBlockId = 0;

        BlockInterval block = getBlockInterval(blockId);
        if (block.getLowAddress() == 0) {
            lower = false;
        }
        else if (memory.get(block.getLowAddress() - 1) == -1) {
            lower = false;
        } else {
            lowBlockId = memory.get(block.getLowAddress() - 1);
        }
        if (block.getHighAddress() == size - 1) {
            high = false;
        } else if (memory.get(block.getHighAddress() + 1) == -1) {
            high = false;
        } else {
            highBlockId = memory.get(block.getHighAddress() - 1);
        }

        if (lower) {
            neighbor.add(lowBlockId);
        }
        if (high) {
            neighbor.add(highBlockId);
        }

        return neighbor;
    }

    @Override
    public double fragmentation() {
        /* TODO
            Replace this return statement with the method that returns the memory fragmentation value. There is
            no need to round decimals, as the Tests will do it before checking.
         */

        int biggest = 0;
        int currentBlock = 0;
        int freeMemory = 0;
        
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (memory.get(key) == -1) {
                freeMemory = freeMemory + 1;
                currentBlock = currentBlock + 1;
            } else if (currentBlock > biggest) {
                biggest = currentBlock;
                currentBlock = 0;
            } else {
                currentBlock = 0;
            }
        }
        int fragmentation = 1 - (biggest/freeMemory);

        return fragmentation;
    }

    public int getBiggestMemoryAvaible() {
        int biggest = 0;
        int currentBlock = 0;
        
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (memory.get(key) == -1) {
                currentBlock = currentBlock + 1;
            } else if (currentBlock > biggest) {
                biggest = currentBlock;
                currentBlock = 0;
            } else {
                currentBlock = 0;
            }
        }
        if (currentBlock > biggest) {
            biggest = currentBlock;
        }
        return biggest;
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

        Set<BlockInterval> freeslots = new HashSet<>();

        int startingSlot = 0;
        int currentSlot = 0;
        boolean freeSlotStarted = false;
        
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (memory.get(key) == -1 && freeSlotStarted == false) {
                currentSlot = key;
                startingSlot = key;
                freeSlotStarted = true;
            } else if (memory.get(key) == -1) {
                currentSlot = key;
            } else if (freeSlotStarted) {
                BlockInterval interval = new BlockInterval(currentSlot, startingSlot);
                System.out.println("test" + key);
                freeslots.add(interval);
                freeSlotStarted = false;
            }
        }
        
        

        return freeslots;
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
