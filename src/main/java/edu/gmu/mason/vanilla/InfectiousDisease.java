package edu.gmu.mason.vanilla;

import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.log.State;

import java.util.Random;

/**
 * TODO:
 *  The first infected person could be set by using
 *  Manipulate functions during simulator
 *  Find it later
 */


public class InfectiousDisease implements java.io.Serializable {
    @Skip
    private Person agent;

    @State
    private InfectionStatus status;
    @State
    private double daysInStatus;
    @State
    private VaccineStatus vaccineStatus;
    @State
    private double daysFromDose;

    @State
    private double chanceToSpreat;
    @State
    private double chanceBeInfected;
    @State
    private double chanceToReport;

    @State
    private double daysQuarantined;
    @State
    private boolean isQuarantined;
    @State
    private boolean isReported;

    public InfectiousDisease(){
        this.agent = null;
        this.chanceToSpreat = 0.5;
        this.chanceBeInfected = 0.5;
        this.chanceToReport = 0.5;
        this.status = InfectionStatus.Suspectible;
        this.daysInfected = 0;
        this.daysFromDose = 0;
        this.vaccineStatus = VaccineStatus.Unvaccined;
        this.isQuarantined = false;
        this.isReported = false;
        this.daysQuarantined = -1;
    }

    public InfectiousDisease(Person p){
        this();
        this.agent = p;
    }

    public void setStatus(InfectionStatus status){
        Random rand = new Random();
        this.status = status;
        this.isReported = (status == InfectiousDisease.Infectious) ? (rand.nextDouble() < this.chanceToReport) : null;
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

    public void setVaccineStatus(VaccineStatus vaccineStatus){
        this.vaccineStatus = vaccineStatus;
    }

    public void setDaysFromDose(double daysFromDose){
        this.daysFromDose = daysFromDose;
    } // Might be useless

    public void setQuarantine(){
        this.isQuarantined = true;
        this.daysQuarantined = 0;
    }

    public void unsetQuarantine(){
        this.isQuarantined = false;
        this.daysQuarantined = -1;
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

    public double getDaysFromDose() {
        return daysFromDose;
    }

    public double getDaysInStatus() {
        return daysInStatus;
    }
    public VaccineStatue getVaccineStatus() {
        return vaccineStatus;
    }

    public void receiveVaccineDose(){
        this.daysFromDose = 0;
        switch (this.vaccineStatus){
            case VaccinceSatus.Unvaccined:
                this.vaccineStatus = VaccineSatus.Partial;
                break;
            case VaccineStatus.Partial:
                if(this.daysQuarantined <= 30)
                    this.vaccineStatus = VaccineSatus.Full;
                break;
            default:
                this.vaccineStatus = VaccineStatus.Booster;
                break;
        }
    }

    public void incrementDays(){
        this.daysInStatus += 1/288;
        this.daysFromDose += 1/288;
        if(this.isQuarantined) this.daysQuarantined += 1/288;
    }

    public boolean isQuarantined() {
        return isQuarantined;
    }

    public boolean isReported() {
        return isReported;
    }
}

