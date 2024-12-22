package se.lnu.os.ht24.a2.required;

import se.lnu.os.ht24.a2.provided.data.ProcessInterval;
import se.lnu.os.ht24.a2.provided.data.StrategyType;
import se.lnu.os.ht24.a2.provided.interfaces.Memory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class MemoryImpl implements Memory {

    private final int size;
    private Dictionary<Integer, Integer> memory = new Hashtable<Integer, Integer>();
    private Dictionary<Integer, Integer> blockListAllocated = new Hashtable<Integer, Integer>();

    /**
     * This class is the memory simulation, the parameter "Size" is the size of the memory.
     */
    public MemoryImpl(int size){
        // set the size.
        this.size = size;
        // set a counter to create the memory of the same size.
        // The memory consiste of a dictionay hashTable containing the range of the memory point and the id of the block assigned to him
        // If the memory is not assigned, it get the default number of "-1"
        int counter = size - 1;
        while (0 <= counter) {
            // set the range of the memory and the default value.
            memory.put(counter, -1);
            counter = counter - 1;
        }
    }


    /**
     * This function tries to allocate the bloc in the memory.
     * @param idBlock is the id of the block.
     * @param dimension is the dimension of the block.
     * @param strategy is the strategy.
     * @return 
     * True -> Allocation succefull (or putted in a waiting list)
     * False -> error : creation of an exeption.
     */
    public boolean AllocateBlock(int idBlock, int dimension, StrategyType strategy) {
        // memoryAvaible is a counter to count the size of the block avaible.
        //CurrentMemory is the current block selected
        int memoryAvaible = 0;
        ArrayList<Integer> currentMemory = new ArrayList<Integer>();

        if (this.containsProcess(idBlock)) {
            System.out.println("Error : already exist");
            return false;
        }

        // as we uses different strategy we save the biggest and smalest avaible block of memory.
        ArrayList<Integer> biggestMemory = new ArrayList<Integer>();
        ArrayList<Integer> smalestMemory = new ArrayList<Integer>(size);
        
        // We will go throug all the memory slots.
        // as we want to go from the first memory (0) to the last (n)
        // we revert the list.
        Enumeration<Integer> id = memory.keys();
        List<Integer> list = new ArrayList<>();

        // create an array list form the enumeration.
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            list.add(key);
        }

        // revert the array list.
        ArrayList<Integer> revArrayList = new ArrayList<Integer>();
        for (int i = list.size() - 1; i >= 0; i--) {
            revArrayList.add(list.get(i));
        }

        // veryfy each memory piece avaibility.
        for (int key : revArrayList) {
            // if memory avaible : add 1 to the memory avaible and arraylist of the current block.
            // if the block is not avaible, we verify if the object have gone thru is the biggest or the smallest.
            if (memory.get(key) != -1) {
                if (memoryAvaible >= dimension) {
                    if (currentMemory.size() > biggestMemory.size()) {
                        biggestMemory.clear();
                        for (int keybig : currentMemory) {
                            biggestMemory.add(keybig);
                        }
                    }
                    if (currentMemory.size() < smalestMemory.size()) {
                        smalestMemory.clear();
                        for (int keybig : currentMemory) {
                            smalestMemory.add(keybig);
                        }
                    }
                    if (smalestMemory.size() == 0) {
                        smalestMemory.clear();
                        for (int keybig : currentMemory) {
                            smalestMemory.add(keybig);
                        }
                    }
                    System.out.println(biggestMemory);
                }
                // clear the currently selected block.
                memoryAvaible = 0;
                currentMemory.clear();
            } else if (memory.get(key) == -1) {
                memoryAvaible = memoryAvaible + 1;
                currentMemory.add(key);
            }

            // if the memory is enough to put the instruction and the strategy is the first fit.
            if (memoryAvaible >= dimension && strategy == StrategyType.FIRST_FIT) {
                // we set the memory to taken.
                for (int i: currentMemory) {
                    memory.put(i, idBlock);
                }
                // we add the block to the dictionay allocated and delete it from the unassigned block.
                blockListAllocated.put(idBlock, dimension);
                return true;
            }
        }
        // at the end of the memory, we check if the last block of memory correspond to the biggest or smallest.
        if (currentMemory.size() > biggestMemory.size() && memoryAvaible >= dimension) {
            biggestMemory = currentMemory;
        }

        if (smalestMemory.size() == 0) {
            smalestMemory = currentMemory;
        }
        if ((currentMemory.size() < smalestMemory.size() && memoryAvaible >= dimension)) {
            smalestMemory = currentMemory;
        }

        // we verify the strategy and add the block to the memory if their is enough place
        if (biggestMemory.size() >= dimension && strategy == StrategyType.WORST_FIT) {
                // we set the memory to taken.
                int count = 0;
                for (int i: biggestMemory) {
                    if (count < dimension) {
                        memory.put(i, idBlock);
                        count = count + 1;
                    }
                }
                // we add the block to the dictionay allocated and delete it from the unassigned block.
                blockListAllocated.put(idBlock, dimension);
                return true;
        }
        if (smalestMemory.size() >= dimension && strategy == StrategyType.BEST_FIT) {
                // we set the memory to taken.
                int count = 0;
                for (int i: smalestMemory) {
                    if (count < dimension) {
                        memory.put(i, idBlock);
                        count = count + 1;
                    }
                }
                // we add the block to the dictionay allocated and delete it from the unassigned block.
                blockListAllocated.put(idBlock, dimension);
                return true;
        }
        return false;
    }

    /**
     * this function unallocate the blocl.
     * @param idBlock is the block id.
     * @return False : exeption found / True : success.
     */
    public boolean unAllocate(int idBlock) {

        // if object is unassigned or is not in the assigned list, return exeption.
        if (((Hashtable<Integer, Integer>) blockListAllocated).containsKey(idBlock) == false) {
            System.out.println("Error : the block is not assigned");
            return false;
        }
        
        // Reset the memory dictionary assigned to the block.
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (memory.get(key) == idBlock) {
                memory.put(key, -1);
            }
        }
        // remove from the list
        blockListAllocated.remove(idBlock);
        return true;
    }

    @Override
    public boolean containsProcess(int blockId) {
        // TODO Replace this return statement with the method that checks if blockId is allocated in the memory

        // if the id is in the assigned list, return true.
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
    public List<Integer> processes() {
        /* TODO
            Replace this return statement with the list of blockIds of the currently allocated blocks
            in the memory. If the memory is empty, return an empty List.
         */

        List<Integer> listIds = new ArrayList<Integer>();

        // add all the ids in the allocated disctionnairy to a arraylist.
        Enumeration<Integer> id = blockListAllocated.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            listIds.add(key);
        }
        return listIds;
    }

    @Override
    public int processSize(int blockId) {
        /* TODO
            Replace this return statement with the method that returns the dimension of the block with blockId
            in the memory, 0 if it is not allocated.
         */

        // get throu all the allocated dictionairy.
        // if the blockid is registered, return it's dimension.
        Enumeration<Integer> id = blockListAllocated.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (key == blockId) {
                return blockListAllocated.get(blockId);
            }
        }
        // if block not allocated, return 0.
        return 0;
    }

    @Override
    public ProcessInterval getProcessInterval(int blockId) {
        /* TODO
            Replace this return statement with the method that returns a BlockInterval instance containing the
            lower and upper address in memory of the block with blockId. Return null if the block is not allocated
         */
        int high = 0;
        int low = 0;
        boolean allocated = false;
        Enumeration<Integer> id = memory.keys();

        // go throu all the memory, get the high memory and low using the indicator "allocated"
        // that indicate if the id has been found.
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (memory.get(key) == blockId) {
                if (allocated) {
                    low = key;
                } else {
                    low = key;
                    high = key;
                    allocated = true;
                }
            }
        }
        // if id was found, return the block interval.
        // else null.
        if (allocated) {
            return new ProcessInterval(low, high);
        } else {
            return null;
        }
    }

    /**
     * this funcrion compact the memory.
     */
    public void compact() {
        ArrayList<Integer> test = new ArrayList<Integer>();
        
        // get the block id in reverse order
        Enumeration<Integer> id2 = memory.keys();
        while (id2.hasMoreElements()) {
            int key = id2.nextElement();
            if(test.contains(memory.get(key)) == false && memory.get(key).equals(-1) == false) {
                test.add(memory.get(key));
            }
        }

        // revert the array list.
        ArrayList<Integer> revArrayList2 = new ArrayList<Integer>();
        for (int i = test.size() - 1; i >= 0; i--) {
            revArrayList2.add(test.get(i));
        }

        // reset all the memory.
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            memory.put(key, -1);
        }

        // reasign each block to the reseted memory.
        for (int key : revArrayList2) {
            int dimmension = blockListAllocated.get(key);
            blockListAllocated.remove(key);
            AllocateBlock(key, dimmension, StrategyType.FIRST_FIT);
        }
    }

    @Override
    public Set<Integer> neighboringProcesses(int blockId) {
        /* TODO
            Replace this return statement with the method that returns the Set containing the ids of all the
            contiguous blocks to the one that has blockId (min. 0 if the block is between two free portions of
              memory and max. 2 if the block is surrounded both left and right by other blocks). For no neighboring
            blocks, return an empty Set.
         */

        System.out.println("starting the methode Neighboring proccess for :" + blockId);
        Set<Integer> neighbor = new HashSet<>(); 
        
        //uses boolean to check if the lower and higher have been found.
        boolean lower = true;
        boolean high = true;
        int lowBlockId = 0;
        int highBlockId = 0;

        System.out.println("Initalization of all the basic parameters needed:");

        // check if the lower exist
        ProcessInterval block = getProcessInterval(blockId);

        if (block.getLowAddress() == 0) {
            lower = false;
        }
        // check if the neighbour with the lower memory exist.
        // if it exist, the lower neighbour is register
        else if (memory.get(block.getLowAddress() - 1) == -1) {
            lower = false;
        } else {
            lowBlockId = memory.get(block.getLowAddress() - 1);
        }

        System.out.println("lower exist : first step passed");

        // same for the higher.
        if (block.getHighAddress() == size - 1) {
            high = false;
        } else if (memory.get(block.getHighAddress() + 1) == -1) {
            high = false;
        } else {
            highBlockId = memory.get(block.getHighAddress() + 1);
        }

        //we add them in the set.
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

        
        double biggest = 0;
        double currentBlock = 0;
        double freeMemory = 0;
        
        // simple calculation with the help of the formula given.
        // here we calculate the freememory and the biggest block of free memory.
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

        if (currentBlock > biggest) {
            biggest = currentBlock;
            currentBlock = 0;
        }
        if (freeMemory == 0) {
            return 0;
        }


        double fragmentation = 1 - (biggest/freeMemory);
        if (biggest == 0) {
            return 0;
        } else {
            return fragmentation;
        }
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
    public Set<ProcessInterval> freeSlots() {

        System.out.println("-- START FREESLOT COUNT --");
        /* TODO
            Replace this return statement with the method that returns the set of BlockInterval instances
            corresponding to the free slots of the memory. Return exactly one BlockInterval per slot, make sure
            that you don't split any slot in two different intervals (e.g. if slot 0-199 is free, adding 0-99
            and 100-199 will be considered an error, while adding 0-199 is the only correct solution). If the
            memory is full, return an empty Set.
         */

        // we save all free set of memory in this hash set. 
        Set<ProcessInterval> freeslots = new HashSet<>();

        int startingSlot = 0;
        int currentSlot = 0;
        boolean freeSlotStarted = false;
        
        // we go throu all the memory and take each set, put them in the list.
        Enumeration<Integer> id = memory.keys();
        while (id.hasMoreElements()) {
            int key = id.nextElement();
            if (memory.get(key) == -1 && freeSlotStarted == false) {
                System.out.println("Start a new free block:");
                System.out.println(key + "Is the first starting slot");
                currentSlot = key;
                startingSlot = key;
                freeSlotStarted = true;
                System.out.println(startingSlot);
            } else if (memory.get(key) == -1) {
                currentSlot = key;
            } else if (freeSlotStarted) {
                System.out.println("End of the free block interval");
                System.out.println(currentSlot);
                ProcessInterval interval = new ProcessInterval(currentSlot, startingSlot);
                System.out.println(currentSlot + " " + startingSlot);
                freeslots.add(interval);
                freeSlotStarted = false;
            }
        }
        if (freeSlotStarted) {
            System.out.println("End of the free block interval");
            System.out.println(currentSlot);
            ProcessInterval interval = new ProcessInterval(currentSlot, startingSlot);
            freeslots.add(interval);
            freeSlotStarted = false;
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

        // we assume the object given is from the interface memory

        System.out.println("Starting the verification");

        System.out.println("Verification of each block:");
        if (o instanceof MemoryImpl) {
            MemoryImpl comparedMemory = (MemoryImpl) o;
            if (this.size != comparedMemory.size) {
                return false;
            }
            for (int key : this.processes()) {
                System.out.println("Verification of the block with " + key);
                if (comparedMemory.containsProcess(key) == false) {
                    System.out.println("------- CASE 1");
                    return false;
                }
                if (comparedMemory.getProcessInterval(key).getHighAddress() != this.getProcessInterval(key).getHighAddress()) {
                    System.out.println("------- CASE 2");
                    return false;
                }
                if (comparedMemory.getProcessInterval(key).getLowAddress() != this.getProcessInterval(key).getLowAddress()) {
                    System.out.println("------- CASE 3");
                    return false;
                }
                if (comparedMemory.processSize(key) != this.processSize(key)) {
                    System.out.println("------- CASE 4");
                    return false;
                }
            }
        return true;
        } else {
            System.out.println("Wrong instance");
            return false;
        }
    
    }

    @Override
    public String toString() {
        StringBuilder retStr = new StringBuilder("Memory Size = " + size + "\n");
        if(processes() != null) {
            for (int blockId : processes()) {
                ProcessInterval inter = getProcessInterval(blockId);
                retStr.append("(").append(inter.getLowAddress()).append("-").append(inter.getHighAddress()).append(")")
                        .append(" --> ").append("ID ").append(blockId).append("\n");
            }
        }
        if(freeSlots() != null) {
            for (ProcessInterval bi : freeSlots()) {
                retStr.append("(").append(bi.getLowAddress()).append("-").append(bi.getHighAddress()).append(")")
                        .append(" --> ").append("EMPTY").append("\n");
            }
        }
        return retStr.toString();
    }
}
