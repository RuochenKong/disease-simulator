package edu.gmu.mason.vanilla;
import scala.Int;

import java.util.*;

import sim.app.lsystem.LSystem;
import sim.util.geo.MasonGeometry;

public class Region {

    private static final int accuracy = 100;

    private int regionID;
    private double perPopulation;
    private int population;
    private MasonGeometry location;

    private int initiatedAgent;

    private int numberOfSingleAgents;
    private int numberOfFamilyAgentsWKids;
    private int numberOfFamilyAgentsWOKids;
    private Random rand;

    // General
    private List<Double> ageGroup;
    private List<Integer> numPerAgeGroup;

    private List<Double> educationLevel;
    private List<Integer> numPerEduLevel;

    private double isMale;
    private int numMale;

    // Atl & San-Fran
    private List<Double> race;
    private List<Integer> numPerRace;

    private static List<Integer> incomeRanges = Arrays.asList(0,10,30,60,100,150,200);
    private List<Double> incomeLevel;
    private List<Integer> numPerIncomeLevel;

    private static List<Integer> indivIncomeRanges = Arrays.asList(0,10,25,50,75,100);
    private List<Double> indivIncomeLevel;
    private List<Integer> numPerIndivIncomeLevel;

    private double isHispanic;
    private int numHispanic;

    // GZ-Tianhe
    private double areaPerAgent;

    private double inProvince;
    private int numInProvince;


    public Region(){
        this.regionID = -1;
        this.perPopulation = 0;
        this.population = 0;

        this.initiatedAgent = 0;

        rand = new Random(0);

        // General
        this.ageGroup = new ArrayList<>();
        this.numPerAgeGroup = new ArrayList<>();

        this.educationLevel = new ArrayList<>();
        this.numPerEduLevel = new ArrayList<>();

        this.isMale = 0;
        this.numMale = 0;

        // Atl & San-Fran
        this.race = new ArrayList<>();
        this.numPerRace = new ArrayList<>();

        this.incomeLevel = new ArrayList<>();
        this.numPerIncomeLevel = new ArrayList<>();

        this.indivIncomeLevel = new ArrayList<>();
        this.numPerIndivIncomeLevel = new ArrayList<>();


        this.isHispanic = 0;
        this.numHispanic = 0;

        // GZ-Tianhe
        this.areaPerAgent = 0;

        this.inProvince = 1;
        this.numInProvince = 0;
    }

    public Region(int id, double perPop){
        this();
        this.regionID = id;
        this.perPopulation = perPop;
    }

    public double getPerPopulation() {
        return perPopulation;
    }

    public void setPopulation(int pop){
        this.population = pop;
    }

