package se.lnu.os.ht23.a2.provided.data;

public enum StrategyType {
    FIRST_FIT {
        @Override
        public String toString() { return "First Fit"; }
    },
    BEST_FIT {
        @Override
        public String toString() { return "Best Fit"; }
    },
    WORST_FIT {
        @Override
        public String toString() { return "Worst Fit"; }
    }
}
