
package com.example.smartbudget;

import java.io.Serializable;


public class Goal implements Serializable {

    public String  name;
    public double  targetAmount;
    public double  savedAmount;
    public long    dueDate;
    public boolean completed;

    public Goal(String name, double targetAmount, long dueDate) {
        this.name         = name.trim();
        this.targetAmount = targetAmount;
        this.dueDate      = dueDate;
        this.savedAmount  = 0;
        this.completed    = false;
    }

    public double getProgress() {
        return (targetAmount == 0) ? 0 : (savedAmount / targetAmount);
    }
}
