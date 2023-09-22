package edu.gmu.mason.vanilla;

import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.log.State;

public class InfectiousDisease implements java.io.Serializable {
    @Skip
    private Person agent;
    @State
    private InfectionStatus status;
    @State
    private double chanceToSpreat;
    @State
    private double chanceBeInfected;
    @State
    private double chanceToReport;
    @State
    private int daysInStatus;
    @State
    private boolean fullyVaccined;
    @States
    private int daysFromDose;
    @State
    private boolean isQuarantined;
    @State
    private boolean isReported;

    public InfectiousDisease(){
        this.agent = null;
        this.chanceToSpreat = 0;
        this.chanceBeInfected = 0;
        this.chanceToReport = 0;
        this.status = InfectionStatus.ALL;
        this.daysInfected = 0;
        this.daysFromDose = 0;
        this.fullyVaccined = false;
        this.isQuarantined = false;
    }

    public InfectiousDisease(Person p){
        this();
        this.agent = p;
    }

    public void setStatus(InfectionStatus status){
        /* TODO:
             Set isReport by RandNum < chanceReport (infected)
             Set isReport = false (other infection status)
         */
        this.status = status;
        this.daysInStatus = 0;
    }

    public void setChanceToSpreat(double c2spread){
        this.chanceToSpreat = c2spread;
    }

    public void setChanceToReport(double c2report){
        this.chanceToReport = c2report;
    }

    public void setChanceBeInfected(double cbinfected){
        this.chanceBeInfected = cbinfected;
    }

    public void setFullyVaccined(boolean isFullyVaccinced){
        this.fullyVaccined = isFullyVaccinced;
    }

    public void setDaysFromDose(int daysFromDose){
        this.daysFromDose = daysFromDose;
    }

    public void setQuarantine(boolean isQuarantined){
        this.isQuarantined = isQuarantined;
    }

    public InfectionStatus getStatus() {
        return status;
    }

    public double getChanceBeInfected() {
        return chanceBeInfected;
    }

    public double getChanceToReport() {
        return chanceToReport;
    }

    public double getChanceToSpreat() {
        return chanceToSpreat;
    }

    public int getDaysFromDose() {
        return daysFromDose;
    }

    public boolean isFullyVaccined() {
        return fullyVaccined;
    }

    public void incrementDaysInStatus(){
        this.daysInStatus += 1;
    }

    public boolean isQuarantined() {
        return isQuarantined;
    }

    public boolean isReported() {
        return isReported;
    }
}

