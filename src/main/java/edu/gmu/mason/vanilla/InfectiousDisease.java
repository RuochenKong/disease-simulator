package edu.gmu.mason.vanilla;

import ec.util.MersenneTwisterFast;
import edu.gmu.mason.vanilla.environment.Pub;
import edu.gmu.mason.vanilla.log.Characteristics;
import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.log.State;
import org.joda.time.LocalDateTime;
import scala.None;

import java.util.Arrays;
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

    // Range of Lasting Days
    @Characteristics
    private static int minExposed;
    @Characteristics
    private static int maxExposed;
    @Characteristics
    private static int minInfectious;
    @Characteristics
    private static int maxInfectious;
    @Characteristics
    private static int minRecovered;
    @Characteristics
    private static int maxRecovered;
    @Characteristics
    private static int minStayingHome;
    @Characteristics
    private static int maxStayingHome;

    // Bias Reporting
    @State
    private boolean isReported;
    @Skip
    private List<Boolean> singleBiasReports;
    @Characteristics
    private String biasReportsTypes;

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

    @State
    private boolean wearingMasks;
    @Skip
    private double maskWearingLength;
    @Skip
    private int maskCheckTikCount;

    @Characteristics
    private double chanceToSpreat;
    @Characteristics
    private double chanceBeInfected;

    // -1: If not quarantined, Otherwise: Days been quarantined
    @State
    private double daysQuarantined;
    @Skip
    private double maxDaysQuarantined;

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

    // HELPER FUNCTIONS
    // Normal distribution in range [0,1], with mean = 0.5
    private double randPositiveGaussian(){
        double rand = agent.getModel().random.nextGaussian();
        rand = (rand > 3) ? 3 : rand;
        rand = (rand < -3) ? -3 : rand;
        rand /= 6;
        return rand;
    }

    // Shifted distribution with new mean and correspondingly new range
    private double randPositiveGaussian(double mean){
        if (mean == 1 || mean == 0) return mean;
        double rand = this.randPositiveGaussian();
        if (mean >  0.5){
            double tmp = (mean - 0.5)/(1-mean);
            rand = (rand + tmp)/(1+tmp);
        } else if (mean < 0.5){
            rand *= 2 * mean;
        }
        return rand;
    }

    // Check whether report with component biases
    private boolean calReport(){
        boolean report;
        Random rand = new Random();
        report =  rand.nextDouble() < agent.getReportingRate();
        return report;
    }

    // Check whether report with single bias
    private void calSingleReport(){
        Random rand = new Random();
        if (this.agent.getReportingRates_Single().size() != Person.numSingleBias)
            this.agent.setReportingRates_Single();
        while (this.singleBiasReports.size() != Person.numSingleBias)
            this.singleBiasReports.add(false);
        for (int i = 0; i < Person.numSingleBias; i++)
            if (rand.nextDouble() < this.agent.getReportingRates_Single().get(i))
                this.singleBiasReports.set(i, true);
    }

    // Reset singleBiasReports
    private void resetSingleBias(){
        while (this.singleBiasReports.size() != Person.numSingleBias)
            this.singleBiasReports.add(false);
        for (int i = 0; i < Person.numSingleBias; i++)
            this.singleBiasReports.set(i, false);
        this.biasReportsTypes = null;
    }

    // Convert boolean list of reporting to a single string
    private void setBiasReportsTypes(){
        StringBuilder ss = new StringBuilder();
        for (int i = 0 ; i < singleBiasReports.size(); i++){
            if (singleBiasReports.get(i))
                ss.append("/").append(Person.singleBiasTypes.get(i));
        }
        this.biasReportsTypes = (ss.length() > 0) ? ss.substring(1) : null;
    }

    // initializing static variables
    private void setStatics(WorldParameters params){

        String [] splits = params.exposedLasting.split("-");
        try{ minExposed = Integer.valueOf(splits[0]); } catch (Exception ignore){ minExposed = 1;}
        try{ maxExposed = Integer.valueOf(splits[1]); } catch (Exception ignore){ maxExposed = 5;}

        splits = params.infectiousLasting.split("-");
        try{ minInfectious = Integer.valueOf(splits[2]); } catch (Exception ignore){ minInfectious = 5;}
        try{ maxInfectious = Integer.valueOf(splits[3]); } catch (Exception ignore){ maxInfectious = 8;}

        splits = params.recoveredLasting.split("-");
        try{ minRecovered = Integer.valueOf(splits[4]); } catch (Exception ignore){ minRecovered = 30;}
        try{ maxRecovered = Integer.valueOf(splits[5]); } catch (Exception ignore){ maxRecovered = 180;}

        splits = params.stayingHome.split("-");
        try{ minStayingHome = Integer.valueOf(splits[6]); } catch (Exception ignore){ minStayingHome = 1;}
        try{ maxStayingHome = Integer.valueOf(splits[7]); } catch (Exception ignore){ maxStayingHome = 4;}

        double infectPer = params.initPercentInfectious/ (double)100;
        int numAgents =  params.numOfAgents;
        remainNumOfInitInfect = (int) (infectPer * numAgents);
        numTikDelay = params.numTikDelay;
        currentTime = params.initialSimulationTime.toString();
        numNewCases.add(0);

    }

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
        this.wearingMasks = false;
        this.maskWearingLength = 0;
        this.singleBiasReports = new ArrayList<>();
        for (int i = 0; i < Person.numSingleBias; i++) this.singleBiasReports.add(false);
        this.biasReportsTypes = null;

        Random rand = new Random();
        this.maskCheckTikCount = rand.nextInt(15)-6;

    }

    public InfectiousDisease(Person p){
        this();

        if (remainNumOfInitInfect == -1){
            setStatics(p.getModel().params);
        }

        this.agent = p;
        this.normalAppetite = this.agent.getFoodNeed().getAppetite();
        this.normalSleepLength = this.agent.getSleepNeed().getSleepLengthInMinutes();

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
        // Exposed for [1-5] days change to Infectious
        if (this.status == InfectionStatus.Exposed)
            this.maxDaysInStatus = rand.nextDouble() * (maxExposed-minExposed) + minExposed;
        // Infectious for [5-8] days change to Recovered,
        // Could bear at most [1-4] days of staying home
        if (this.status == InfectionStatus.Infectious){
            this.maxDaysInStatus = rand.nextDouble() * (maxInfectious-minInfectious) + minInfectious;
            this.maxDaysQuarantined = rand.nextDouble() * (maxStayingHome-minStayingHome) + minStayingHome;

            this.isReported = this.calReport();
            this.calSingleReport();
            this.setBiasReportsTypes();

            // Increment the count at the newest tik
            if (this.isReported) numNewCases.set(numNewCases.size() - 1, numNewCases.get(numNewCases.size() - 1) + 1);
        }

        // Recovered for [1-6] months change to Susceptible
        if (this.status == InfectionStatus.Recovered)
            this.maxDaysInStatus = rand.nextDouble() * (maxRecovered-minRecovered) + minRecovered;
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
        if (this.status == InfectionStatus.Susceptible){
            if (!this.wearingMasks) return this.chanceBeInfected;
            double effectivity = randPositiveGaussian(agent.getModel().params.avgMaskEffectivity);
            return (1-effectivity) * this.chanceBeInfected;
        }
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

        /*  DEBUGGER
        System.out.println("[" + currentTime + "]:");
        System.out.println("\t Known Cases: "+numKnownCases);
        System.out.println("\t New Cases: "+numNewCases.toString());
        */

        currentTime = agentTime;
        numNewCases.add(0);  // Add placeholder for new tik
        if (numNewCases.size() == numTikDelay + 1){
            numKnownCases += numNewCases.get(0);  // Increment the known case for one tik further
            numNewCases.remove(0); // Remove the one already counted to known cases
        }
    }

    public void incrementDays(double tikMin){
        this.daysInStatus += tikMin/(24*60);
        incrementTikCounts();

        if (this.wearingMasks ){

            if (randPositiveGaussian(0.65) < 0.5 && this.maskWearingLength>agent.getModel().params.minMaskWearingLength){
                System.out.println("[Agent "+agent.getAgentId()+"] end wearing masks at time "+agent.getSimulationTime());
                this.wearingMasks = false;
                this.maskWearingLength = 0;
            } else {
                this.maskWearingLength += tikMin;
            }
        }

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
            this.resetSingleBias();
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

        if (this.status == InfectionStatus.Susceptible){
            if ( maskCheckTikCount > 0){
                this.maskCheckTikCount --;
            } else {
                if (!wearingMasks){
                    double chanceWearingMask = this.getKnowCaseImpactParam();
                    if (agent.getModel().random.nextDouble() < chanceWearingMask){

                        System.out.println("[Agent "+agent.getAgentId()+"] start wearing masks "+agent.getSimulationTime());
                        this.wearingMasks = true;
                        this.maskCheckTikCount =12;
                    }
                  }
            }
        } else if (this.wearingMasks) {
            this.wearingMasks = false;
            this.maskWearingLength = 0;
            this.maskCheckTikCount = 12;
        }
    }

    public boolean isWearingMasks(){
        return this.wearingMasks;
    }

    public boolean getCompReport() {
        if (this.status == InfectionStatus.Infectious)
            return isReported;
        return false;
    }

    public String getSingleBiasReports(){
        return this.biasReportsTypes;
    }

    public double getKnowCaseImpactParam(){
        int totalAgents = this.agent.getModel().params.numOfAgents;
        double threshold = this.agent.getModel().params.maskWearingThreshold;
        double param = this.agent.getModel().params.knownCaseImpactParam;
        double percentageInfectious = (double)numKnownCases/(double)totalAgents;
        if (percentageInfectious < threshold) return percentageInfectious/threshold*param;
        return param;
    }

}