    public void calNumAgents(int numOfSingleAgentsPer1000, int numOfFamilyAgentsWithKidsPer1000){

        double ratio = (double) this.population / 1000;
        double singleNum = numOfSingleAgentsPer1000 * ratio;
        double familyWKidsNum = numOfFamilyAgentsWithKidsPer1000
                * ratio;

        numberOfSingleAgents = Math.toIntExact(Math.round(singleNum));
        numberOfFamilyAgentsWKids = Math.toIntExact(Math.round(familyWKidsNum));
        numberOfFamilyAgentsWOKids = this.population - numberOfSingleAgents
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

    public void addIncomeLevel(double percentage){
        this.incomeLevel.add(percentage);
    }

    public void addIndivIncomeLevel(double percentage){
        this.indivIncomeLevel.add(percentage);
    }

    public void setIsHispanic(double prob){ this.isHispanic = prob; }
    public void setIsMale(double prob){ this.isMale = prob;}
    public void setInProvince(double inProvince){ this.inProvince = inProvince;}

    public void setAreaPerAgent(double AreaPerAgent){this.areaPerAgent = areaPerAgent;}

    public void setLocation(MasonGeometry location) {
        this.location = location;
    }

    public int getRegionID(){ return this.regionID; }

    private int getRandIdxWithDist(List<Double> probs){
        double cumProb = 0;
        double randVal = rand.nextDouble();
        for (int i = 0; i <probs.size(); i++){
            cumProb += probs.get(i);
            if (randVal < cumProb) return i;
        }
        return -1;
    }

    public void initCounts(){
        for (int i = 0; i < ageGroup.size(); i++) numPerAgeGroup.add(0);
        for (int i = 0; i < educationLevel.size(); i++) numPerEduLevel.add(0);
        for (int i = 0; i < race.size(); i++) numPerRace.add(0);
        for (int i = 0; i < incomeLevel.size(); i++) numPerIncomeLevel.add(0);
        for (int i = 0; i < indivIncomeLevel.size(); i++) numPerIndivIncomeLevel.add(0);
    }

    public EducationLevel newEduLevelAssigned(){
        int eduIdx = this.getRandIdxWithDist(educationLevel);
        if (eduIdx == -1) return null;
        numPerEduLevel.set(eduIdx, numPerEduLevel.get(eduIdx) + 1);
        return EducationLevel.valueOf(eduIdx);
    }

    public AgeGroup newAgeGroupAssigned(){
        int ageIdx = this.getRandIdxWithDist(ageGroup);
        if (ageIdx == -1) return null;
        numPerAgeGroup.set(ageIdx, numPerAgeGroup.get(ageIdx) + 1);
        return AgeGroup.valueOf(ageIdx);
    }

    public Race newRaceAssigned(){
        int raceIdx = this.getRandIdxWithDist(race);
        if (raceIdx == -1) return null;
        numPerRace.set(raceIdx, numPerRace.get(raceIdx) + 1);
        return Race.valueOf(raceIdx);
    }

    public int newIncomeAssigned(){
        if (this.incomeLevel.size() == 0) return -1;
        int incomeIdx = this.getRandIdxWithDist(incomeLevel);
        numPerIncomeLevel.set(incomeIdx, numPerIncomeLevel.get(incomeIdx) + 1);

        int lowerBound = incomeRanges.get(incomeIdx) * 1000;
        int upperBound = (incomeIdx + 1 < incomeRanges.size()) ? incomeRanges.get(incomeIdx+1) * 1000: 1000000;
        return lowerBound + rand.nextInt(upperBound-lowerBound);
    }

    public int newIndivIncomeAssigned(){
        if (this.indivIncomeLevel.size() == 0) return -1;
        int incomeIdx = this.getRandIdxWithDist(indivIncomeLevel);
        numPerIndivIncomeLevel.set(incomeIdx, numPerIndivIncomeLevel.get(incomeIdx) + 1);

        int lowerBound = indivIncomeRanges.get(incomeIdx) * 1000;
        int upperBound = (incomeIdx + 1 < indivIncomeRanges.size()) ? indivIncomeRanges.get(incomeIdx+1) * 1000: 500000;
        return lowerBound + rand.nextInt(upperBound-lowerBound);
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

    public boolean newInProvince(){
        if (rand.nextDouble() < this.inProvince){ // 1 for in province, 0 for out
            numInProvince ++;
            return true;
        }
        return false;
    }

    public void incrementInitiated(){
        this.initiatedAgent ++;
    }

    public int getPopulation(){
        return this.population;
    }

    private String getListElements(String attr, List<Integer> nums){
        if (nums.size() == 0) return null;
        StringBuilder res = new StringBuilder();
        res.append("# of ").append(attr);
        for (Integer num : nums) res.append(num.toString()).append(" ");
        return res.toString();
    }

    public String getAgeGroup(){
        return this.getListElements("Age Group: ", numPerAgeGroup);
    }

    public String getEduLevel(){
        return this.getListElements("Education Level: ", numPerEduLevel);
    }

    public String getRace(){
        return this.getListElements("Race: ", numPerRace);
    }

    public String getIncomeLevel(){
        return this.getListElements("Income Level: ", numPerIncomeLevel);
    }

    public String getIndivIncomeLevel(){
        return this.getListElements("Individual Income Level: ", numPerIndivIncomeLevel);
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

    public String getInProvince(){
        StringBuilder res = new StringBuilder();
        res.append("HH Reg In: "+numInProvince+", Out: "+(population-numInProvince));
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

    public MasonGeometry getLocation() {return this.location;}

    public double getAreaPerAgent() {return this.areaPerAgent;}
}
