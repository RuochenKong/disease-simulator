package edu.gmu.mason.vanilla;

import ec.util.MersenneTwisterFast;
import edu.gmu.mason.vanilla.environment.Pub;
import edu.gmu.mason.vanilla.log.Characteristics;
import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.log.State;
import org.joda.time.LocalDateTime;
import scala.None;

import java.util.ArrayList;
import java.util.List;
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
    @Skip
    private static int remainNumOfInitInfect = -1;

    // Let the agents know something about the disease
    // But the information is delayed
    @Characteristics
    private static int numKnownCases = 0;
    @Skip
    private static List<Integer> numNewCases = new ArrayList<>();
    @Skip
    private static int numTikDelay = 0;
    @Skip
    private static String currentTime;

    @Characteristics
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

    // -1: If not quarantined, Otherwise: Days been quarantined
    @State
    private double daysQuarantined;
    @Skip
    private double maxDaysQuarantined;
    @State
    private boolean isReported;
    @Characteristics
    private double normalAppetite;
    @Characteristics
    private int normalSleepLength;

    // Logging info
    @Skip
    private long infectedByAgentID;
    @Skip
    private LocalDateTime statusChangeTime;
    @Skip
    private String statusChangeLocation;
    @Skip
    private PersonMode statusChangeCheckIn;

    public InfectiousDisease(){
        this.agent = null;
        this.chanceToSpreat = 0.5;
        this.chanceBeInfected = 0.5;
        this.status = InfectionStatus.Susceptible;
        this.daysInStatus = 0;
        this.maxDaysInStatus = 0;
        this.daysFromDose = 0;
        this.vaccineStatus = VaccineStatus.Unvaccined;
        this.isReported = false;
        this.daysQuarantined = -1;
        this.maxDaysQuarantined = 0;
        this.infectedByAgentID = -1; // -1 for not yet been infected, self ID for initial random zero-patients
        this.statusChangeTime = null;
        this.statusChangeLocation = null;
        this.statusChangeCheckIn = null;
    }

    public InfectiousDisease(Person p){
        this();
        if (remainNumOfInitInfect == -1){
            double infectPer = p.getModel().params.initPercentInfectious/ (double)100;
            int numAgents = p.getModel().params.numOfAgents;
            remainNumOfInitInfect = (int) (infectPer * numAgents);
            numTikDelay = p.getModel().params.numTikDelay;
            numNewCases.add(0);
            currentTime = p.getModel().params.initialSimulationTime.toString();
        }

        this.agent = p;
        this.normalAppetite = this.agent.getFoodNeed().getAppetite();
        this.normalSleepLength = this.agent.getSleepNeed().getSleepLengthInMinutes();


        // Random rand = new Random();
        MersenneTwisterFast rand = agent.getModel().random;
        // Chance to be infected in range [0.3-0.7]
        this.chanceBeInfected = 0.3 + 0.4*rand.nextDouble();

        // Chance to spread in range [0.7-0.9]
        this.chanceToSpreat = 0.7 + 0.2*rand.nextDouble();

        this.isReported = false;
    }

    /**
     * TODO:
     *      Override *Constructor* to take an input variable of work type (or others) to change the chances.
     */

    public void setStatus(InfectionStatus status){
        this.status = status;
        this.daysInStatus = 0;
        this.statusChangeTime = agent.getModel().getSimulationTime();
        this.statusChangeLocation = agent.getLocation().toString();
        this.statusChangeCheckIn = agent.getCurrentMode();

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

        // Random rand = new Random();
        MersenneTwisterFast rand = agent.getModel().random;

        if (this.status == InfectionStatus.Susceptible)
            this.maxDaysInStatus = 0;
        // Exposed for [3-14] days change to Infectious
        if (this.status == InfectionStatus.Exposed)
            this.maxDaysInStatus = rand.nextDouble() * 11 + 3;
        // Infectious for [5-10] days change to Recovered,
        // Could bear at most [1-4] days of staying home
        if (this.status == InfectionStatus.Infectious){
            this.maxDaysInStatus = rand.nextDouble() * 4 + 5;
            this.maxDaysQuarantined = rand.nextDouble() * 3 + 1;
            this.isReported = rand.nextDouble() < agent.getModel().params.reportingProbability;

            // Increment the count at the newest tik
            if (this.isReported) numNewCases.set(numNewCases.size() - 1, numNewCases.get(numNewCases.size() - 1) + 1);
        }

        // Recovered for [1-6] months change to Susceptible
        if (this.status == InfectionStatus.Recovered)
            this.maxDaysInStatus = rand.nextDouble() * 150 + 30;
    }

    // For zero patients
    public int getRemainNumOfInitInfect(){
        return remainNumOfInitInfect;
    }

    public void setStatus(){
        setStatus(InfectionStatus.Infectious);
        this.infectedByAgentID = agent.getAgentId();
        remainNumOfInitInfect --;
    }

    // For exposed status
    public void setStatus(long agentID){
        setStatus(InfectionStatus.Exposed);
        this.infectedByAgentID = agentID;
    }

    public void setChanceToSpreat(double c2spread){
        this.chanceToSpreat = c2spread;
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

    public long getInfectedByAgentID(){
        return this.infectedByAgentID;
    }

    public LocalDateTime getStatusChangeTime(){
        return this.statusChangeTime;
    }

    public String getStatusChangeLocation(){
        return this.statusChangeLocation;
    }

    public PersonMode getStatusChangeCheckIn() {
        return this.statusChangeCheckIn;
    }

    private void incrementTikCounts(){
        String agentTime = agent.getModel().getSimulationTime().toString();
        if (agentTime.equals(currentTime)) return;
        // DEBUGGER
        System.out.println("[" + currentTime + "]:");
        System.out.println("\t Known Cases: "+numKnownCases);
        System.out.println("\t New Cases: "+numNewCases.toString());

        currentTime = agentTime;
        numNewCases.add(0);  // Add placeholder for new tik
        if (numNewCases.size() == numTikDelay + 1){
            numKnownCases += numNewCases.get(0);  // Increment the known case for one tik further
            numNewCases.remove(0); // Remove the one already counted to known cases
        }
    }

    public void incrementDays(double tikMin){
        this.daysInStatus += tikMin/(24*60);

        if (!this.vaccineStatus.equals(VaccineStatus.Unvaccined)){
            this.daysFromDose += tikMin/(24*60);
        }

        // Exposed for [3-14] days change to Infectious
        if(this.status == InfectionStatus.Exposed && daysInStatus >= maxDaysInStatus){
            setStatus(InfectionStatus.Infectious);
        }

        // Infectious for [5-10] days change to Recovered
        if(this.status == InfectionStatus.Infectious && daysInStatus >= maxDaysInStatus){
            if(this.isReported) numNewCases.set(numNewCases.size() - 1, numNewCases.get(numNewCases.size() - 1) - 1);
            setStatus(InfectionStatus.Recovered);
            this.daysQuarantined = -1;
            this.isReported = false;
        }

        // Recovered for [3-6] months change to Susceptible
        if(this.status == InfectionStatus.Recovered && daysInStatus >= maxDaysInStatus){
            setStatus(InfectionStatus.Susceptible);
        }

        if(this.daysQuarantined != -1) {
            if(this.status != InfectionStatus.Infectious){
                this.daysQuarantined = -1;
            } else if (this.daysQuarantined > this.maxDaysQuarantined){
                this.daysQuarantined = -1;
                this.maxDaysQuarantined = agent.getModel().random.nextDouble() * 2 + 3;
            } else {
                this.daysQuarantined += tikMin/(24*60);
            }
        }

        incrementTikCounts();
    }

    public boolean isReported() {
        if (this.status == InfectionStatus.Infectious)
            return isReported;
        return false;
    }

    public double getKnowCaseImpactParam(){
        int totalAgents = this.agent.getModel().params.numOfAgents;
        double param = this.agent.getModel().params.knownCaseImpactParam;
        return ((double)numKnownCases/(double)totalAgents) * param;
    }

}

