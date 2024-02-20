package edu.gmu.mason.vanilla;
import scala.Int;
import java.util.Random;
import sim.app.lsystem.LSystem;
import sim.util.geo.MasonGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

public class Region {

    private static int accuracy = 100;

    private WorldParameters params;

    private int regionID;
    private int population;

    private int numberOfSingleAgents;
    private int numberOfFamilyAgentsWKids;
    private int numberOfFamilyAgentsWOKids;

    private List<Double> race;
    private List<Double> ageGroup;
    private List<Double> educationLevel;

    private List<Integer> raceDist;
    private List<Integer> ageGroupDist;
    private List<Integer> educationLevelDist;

    private List<Integer> numPerRace;
    private List<Integer> numPerAgeGroup;
    private List<Integer> numPerEduLevel;

    private double isHispanic;
    private double isMale;

    private int numHispanic;
    private int numMale;
    private int medianHouseholdIncome;

    private MasonGeometry location;

    private int initiatedAgent;

    private Random rand;

    public Region(){
        this.regionID = -1;
        this.population = 0;

        this.race = new ArrayList<>();
        this.ageGroup = new ArrayList<>();
        this.educationLevel = new ArrayList<>();

        this.raceDist = new ArrayList<>();
        this.ageGroupDist = new ArrayList<>();
        this.educationLevelDist = new ArrayList<>();

        this.numPerRace = new ArrayList<>();
        this.numPerAgeGroup = new ArrayList<>();
        this.numPerEduLevel = new ArrayList<>();

        this.isHispanic = 0;
        this.isMale = 0;

        this.numHispanic = 0;
        this.numMale = 0;

        this.medianHouseholdIncome = 0;
        this.initiatedAgent = 0;

        rand = new Random(0);
    }

    public Region(int id, int pop, WorldParameters params){
        this();
        this.regionID = id;
        this.population = pop;
        this.params = params;

        double ratio = (double) pop / 1000;
        double singleNum = params.numOfSingleAgentsPer1000 * ratio;
        double familyWKidsNum = params.numOfFamilyAgentsWithKidsPer1000
                * ratio;

        numberOfSingleAgents = Math.toIntExact(Math.round(singleNum));
        numberOfFamilyAgentsWKids = Math.toIntExact(Math.round(familyWKidsNum));
        numberOfFamilyAgentsWOKids = pop - numberOfSingleAgents
                - numberOfFamilyAgentsWKids;

        // System.out.println("Region "+id +" has "+population+" agents.");
    }

    public void addRace(double percentage){
        this.race.add(percentage);
    }

    public void addAgeGroup(double percentage){
        this.ageGroup.add(percentage);
    }

    public void addEduLevel(double percentage){
        this.educationLevel.add(percentage);
    }

    private void setEachDistribution(List<Double> percents, List<Integer> dists, List<Integer> counts){
        int n = 0;
        for (int i = 0; i < percents.size(); i++){
            int numRepeat = (int)(percents.get(i) * accuracy + 0.5);
            if (n + numRepeat > accuracy) numRepeat = accuracy - n;
            n += numRepeat;
            for (int r = 0; r < numRepeat; r++) dists.add(i);
            counts.add(0);
        }
        for (int i = dists.size(); i < accuracy; i++)  dists.add(rand.nextInt(percents.size()-1)+1);
    }

    public void setDistribution(){
        // Ignore UnKown Education Level
        double refineEduProb = 1-educationLevel.get(0);
        educationLevel.set(0,0.0);
        for (int i = 0; i < educationLevel.size(); i++)
            educationLevel.set(i, educationLevel.get(i)/refineEduProb);

        setEachDistribution(race, raceDist, numPerRace);
        setEachDistribution(ageGroup, ageGroupDist, numPerAgeGroup);
        setEachDistribution(educationLevel, educationLevelDist, numPerEduLevel);
    }

    public void setIsHispanic(double prob){ this.isHispanic = prob; }
    public void setIsMale(double prob){ this.isMale = prob;}

    public void setMedianHouseholdIncome(int income){
        this.medianHouseholdIncome = income;
    }

    public void setLocation(MasonGeometry location) {
        this.location = location;
    }

    public int getRegionID(){ return this.regionID; }

    public EducationLevel newEduLevelAssigned(){
        int idx = rand.nextInt(accuracy);
        int eduIdx = educationLevelDist.get(idx);
        numPerEduLevel.set(eduIdx, numPerEduLevel.get(eduIdx) + 1);
        return EducationLevel.valueOf(eduIdx);
    }

    public AgeGroup newAgeGroupAssigned(){
        int idx = rand.nextInt(accuracy);
        int ageIdx = ageGroupDist.get(idx);
        numPerAgeGroup.set(ageIdx, numPerAgeGroup.get(ageIdx) + 1);
        return AgeGroup.valueOf(ageIdx);
    }

    public Race newRaceAssigned(){
        int idx = rand.nextInt(accuracy);
        int raceIdx = raceDist.get(idx);
        numPerRace.set(raceIdx, numPerRace.get(raceIdx) + 1);
        return Race.valueOf(raceIdx);
    }

    public boolean newGender(){ // 1 for male, 0 for female
        if (rand.nextDouble() < this.isMale){
            numMale ++;
            return true;
        }

        return false;
    }

    public boolean newHispanic(){ // 1 for hispanic, 0 for non-hispanic
        if (rand.nextDouble() < this.isHispanic){
            numHispanic ++;
            return true;
        }
        return false;
    }

    public void incrementInitiated(){
        this.initiatedAgent ++;
    }
    public boolean isRegionFull(){
        return this.initiatedAgent == this.population;
    }

    public int getPopulation(){
        return this.population;
    }

    public String getRace(){
        StringBuilder res = new StringBuilder();
        res.append("Race: ");
        for (int i = 0; i < 7; i++)
            res.append(numPerRace.get(i).toString()).append(" ");
        return res.toString();
    }

    public String getAgeGroup(){
        StringBuilder res = new StringBuilder();
        res.append("Age Group: ");
        for (int i = 0; i < 10; i++)
            res.append(numPerAgeGroup.get(i).toString()).append(" ");
        return res.toString();
    }

    public String getEduLevel(){
        StringBuilder res = new StringBuilder();
        res.append("Education Level: ");
        for (int i = 0; i < 5; i++)
            res.append(numPerEduLevel.get(i).toString()).append(" ");
        return res.toString();
    }

    public String getGender(){
        StringBuilder res = new StringBuilder();
        res.append("Male: "+numMale+", Female: "+(population-numMale));
        return res.toString();
    }

    public String getHispanic(){
        StringBuilder res = new StringBuilder();
        res.append("Hispanic: "+numHispanic+", Non-Hispanic: "+(population-numHispanic));
        return res.toString();
    }

    public int getNumberOfSingleAgents() {
        return numberOfSingleAgents;
    }

    public int getNumberOfFamilyAgentsWKids() {
        return numberOfFamilyAgentsWKids;
    }

    public int getNumberOfFamilyAgentsWOKids() {
        return numberOfFamilyAgentsWOKids;
    }

    public void clearDistCollection(){
        this.raceDist.clear();
        this.educationLevelDist.clear();
        this.ageGroupDist.clear();

        this.raceDist = null;
        this.ageGroupDist = null;
        this.educationLevelDist = null;
    }

}
