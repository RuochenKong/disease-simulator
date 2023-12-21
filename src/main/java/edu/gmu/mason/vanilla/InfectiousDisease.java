package edu.gmu.mason.vanilla;

import edu.gmu.mason.vanilla.environment.Pub;
import edu.gmu.mason.vanilla.log.Characteristics;
import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.log.State;
import org.joda.time.LocalDateTime;
import scala.None;

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
    @Skip
    private double maxDaysInStatus;
    @Skip
    private VaccineStatus vaccineStatus;
    @Skip
    private double daysFromDose;

    @Characteristics
    private double chanceToSpreat;
    @Characteristics
    private double chanceBeInfected;
    @Characteristics
    private double chanceToReport;

    // -1: If not quarantined, Otherwise: Days been quarantined
    @State
    private double daysQuarantined;
    @State
    private boolean isReported;
    @Characteristics
    private double normalAppetite;
    @Characteristics
    private int normalSleepLength;

    /**
     * Assume one time infection, otherwise change to List.
     */
    @Skip
    private long infectedByAgentID;
    @Skip
    private LocalDateTime exposedTime;
    @Skip
    private AgentGeometry exposedLocation;
    @Skip
    private PersonMode exposedCheckIn;
    @Skip
    private AgentGeometry infectiousLocation;
    @Skip
    private PersonMode infectiousCheckIn;
    @Skip
    private LocalDateTime infectiousTime;
    @Skip
    private LocalDateTime recoverTime;

    public InfectiousDisease(){
        this.agent = null;
        this.chanceToSpreat = 0.5;
        this.chanceBeInfected = 0.5;
        this.chanceToReport = 0.5;
        this.status = InfectionStatus.Susceptible;
        this.daysInStatus = 0;
        this.maxDaysInStatus = 0;
        this.daysFromDose = 0;
        this.vaccineStatus = VaccineStatus.Unvaccined;
        this.isReported = false;
        this.daysQuarantined = -1;
        this.infectedByAgentID = -1; // -1 for not yet been infected, self ID for initial random zero-patients
        this.exposedTime = null;
        this.exposedLocation = null;
        this.exposedCheckIn = null;
        this.infectiousLocation = null;
        this.infectiousCheckIn = null;
        this.infectiousTime = null;
        this.recoverTime = null;
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
            this.agent.getFoodNeed().setAppetite(this.normalAppetite);
            this.agent.getSleepNeed().changeSleepLength(this.normalSleepLength);
        }

        // System.out.println("  After setting:");
        // System.out.println(agent.getFoodNeed().getFoodNeedInfo());

        Random rand = new Random();

        if (this.status == InfectionStatus.Susceptible)
            this.maxDaysInStatus = 0;
        // Exposed for [3-14] days change to Infectious
        if (this.status == InfectionStatus.Exposed)
            this.maxDaysInStatus = rand.nextInt(10) + rand.nextDouble() + 3;
        // Infectious for [5-10] days change to Recovered
        if (this.status == InfectionStatus.Infectious)
            this.maxDaysInStatus = rand.nextInt(4) + rand.nextDouble() + 5;
        // Recovered for [3-6] months change to Susceptible
        if (this.status == InfectionStatus.Recovered)
            this.maxDaysInStatus = rand.nextInt(89) + rand.nextDouble() + 90;
    }

    // For zero patients
    public void setStatus(LocalDateTime exposedTime){
        setStatus(InfectionStatus.Infectious);
        this.exposedTime = exposedTime;
        this.infectiousTime = exposedTime;
        this.infectedByAgentID = agent.getAgentId();
    }

    // For exposed status
    public void setStatus(LocalDateTime exposedTime, long agentID){
        setStatus(InfectionStatus.Exposed);
        this.exposedTime = exposedTime;
        this.infectedByAgentID = agentID;
        this.exposedLocation = agent.getLocation();
        this.exposedCheckIn = agent.getCurrentMode();
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

    public boolean isZeroPatient(){
        return infectedByAgentID == this.agent.getAgentId();
    }

    public long getInfectedByAgentID(){
        return this.infectedByAgentID;
    }

    public LocalDateTime getExposedTime(){
        return this.exposedTime;
    }
    
    public AgentGeometry getExposedLocation(){
        return this.exposedLocation;
    }

    public PersonMode getExposedCheckIn() {
        return this.exposedCheckIn;
    }
    public AgentGeometry getInfectiousLocation(){
        return this.infectiousLocation;
    }

    public PersonMode getInfectiousCheckIn() {return this.infectiousCheckIn;}
    public LocalDateTime getInfectiousTime(){
        return this.infectiousTime;
    }
    public LocalDateTime getRecoverTime(){
        return this.recoverTime;
    }

    public void incrementDays(double tikMin){
        this.daysInStatus += tikMin/(24*60);

        if (!this.vaccineStatus.equals(VaccineStatus.Unvaccined)){
            this.daysFromDose += tikMin/(24*60);
        }

        Random rand = new Random();

        // Exposed for [3-14] days change to Infectious
        if(this.status == InfectionStatus.Exposed && daysInStatus >= maxDaysInStatus){
            setStatus(InfectionStatus.Infectious);
            this.infectiousTime = this.agent.getSimulationTime();
            this.infectiousLocation = this.agent.getLocation();
            this.infectiousCheckIn = this.agent.getCurrentMode();
        }

        // Infectious for [5-10] days change to Recovered
        if(this.status == InfectionStatus.Infectious && daysInStatus >= maxDaysInStatus){
            setStatus(InfectionStatus.Recovered);
            this.recoverTime = this.agent.getSimulationTime();
        }

        // Recovered for [3-6] months change to Susceptible
        if(this.status == InfectionStatus.Recovered && daysInStatus >= maxDaysInStatus){
            setStatus(InfectionStatus.Susceptible);
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

