package se.lnu.os.ht24.a2.provided.exceptions;

import se.lnu.os.ht24.a2.provided.abstract_.Instruction;

import java.util.Objects;

public class InstructionException extends RuntimeException{

    public Class<? extends Instruction> getInstructionType() {
        return instructionType;
    }

    public int getAllocatableMemoryAtException() {
        return allocatableMemoryAtException;
    }

    private final Class<? extends Instruction> instructionType;
    private final int allocatableMemoryAtException;

    public InstructionException(Instruction instruction, int allocatableMemoryAtException) {
        super();
        this.instructionType = instruction.getClass();
        this.allocatableMemoryAtException = allocatableMemoryAtException;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instructionType, allocatableMemoryAtException);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstructionException that = (InstructionException) o;
        return allocatableMemoryAtException == that.allocatableMemoryAtException && Objects.equals(instructionType, that.instructionType);
    }

    @Override
    public String toString() {
        return "("  + instructionType.getSimpleName().charAt(0) + "ex, " + allocatableMemoryAtException + ')';
    }
}
