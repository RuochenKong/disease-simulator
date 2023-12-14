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

    // -1: If not quarantined, Otherwise: Days been quarantined
    @State
    private double daysQuarantined;
    @State
    private boolean isReported;

    @State
    private double normalAppetite;

    @State
    private int normalSleepLength;

    public InfectiousDisease(){
        this.agent = null;
        this.chanceToSpreat = 0.5;
        this.chanceBeInfected = 0.5;
        this.chanceToReport = 0.5;
        this.status = InfectionStatus.Susceptible;
        this.daysInStatus = 0;
        this.daysFromDose = 0;
        this.vaccineStatus = VaccineStatus.Unvaccined;
        this.isReported = false;
        this.daysQuarantined = -1;
    }

    public InfectiousDisease(Person p){
        this();
        this.agent = p;

        this.normalAppetite = this.agent.getFoodNeed().getAppetite();
        this.normalSleepLength = this.agent.getSleepNeed().getSleepLengthInMinutes();
        Random rand = new Random();

        // Chance to be infected in range [0.3-0.7]
        this.chanceBeInfected = 0.3 + 0.4*rand.nextDouble();

        // Chance to spread in range [0.7-0.9]
        this.chanceToSpreat = 0.7 + 0.2*rand.nextDouble();

        // Chance to report in range [0.3,0.9]
        this.chanceToReport = 0.3 + 0.6*rand.nextDouble();

        this.isReported = (rand.nextDouble() < this.chanceToReport);
    }

    /**
     * TODO:
     *      Override *Constructor* to take an input variable of work type (or others) to change the chances.
     */

    public void setStatus(InfectionStatus status){
        this.status = status;
        this.daysInStatus = 0;

        // System.out.println("  Before setting:");
        // System.out.println(agent.getFoodNeed().getFoodNeedInfo());

        // Eat less when get infected
        // Sleep 30-90 minutes longer
        double lower = agent.getModel().params.appetiteLowerBound;
        if (this.status == InfectionStatus.Infectious) {
            this.agent.getFoodNeed().setAppetite(Math.max(this.normalAppetite * 0.7,lower));
            this.agent.getSleepNeed().changeSleepLength(this.normalSleepLength + agent.getModel().random.nextInt(60) + 30);
        } else { // Back to normal
            this.agent.getFoodNeed().setAppetite(this.normalAppetite); //setAppetite ?
            this.agent.getSleepNeed().changeSleepLength(this.normalSleepLength);
        }

        // System.out.println("  After setting:");
        // System.out.println(agent.getFoodNeed().getFoodNeedInfo());

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
        /** TODO:
         *      If fully vaccined, cbinfected and c2spread should decrease
         */
    }

    public void setDaysFromDose(double daysFromDose){
        this.daysFromDose = daysFromDose;
    } // Might be useless

    public void setQuarantine(){
        this.daysQuarantined = 0;
    }

    public void unsetQuarantine(){
        this.daysQuarantined = -1;
    }

    public InfectionStatus getStatus() {
        return status;
    }

    public double getChanceBeInfected() {
        if (this.status == InfectionStatus.Susceptible) return chanceBeInfected;
        return 0;
    }

    public double getChanceToReport() {
        if (this.status == InfectionStatus.Infectious) return chanceToReport;
        return 0;
    }

    public double getChanceToSpreat() {
        if (this.status == InfectionStatus.Infectious) return chanceToSpreat;
        return 0;
    }

    public double getDaysFromDose() {
        return daysFromDose;
    }

    public double getDaysInStatus() {
        return daysInStatus;
    }

    public double getDaysQuarantined(){
        return daysQuarantined;
    }

    public boolean isQuarantined(){
        return daysQuarantined != -1;
    }

    public VaccineStatus getVaccineStatus() {
        return vaccineStatus;
    }

    public void receiveVaccineDose(){
        this.daysFromDose = 0;
        if(this.vaccineStatus == VaccineStatus.Unvaccined){
            this.vaccineStatus = VaccineStatus.Partial;
        } else if (this.vaccineStatus == VaccineStatus.Partial){
            if(this.daysQuarantined <= 30)
                this.vaccineStatus = VaccineStatus.Full;
        } else {
            this.vaccineStatus = VaccineStatus.Booster;
        }
    }

    public void incrementDays(double tikMin){
        this.daysInStatus += tikMin/(24*60);

        if (!this.vaccineStatus.equals(VaccineStatus.Unvaccined)){
            this.daysFromDose += tikMin/(24*60);
        }

        Random rand = new Random();

        // Explosed for [3-14] days change to Infectious
        if(this.status == InfectionStatus.Exposed){
            if (daysInStatus >= rand.nextInt(11) + 3) setStatus(InfectionStatus.Infectious);
        }

        // Infectious for [5-12] days change to Recovered
        if(this.status == InfectionStatus.Infectious){
            if (daysInStatus >= rand.nextInt(7) + 5) setStatus(InfectionStatus.Recovered);
        }

        // Recovered for [3-6] months change to Susceptible
        if(this.status == InfectionStatus.Recovered){
            if (daysInStatus >= rand.nextInt(90)+ 90) setStatus(InfectionStatus.Susceptible);
        }

        if(this.daysQuarantined != -1) {
            if(this.status != InfectionStatus.Infectious){
                this.daysQuarantined = -1;
            } else {
                this.daysQuarantined += tikMin/(24*60);
            }
        }

    }


    public boolean isReported() {
        if (this.status == InfectionStatus.Infectious)
            return isReported;
        return false;
    }
}

