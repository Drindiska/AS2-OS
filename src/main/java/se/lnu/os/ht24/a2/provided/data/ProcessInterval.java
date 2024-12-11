package se.lnu.os.ht24.a2.provided.data;

import java.util.Objects;

public class ProcessInterval {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInterval that = (ProcessInterval) o;
        return lowAddress == that.lowAddress && highAddress == that.highAddress;
    }

    public ProcessInterval(int lowAddress, int highAddress) {
        if(lowAddress > highAddress){
            throw new RuntimeException("Interval ends are incorrect");
        }
        this.lowAddress = lowAddress;
        this.highAddress = highAddress;
    }

    public int getLowAddress() {
        return lowAddress;
    }

    private final int lowAddress;

    public int getHighAddress() {
        return highAddress;
    }

    private final int highAddress;

    @Override
    public int hashCode() {
        return Objects.hash(lowAddress, highAddress);
    }
}
