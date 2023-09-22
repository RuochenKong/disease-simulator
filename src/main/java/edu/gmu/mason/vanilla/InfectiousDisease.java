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

    public InfectiousDisease(){
        this.agent = null;
        this.chanceToSpreat = 0;
        this.chanceBeInfected = 0;
        this.chanceToReport = 0;
        this.status = InfectionStatus.ALL;
    }

    public InfectiousDisease(Person p){
        this();
        this.agent = p;
    }

    public void setStatus(InfectionStatus status){
        this.status = status;
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
}

