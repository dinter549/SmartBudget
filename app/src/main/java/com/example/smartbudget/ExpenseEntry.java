
package com.example.smartbudget;

import java.io.Serializable;

public class ExpenseEntry implements Serializable {
    public final double amount;
    public final long   timestamp;

    public ExpenseEntry(double amount, long timestamp) {
        this.amount    = amount;
        this.timestamp = timestamp;
    }
}
