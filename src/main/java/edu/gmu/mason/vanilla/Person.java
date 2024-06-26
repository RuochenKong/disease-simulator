package edu.gmu.mason.vanilla;

import java.util.*;
import java.util.Map.Entry;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import edu.gmu.mason.vanilla.environment.*;
import edu.gmu.mason.vanilla.log.Characteristics;
import edu.gmu.mason.vanilla.log.ExtLogger;
import edu.gmu.mason.vanilla.log.Referenceable;
import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.log.State;
import edu.gmu.mason.vanilla.utils.*;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.network.Edge;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;


/**
 * General description_________________________________________________________
 * Main agent class containing agent properties and behaviors. This class
 * references all the need and mobility classes.
 * 
 * @author Hamdi Kavak (hkavak at gmu.edu), Joon-Seok Kim (jkim258 at gmu.edu)
 * 
 */
@Referenceable(keyMethod = "getAgentId", keyType = Long.class)
public class Person implements Steppable, java.io.Serializable {

	private static final long serialVersionUID = -8380434774552643111L;

	private final static ExtLogger logger = ExtLogger.create(Person.class);

	// a reference to the model
	@Skip
	private WorldModel model;

	// helper references
	@Skip
	PointMoveTo pointMoveTo = new PointMoveTo();
	static private GeometryFactory fact = new GeometryFactory();

	// mason references
	@State
	private AgentGeometry location;
	@Skip
	private Stoppable stoppable;
	@Skip
	private Jitter jitter;

	private long agentId;
	@State
	private int neighborhoodId;

	// bias properties -- General
	@Characteristics
	private int originRegionId; // Assigned while initiate
	@Characteristics
	private boolean isMale;
	@Characteristics
	private EducationLevel educationLevel;
	@Characteristics
	private double age;

	// bias properties -- Atl Examples
	@Characteristics
	private boolean isHispanic;
	@Characteristics
	private Race race;
	@Characteristics
	private int indiv_census_income;
	@Characteristics
	private int hh_census_income;

	// bias properties -- GZ-Tianhe Examples
	@Characteristics
	private boolean householdInProvince;

	// bias reporting
	@Skip
	public static List<String> singleBiasTypes = new ArrayList<>();
	@Skip
	public static List<List<List<Double>>> processedSingleBiasParams = new ArrayList<>();
	@Skip
	public static int numSingleBias = 0;

	@Characteristics
	public double reportingRate;
	@Characteristics
	public List<Double> reportingRates_Single;

	// Agent properties
	@State
	private PersonMode currentMode;
	@State
	private BuildingUnit currentUnit;

	private Family family;

	@Characteristics
	private AgentInterest interest;
	@State
	private LifeStatus lifeStatus;

	// strategic properties
	@Characteristics
	private double joviality;
	@Characteristics
	private double jovialityBase;
	@State
	private InfectiousDisease infectiousDisease;



	// agent needs from maslov's hierarchy of needs
	// level 1
	private FoodNeed foodNeed;
	private SleepNeed sleepNeed;
	private ShelterNeed shelterNeed;

	/**
	 *  TODO: Health Need
	 */

	// level 2
	private FinancialSafetyNeed financialSafetyNeed;
	// level 3
	@State
	private LoveNeed loveNeed;

	// agent's daily activity-related variables
	@Skip
	private AgentMobility mobility;

	@State
	private VisitReason visitReason;

	@State
	private DailyPlan todaysPlan;

	// this hashmap keeps agent's daily plans for today and tomorrow
	// old plans are deleted once after they expire.
	@Skip
	private Map<String, DailyPlan> plans;

	@Skip
	private InstructionQueue instructionQueue;

	@Skip
	private JournalRecord currentJournal = null;
	@Skip
	private boolean inInitializationMode = true;

	/**
	 * Constructor to call when creating an agent that is single. Use
	 * {@code Person(WorldModel model, long id, Family family) } for
	 * initializing agents with family.
	 * 
	 * @param model
	 * @param id
	 */
	public Person(WorldModel model, long id) { // single agent initialization
		this(model, id, null);
	}

	/**
	 * Constructor to call when creating an agent with family. Use
	 * {@code Person(WorldModel model, long id) } for initializing single
	 * agents.
	 * 
	 * @param model
	 * @param id
	 * @param family
	 */
	public Person(WorldModel model, long id, Family family) {
		this.model = model; // keeping a reference to the model for easy access
							// from methods.

		Coordinate c = new Coordinate(0, 0);// default
		this.lifeStatus = LifeStatus.Alive;
		this.agentId = id;
		this.location = new AgentGeometry(fact.createPoint(c));
		this.location.setAgent(this);
		this.location.isMovable = true;
		this.mobility = new AgentMobility(this);
		this.family = family;
		this.foodNeed = new FoodNeed(this);
		this.sleepNeed = new SleepNeed(this);
		this.shelterNeed = new ShelterNeed(this);
		this.financialSafetyNeed = new FinancialSafetyNeed(this);
		this.loveNeed = new LoveNeed(this);
		this.jitter = new Jitter((model.random.nextDouble() - 0.5) * 30,
				(model.random.nextDouble() - 0.5) * 30);
		this.plans = new HashMap<>();
		this.visitReason = VisitReason.None;
		this.instructionQueue = new InstructionQueue();

		// Adding Infectious Disease
		this.infectiousDisease = new InfectiousDisease(this);

		this.originRegionId = 0;
		this.reportingRate = 0;
		this.reportingRates_Single = new ArrayList<>();
	}

	public static void addBiasType(String newBias){
		singleBiasTypes.add(newBias);
	}

	public void setReportingRate(){
		String[] consideredBias = model.biasParams.biasConsideration.split("/");
		double numerator = model.biasParams.intercept;
		for (String biasType: consideredBias){
			if (biasType.equals("Vulnerability") && this.age >= 50) numerator *= model.biasParams.vulnerability;
			if (biasType.equals("Race") && this.race == Race.WhiteOnly) numerator *= model.biasParams.raceWhite;
			if (biasType.equals("Gender") && this.isMale) numerator *= model.biasParams.genderMale;
			if (biasType.equals("Income") && this.indiv_census_income > 70000) numerator *= model.biasParams.income;
			if (biasType.equals("Household Income") && this.hh_census_income > 100000) numerator *= model.biasParams.hh_income;

			if (biasType.equals("Hispanic") && model.biasParams.hispanic != 0 && this.isHispanic) numerator *= model.biasParams.hispanic;
			if (biasType.equals("Education") && model.biasParams.eduBachelor != 0 &&
					(this.educationLevel == EducationLevel.Bachelors || this.educationLevel == EducationLevel.Graduate))
				numerator *= model.biasParams.eduBachelor;
			if (biasType.equals("Living Area") && model.biasParams.livingArea != 0 &&
					this.getOriginRegion().getAreaPerAgent() > 30) numerator *= model.biasParams.livingArea;
			if (biasType.equals("Residency") && model.biasParams.resInProvince != 0 && this.householdInProvince)
				numerator *= model.biasParams.resInProvince;
		}
		this.reportingRate = numerator/(1+numerator);
	}

	public double getReportingRate(){
		return this.reportingRate;
	}

	public void setReportingRates_Single(){
		numSingleBias = singleBiasTypes.size();
		for (int i = 0 ; i < numSingleBias; i++) {
			String biasType = singleBiasTypes.get(i);
			List<List<Double>> pairProb = processedSingleBiasParams.get(i);

			int idxOther = -1;
			boolean found = false;

			if (biasType.equals("Age") || biasType.equals("Income") || biasType.equals("HouseHold Income")  || biasType.equals("Living Area")) {
				double compareVal = 0;
				if (biasType.equals("Age")) compareVal = this.age;
				if (biasType.equals("Income")) compareVal = (double) this.indiv_census_income / 1000.0;
				if (biasType.equals("HouseHold Income")) compareVal = (double) this.hh_census_income / 1000.0;
				if (biasType.equals("Living Area")) compareVal = this.getOriginRegion().getAreaPerAgent();
				for (int j = 0; j < pairProb.size() && !found; j++) {
					List<Double> param = pairProb.get(j);
					if (param.get(0) == -1.0) {
						idxOther = j;
						continue;
					}
					if (param.get(0) <= compareVal && param.get(1) > compareVal) {
						this.reportingRates_Single.add(param.get(2));
						found = true;
					}
				}
				if (!found) {
					double rate = (idxOther == -1) ? 0.5 : pairProb.get(idxOther).get(1);
					this.reportingRates_Single.add(rate);
				}
			} else {
				double searchIdx = -1.0;
				if (biasType.equals("Gender")) searchIdx = this.isMale ? 1.0 : 0.0;
				if (biasType.equals("Education")) searchIdx = this.educationLevel.getValue();
				if (biasType.equals("Race")) searchIdx = this.race.getValue();
				if (biasType.equals("Hispanic")) searchIdx = this.isHispanic ? 1.0 : 0.0;
				if (biasType.equals("Residency")) searchIdx = this.householdInProvince ? 1.0 : 0.0;
				for (int j = 0; j < pairProb.size() && !found; j++) {
					List<Double> param = pairProb.get(j);
					if (param.get(0) == -1.0) {
						idxOther = j;
						continue;
					}
					if (param.get(0) == searchIdx) {
						this.reportingRates_Single.add(param.get(1));
						found = true;
					}
				}
				if (!found) {
					double rate = (idxOther == -1) ? 0.5 : pairProb.get(idxOther).get(1);
					this.reportingRates_Single.add(rate);
				}
			}
		}
	}

	public List<Double> getReportingRates_Single(){
		return this.reportingRates_Single;
	}

	public void setOriginRegion(int regionId){
		this.originRegionId = regionId;
	}

	public void setRace(Race race){ this.race = race;}

	public void setHHCensusIncome(int income){this.hh_census_income = income;}

	public void setIndivCensusIncome(int income){this.indiv_census_income = income;}


	public void setGender(boolean isMale){ this.isMale = isMale; }

	public void setHispanic(boolean isHispanic) {this.isHispanic = isHispanic;}

	public void setHouseholdInProvince(boolean inProvince) {this.householdInProvince = inProvince;}


	public void initializeDiseaseStatus(InfectionStatus status, VaccineStatus vaccineStatus, double c2spread, double cbinfected){

		if (status == InfectionStatus.Infectious){
			this.infectiousDisease.setStatus();
		} else {
			this.infectiousDisease.setStatus(status);
		}

		if (vaccineStatus != null) this.infectiousDisease.setVaccineStatus(vaccineStatus);

		// Customize the chances when specified
		if (c2spread != -1) this.infectiousDisease.setChanceToSpreat(c2spread);
		if (cbinfected != -1) this.infectiousDisease.setChanceBeInfected(cbinfected);
	}

	public InfectionStatus getDiseaseStatus() {
		return this.infectiousDisease.getStatus();
	}

	public boolean isWearingMask(){ return this.infectiousDisease.isWearingMasks(); }
	public double getChanceBeInfected() {
		return this.infectiousDisease.getChanceBeInfected();
	}

	public double getChanceToSpreat(){
		return this.infectiousDisease.getChanceToSpreat();
	}

	public double getDaysFromDose() {
		return this.infectiousDisease.getDaysFromDose();
	}
	public double getDaysInDiseaseStatus() {
		return this.infectiousDisease.getDaysInStatus();
	}
	public VaccineStatus getVaccineStatus() {
		return this.infectiousDisease.getVaccineStatus();
	}

	public double getDaysQuarantined(){return this.infectiousDisease.getDaysQuarantined();}
	public void setQuarantine(){this.infectiousDisease.setQuarantine();}
	public void unsetQuarantine(){this.infectiousDisease.unsetQuarantine();}
	public boolean isQuarantined(){return this.infectiousDisease.isQuarantined();}

	public void beenExposed(LocalDateTime exposedTime, long agentId){
		this.infectiousDisease.setStatus(agentId);
	}

	public int getOriginRegionId(){return this.originRegionId;}

	public Region getOriginRegion(){return model.getRegion(this.originRegionId);}


	public String getCurrentDiseaseStatus(){
		String line = "[Agent "+agentId;
		line += "] " + getDiseaseStatus().toString() + " for " + getDaysInDiseaseStatus() + " days\n";
		line += "  Chances:"+ this.infectiousDisease.getChanceToSpreat() + " to spread,\n";
		line += "          "+ this.infectiousDisease.getChanceBeInfected() + " to be infected.";
		return line;
	}

	public void toBeTheFirstPatient(){
		initializeDiseaseStatus(InfectionStatus.Infectious, null, -1,-1);
		System.out.println("[Agent "+agentId+"] Started the disesase, with "+infectiousDisease.getRemainNumOfInitInfect()+" agents remaining.");
	}

	/**
	 * Places the agent in the assigned neighborhood.
	 */
	public void placeInNeighborhood() {

		// find a job
		financialSafetyNeed.satisfy();
		// satisfy the shelter need
		shelterNeed.satisfy();

		// put the agent to her/his home
		this.currentMode = PersonMode.AtHome;
		this.currentUnit = this.shelterNeed.getCurrentShelter();
		moveTo(this.getShelter().getLocation().geometry.getCoordinate());

		// Initialize the disease
		if (infectiousDisease.getRemainNumOfInitInfect() > 0 && model.random.nextDouble() >= (1-model.params.initPercentInfectious/(double) 70) ) toBeTheFirstPatient();

		// if the agent is has a family and a kid, find a school for the kid and
		// assign it.
		if (this.hasFamily() && this.family.haveKids()) {
			shelterNeed.assignSchool();
		}

		// create an initial daily plan for the agent.
		DailyPlan plan = planForSpecificDay(0);
		String todaysKey = DateTimeUtil.getDateString(getSimulationTime());
		plans.put(todaysKey, plan);
		inInitializationMode = false;
	}

	/**
	 * This method is executed each time tick.
	 */
	@Override
	public void step(SimState model) {
		preStepRoutines();

		foodNeed.update(); // this will update the status of the food need
		sleepNeed.update(); // this will set 'time to sleep' status if it is
							// reached sleeping time. for agents already
							// sleeping, it will check wake-up time and set the
							// status as 'awake'

		this.infectiousDisease.incrementDays(5); // update the days counts, including days from dose,
										    // days in a specific disease status, and days being quarainted.

		/**
		 * TODO: Health Need update & satisfy
		 */

		// if the agent is in transport, it will continue to be until reaches
		// next destination
		// no action on transport mode.
		if (currentMode == PersonMode.Transport) {
			mobility.transport();
			return;
		}

		// Sent the infected agent to home
		if (this.getDiseaseStatus() == InfectionStatus.Infectious
				&& !this.isQuarantined()
				&& model.random.nextDouble() > (1 - this.model.params.selfQuarantinedProbability/(double) (100*288))){
			this.infectiousDisease.setQuarantine();
			travelToHome(VisitReason.Disease);
		}

		sleepNeed.satisfy(); // checks sleeping/waking up schedule

		if (sleepNeed.getStatus() == SleepStatus.Awake) {
			// if awake, satisfy every need if not satisfied
			foodNeed.satisfy();
			// the next line fixes a bug (NullPointerException in daily plan). But, it
			// produces a different world.
			// if(isForceMoveHome && currentMode != PersonMode.Transport) {
			if (isForceMoveHome) {
				shelterNeed.vacate();
				isForceMoveHome = false;
				writeInterventionLog("MoveHomeOut", currentHomeStatus());
			}
			shelterNeed.satisfy();
			// the next line fixes a bug (NullPointerException in daily plan). But, it
			// produces a different world.
			// if(isForceQuitJob && getSimulationTime().getHourOfDay() < 19 &&
			// getSimulationTime().getHourOfDay() >= 18 && currentMode !=
			// PersonMode.Transport) {
			if (isForceQuitJob && getSimulationTime().getHourOfDay() < 19 && getSimulationTime().getHourOfDay() >= 18) {
				financialSafetyNeed.quitCurrentJob();
				isForceQuitJob = false;
				writeInterventionLog("QuitJob", currentJobStatus());
			}
			financialSafetyNeed.satisfy();
			loveNeed.satisfy();
			// updateJournalRecord();
		}

		// bury agents who already exited
		if (lifeStatus != LifeStatus.Alive) {
			this.bury();
		}
	}

	@Skip
	boolean isForceMoveHome = false;
	@Skip
	boolean isForceQuitJob = false;
	@Skip
	long lastMoveHomeIntervention = 0;
	@Skip
	long lastQuitJobIntervention = 0;

	public void enableForceMoveHome() {
		isForceMoveHome = true;
		lastMoveHomeIntervention = model.schedule.getSteps();
		// logging
		writeInterventionLog("MoveHomeRequest", currentHomeStatus());
	}

	public void enableForceQuitJob() {
		isForceQuitJob = true;
		// logging
		lastQuitJobIntervention = model.schedule.getSteps();
		writeInterventionLog("QuitJobRequest", currentJobStatus());
	}

	public String currentHomeStatus() {
		String line = lastMoveHomeIntervention + ",";
		// home
		if (shelterNeed.getCurrentShelter() != null) {
			line += shelterNeed.getCurrentShelter().getLocation().getGeometry();
		}
		line += ",";
		if (shelterNeed.getCurrentShelter() != null) {
			line += shelterNeed.getCurrentShelter().getRentalCostPerPerson();
		}
		return line;
	}

	public String currentJobStatus() {
		String line = lastQuitJobIntervention + ",";
		// work
		if (financialSafetyNeed.getJob() != null) {
			line += financialSafetyNeed.getJob().getWorkplace().getLocation().getGeometry();
		}
		line += ",";
		// wage
		if (financialSafetyNeed.getJob() != null) {
			line += financialSafetyNeed.getJob().getHourlyRate();
		}
		return line;
	}

	private void updateJournalRecord() {
		// if there is an active journal and agent's current mode (e.g., pub visit) is
		// captured
		if (currentJournal != null && model.getJournalSettings().checkMode(currentMode) == true) {
			// update the journal record
			// how many people at this place excluding me
			int people = model.getNumberOfPeopleByPlaceId(getCurrentUnit().getId()) - 1;

			currentJournal
					.setMaxNumberOfPeopleAtThePlace(Math.max(people, currentJournal.getMaxNumberOfPeopleAtThePlace()));
			currentJournal
					.setMinNumberOfPeopleAtThePlace(Math.min(people, currentJournal.getMinNumberOfPeopleAtThePlace()));

			// total people at the place
			if (getLoveNeed().meetingNow() == true) {
				long meetingId = getLoveNeed().getMeetingId();
				if (currentJournal.getEvents().containsKey(meetingId) == false) {
					currentJournal.getEvents().put(meetingId, new JournalEvent());
				}
				JournalEvent currentEvent = currentJournal.getEvents().get(meetingId);

				// first check if the agent is in meeting now
				int numOfFriends = 0, numOfColleagues = 0;
				List<Long> meetingGroupList = new ArrayList<Long>(loveNeed.getMeeting().getParticipants());
				numOfFriends = meetingGroupList.size() - 1;
				meetingGroupList.remove(this.agentId); // removing itself

				// check how many people are colleagues
				for (Long personId : meetingGroupList) {
					currentEvent.getParticipants().put(personId, true);
					if (NetworkUtil.areFriends(model.getWorkNetwork(), this.agentId, personId) == true) {
						numOfColleagues++;
					}
				}
				// total friends
				currentEvent.setMaxNumberOfFriends(Math.max(numOfFriends, currentEvent.getMaxNumberOfFriends()));
				currentEvent.setMinNumberOfFriends(Math.min(numOfFriends, currentEvent.getMinNumberOfFriends()));

				// total colleagues
				currentEvent
						.setMaxNumberOfColleauges(Math.max(numOfColleagues, currentEvent.getMaxNumberOfColleauges()));
				currentEvent
						.setMinNumberOfColleauges(Math.min(numOfColleagues, currentEvent.getMinNumberOfColleauges()));

			}
		}
	}

	private void preStepRoutines() {
		if (instructionQueue.isEmpty() == false) {
			instructionQueue.processElements(this);
		}

		if (visitReason != null && visitReason != VisitReason.None) {
			visitReason = VisitReason.None;
		}
	}

	public boolean physiologicalNeedsSatisfied() {
		return foodNeed.isSatisfied() == true
				&& shelterNeed.isSatisfied() == true
				&& sleepNeed.isSatisfied() == true;
	}

	public void travelToHome(VisitReason reason) {
		Travel travel = new Travel(getLocation(), getShelter().getLocation(),
				Double.MAX_VALUE, reason);
		mobility.beginToTransport(travel, PersonMode.AtHome, getShelter(), true);
	}

	/**
	 * The method called for rent payment. This should be called once a month,
	 * preferably first day of the month.
	 */
	public void payForShelter() {
		double amount = this.shelterNeed.getCurrentShelter().getRentalCostPerPerson();
		financialSafetyNeed.withdrawMoney(amount, ExpenseType.Shelter);
	}

	public void exitTheWorldByOrder() {
		// exitTheWorld(LifeStatus.Abandoned);
	}

	/**
	 * This method is called when the agent leaves the world.
	 * 
	 * @param finalStatus
	 *                    The reason why agent exits
	 */
	public void exitTheWorld(LifeStatus finalStatus) {
		LocalDateTime exitTime = getSimulationTime();
		this.lifeStatus = finalStatus;

		// procedures before quitting to make sure the agent has nothing
		// remained.

		// leave from the building
		if (getCurrentUnit() != null) {
			getCurrentUnit().agentLeaves(this);
		}

		// quit job and vacate the house
		if (financialSafetyNeed.isEmployed()) {
			financialSafetyNeed.quitCurrentJob();
		}

		// if there is a house, vacate it
		if (shelterNeed.isSatisfied()) {
			shelterNeed.vacate();
		}

		// notify friends that this agent is gone
		Bag nodes = model.getFriendFamilyNetwork().getEdgesOut(this.agentId);

		for (Object obj : nodes) {
			Edge edge = (Edge) obj;
			long agentIdToNotify = (long) edge.getTo();
			model.getAgent(agentIdToNotify).getLoveNeed().lostFriend();
		}

		// remove from social networks and its visualization
		model.getAgentsMap().remove(this.agentId);
		model.getFriendFamilyNetwork().removeNode(this.agentId);
		model.getWorkNetwork().removeNode(this.agentId);

		model.getVisualFriendFamilyGraph().removeNode(String.valueOf(agentId));
		model.getVisualWorkGraph().removeNode(String.valueOf(agentId));

		// remove agent presence from all buildings
		for (Pub pub : model.getAllPubs()) {
			pub.removeAgentPresence(agentId);
		}

		for (Restaurant restaurant : model.getAllRestaurants()) {
			restaurant.removeAgentPresence(agentId);
		}

		if (finalStatus == LifeStatus.Abandoned) {
			model.incrementNumberOfAbondenedAgents();
		} else if (finalStatus == LifeStatus.DiedDueStarving) {
			model.incrementNumberOfDeadAgents();
		}

		stoppable.stop();
		model.getAgentLayer().removeGeometry(location);
		logger.info("Agent #" + agentId + " exits: " + finalStatus + " - at time:" + exitTime);
	}

	// payment/earning methods

	public void getPaid(double amount) {
		financialSafetyNeed.depositMoney(amount);
	}

	public void getRentalAdjustment(double amount) {
		financialSafetyNeed.depositMoney(amount, true);
	}

	// helper methods

	public void moveTo(Coordinate c) {
		pointMoveTo.setCoordinate(c);
		location.getGeometry().apply(pointMoveTo);
		location.getGeometry().geometryChanged();
	}

	// planning methods
	public void addPlan(LocalDateTime dateTime, DailyPlan plan) {
		String dateString = DateTimeUtil.getDateString(dateTime);
		plans.put(dateString, plan);

		Iterator<Entry<String, DailyPlan>> iter = plans.entrySet().iterator();

		// clean plans that are older than today
		while (iter.hasNext()) {
			Entry<String, DailyPlan> entry = iter.next();
			if (entry.getValue().getDay()
					.isBefore(getSimulationTime().toLocalDate())) {
				iter.remove();
			}
		}
	}

	/**
	 * Returns the plan for a specific day.
	 * 
	 * @param dayDifferenceFromToday
	 * @return null if there is no plan
	 */
	public DailyPlan getPlanForSpecificDay(int dayDifferenceFromToday) {
		LocalDateTime planDay = model.getSimulationTime();

		if (dayDifferenceFromToday > 0) {
			planDay = planDay.plusDays(dayDifferenceFromToday);
		}

		String dateString = DateTimeUtil.getDateString(planDay);

		return plans.get(dateString);
	}

	/**
	 * 
	 * @param dayDifferenceFromToday
	 *                               use {@code 0} for today
	 * @return
	 */
	public DailyPlan planForSpecificDay(int dayDifferenceFromToday) {

		if (dayDifferenceFromToday < 0) { // cannot plan for past. sorry.
			return null;
		}

		DailyPlan plan = new DailyPlan();
		LocalDateTime planDay = model.getSimulationTime();

		if (dayDifferenceFromToday > 0) {
			planDay = planDay.plusDays(dayDifferenceFromToday);
		}

		plan.setDay(planDay.toLocalDate());

		/* DEBUGGER
		if(!this.getDiseaseStatus().equals(InfectionStatus.Susceptible))
			System.out.println("\n" + getCurrentDiseaseStatus());
		 */

		// if agent is employed and the day is a work day --> !reportedInfectious() &&
		if (this.financialSafetyNeed.isEmployed()
				&& this.financialSafetyNeed.getJob().isWorkDay(planDay)
				&& !this.isQuarantined()) {
			plan.setWorkDay(true);

			/* DEBUGGER
			if(this.getDiseaseStatus().equals(InfectionStatus.Infectious)){
				System.out.println("[Agent" + this.agentId + "] Infected, On work.");
			}
			*/

			// calculate wake up time
			LocalTime jobStartTime = this.financialSafetyNeed.getJob()
					.getStartTime();
			MasonGeometry origin = this.getShelter().getLocation();
			MasonGeometry destination = this.getJob().getWorkplace()
					.getLocation();
			int commuteLenghtAsTicks = model.getSpatialNetwork()
					.getDistanceAsTicks(origin, destination,
							model.getMinutePerStep(), mobility.getSpeed());

			LocalTime plannedLeaveTimeForWork = jobStartTime
					.minusMinutes(commuteLenghtAsTicks
							* model.getMinutePerStep()); // consider travel time

			plan.setLeaveTimeForWork(plannedLeaveTimeForWork);
			plan.setLeaveTimeFromWork(jobStartTime.plusHours(getJob()
					.getDailyWorkLengthHour()));

			int prepareTime = model.params.preparationTimeInMinutes;
			// If infected, need 15-30 more minutes to persuade themselves to work
			if (this.getDiseaseStatus().equals(InfectionStatus.Infectious))
				prepareTime += getModel().random.nextInt(15) + 15;
			plan.setWakeUpTime(plannedLeaveTimeForWork
					.minusMinutes(prepareTime));

			// sleep time is set according to wakeup time

			sleepNeed.setSleepStartTime(plan.getWakeUpTime().minusMinutes(
					sleepNeed.getSleepLengthInMinutes()));
		} else {
			// agent does not work tomorrow. so, let's have regular sleep time
			// set.

			/* DEBUGGER
			if(this.getDiseaseStatus().equals(InfectionStatus.Infectious)){
				System.out.println("[Agent" + this.agentId + "] Infected, Reported and Off work.");
			}
			*/

			plan.setWorkDay(false);
			plan.setWakeUpTime(sleepNeed.getSleepStartTime().plusMinutes(
					sleepNeed.getSleepLengthInMinutes()));
		}

		return plan;
	}

	public void makeFamily(boolean addKids, int totalPeople) {
		this.family = new Family(totalPeople, addKids);
	}

	public void increaseAge(double amount) {
		age += amount;
	}

	public void unjitter() {
		if (this.jitter.isApplied() == true) {
			Coordinate c = new Coordinate(location.geometry.getCoordinate().x
					- jitter.getX(),
					location.geometry.getCoordinate().y
							- jitter.getY());
			moveTo(c);
			this.jitter.setApplied(false);
		}
	}

	public void jitter() {
		if (this.jitter.isApplied() == false) {
			Coordinate c = new Coordinate(location.geometry.getCoordinate().x
					+ jitter.getX(),
					location.geometry.getCoordinate().y
							+ jitter.getY());
			moveTo(c);
			this.jitter.setApplied(true);
		}
	}

	/**
	 * Calculate damage and adjust speed of agent.
	 * 
	 * @param damage
	 */
	public void adjustSpeed(double damage) {

		// 0 <= damage <= 1
		if (damage > 1)
			damage = 1;
		else if (damage < 0)
			damage = 0;
		final double minimalMovingRate = 0.1;
		double moveRate = Math.max(minimalMovingRate, 1 - damage);
		mobility.setMoveRate(moveRate);
	}

	public void bury() {
		this.currentUnit = null;
		this.family = null;
		this.jitter = null;
		this.location = null;
		this.mobility = null;
		this.model = null;
		this.plans = null;
		this.todaysPlan = null;

		this.financialSafetyNeed.kill();
		this.financialSafetyNeed = null;

		this.foodNeed.kill();
		this.foodNeed = null;

		this.loveNeed.kill();
		this.loveNeed = null;

		this.shelterNeed.kill();
		this.shelterNeed = null;

		this.sleepNeed.kill();
		this.sleepNeed = null;
	}

	// GETTERS/SETTERS

	public long getAgentId() {
		return agentId;
	}

	public AgentGeometry getLocation() {

		if (this.jitter.isApplied() == true) {
			Coordinate c = new Coordinate(location.geometry.getCoordinate().x
					- jitter.getX(),
					location.geometry.getCoordinate().y
							- jitter.getY());

			return new AgentGeometry(fact.createPoint(c));
		}

		return location;
	}

	public AgentGeometry getAgentLocation() {
		return location;
	}

	public PersonMode getCurrentMode() {
		return currentMode;
	}

	public void setCurrentMode(PersonMode currentMode) {
		this.currentMode = currentMode;
	}

	public WorldModel getModel() {
		return model;
	}

	public AgentMobility getMobility() {
		return mobility;
	}

	public double getAge() {
		return age;
	}

	public void setAge(double age) {
		this.age = age;
	}

	public EducationLevel getEducationLevel() {
		return educationLevel;
	}

	public void setEducationLevel(EducationLevel educationLevel) {
		this.educationLevel = educationLevel;
	}

	public FoodNeed getFoodNeed() {
		return foodNeed;
	}

	public SleepNeed getSleepNeed() {
		return sleepNeed;
	}

	public ShelterNeed getShelterNeed() {
		return shelterNeed;
	}

	public FinancialSafetyNeed getFinancialSafetyNeed() {
		return financialSafetyNeed;
	}

	public Family getFamily() {
		return family;
	}

	public LoveNeed getLoveNeed() {
		return loveNeed;
	}

	public boolean hasFamily() {
		if (family == null)
			return false;
		return family.getNumberOfPeople() != 1;
	}

	public BuildingUnit getCurrentUnit() {
		return currentUnit;
	}

	public void setCurrentUnit(BuildingUnit currentUnit) {
		this.currentUnit = currentUnit;
	}

	public Stoppable getStoppable() {
		return stoppable;
	}

	public void setStoppable(Stoppable stoppable) {
		this.stoppable = stoppable;
	}

	public int getNeighborhoodId() {
		return neighborhoodId;
	}

	public void setNeighborhoodId(int neighborhoodId) {
		this.neighborhoodId = neighborhoodId;
	}

	public AgentInterest getInterest() {
		return interest;
	}

	public LifeStatus getLifeStatus() {
		return lifeStatus;
	}

	public AgentCharacteristic getCharacteristic() {
		if (joviality > 0.66) {
			return AgentCharacteristic.Sociolus;
		} else if (joviality > 0.33) {
			return AgentCharacteristic.Balancus;
		} else {
			return AgentCharacteristic.Croesus;
		}
	}

	public void setInterest(AgentInterest interest) {
		this.interest = interest;
	}

	// reference methods for some commonly used objects
	public Job getJob() {
		return this.financialSafetyNeed.getJob();
	}

	public Workplace getWorkplace() {
		return this.financialSafetyNeed.getJob().getWorkplace();
	}

	public Apartment getShelter() {
		return this.shelterNeed.getCurrentShelter();
	}

	public LocalDateTime getSimulationTime() {
		return model.getSimulationTime();
	}

	public DailyPlan getTodaysPlan() {
		todaysPlan = plans.get(DateTimeUtil.getDateString(getSimulationTime()));
		return plans.get(DateTimeUtil.getDateString(getSimulationTime()));
	}

	public double getWalkingSpeed() {
		return mobility.getWalkingSpeed();
	}

	public void setWalkingSpeed(double agentWalkingSpeed) {
		mobility.setWalkingSpeed(agentWalkingSpeed);
	}

	public void setVisitReason(VisitReason visitReason) {
		this.visitReason = visitReason;
	}

	public VisitReason getVisitReason() {
		return visitReason;
	}

	public JournalRecord getCurrentJournal() {
		return currentJournal;
	}

	public void recordANewJournal(PersonMode targetMode, BuildingUnit targetUnit, VisitReason visitReason) {
		JournalRecord record = new JournalRecord();

		record.setTravelStartTime(getSimulationTime());
		record.setTravelStartPlaceType(getCurrentMode());
		record.setTravelStartLocationId(currentUnit.getId());
		record.setIntendedTravelEndPlaceType(targetMode);
		record.setIntendedTravelEndLocationId(targetUnit.getId());

		if (visitReason.toString().startsWith("Bar")) {
			record.setPurpose("Recreation (Social Gathering)");
		} else if (visitReason == VisitReason.ComingBackFromRestaurant) {
			record.setPurpose("Coming Back From Restaurant");
		} else if (visitReason == VisitReason.Home_ComingBackFromWork || visitReason == VisitReason.Workplace_Work) {
			record.setPurpose("Work/Home Commute");
		} else if (visitReason.toString().startsWith("Home")) {
			record.setPurpose("Going Back to Home");
		} else if (visitReason == VisitReason.Restaurant_WantToEatOutside) {
			record.setPurpose("Eating");
		} else {
			record.setPurpose("None");
		}

		currentJournal = record;
	}

	public void endCurrentJournalRecord() {
		currentJournal.setCheckOutTime(model.getSimulationTime());
		currentJournal
				.setMoneyBalanceAfter(financialSafetyNeed.getAvailableBalance() + currentJournal.getMoneyOffset());
		// fixing the default value.
		if (currentJournal.getMinNumberOfPeopleAtThePlace() == Integer.MAX_VALUE) {
			currentJournal.setMinNumberOfPeopleAtThePlace(0);
		}
		// if(journalEnabled)
		writeJournalToFile(currentJournal);
		currentJournal = null;
	}

	private void writeJournalToFile(JournalRecord journal) {
		String line = model.schedule.getSteps() + "," +
				agentId + "," +
				journal.getTravelStartTime() + "," +
				journal.getTravelStartPlaceType() + "," +
				journal.getTravelStartLocationId() + "," +
				journal.getTravelEndTime() + "," +
				journal.getTravelEndLocationId() + "," +
				journal.getTravelEndPlaceType() + "," +
				journal.getIntendedTravelEndLocationId() + "," +
				journal.getIntendedTravelEndPlaceType() + "," +
				journal.getPurpose() + "," +
				journal.getCheckInTime() + "," +
				journal.getCheckOutTime() + "," +
				journal.getMinNumberOfPeopleAtThePlace() + "," +
				journal.getMaxNumberOfPeopleAtThePlace() + "," +
				journal.getMoneyBalanceBefore() + "," +
				journal.getMoneyBalanceAfter() + ",";

		if ((journal.getTravelEndPlaceType() == PersonMode.AtRestaurant
				|| journal.getTravelEndPlaceType() == PersonMode.AtRecreation) && journal.getEvents().size() > 0) {

			int i = 0;
			String eventDetails = "", eventOtherDetails = "", participantIds = "";
			for (long eventId : journal.getEvents().keySet()) {
				JournalEvent currentEvent = journal.getEvents().get(eventId);
				if (i == 0) {
					eventDetails = "Gathering " + (i + 1) + " => with others: " + currentEvent.getParticipants().size();
					eventOtherDetails = "Gathering " + (i + 1) + " => MinNumberOfFriends: "
							+ currentEvent.getMinNumberOfFriends() +
							" - MaxNumberOfFriends: " + currentEvent.getMaxNumberOfFriends() +
							" - MinNumberOfColleauges: " + currentEvent.getMinNumberOfColleauges() +
							" - MaxNumberOfColleauges: " + currentEvent.getMaxNumberOfColleauges();
					participantIds = "Gathering " + (i + 1) + " => participants: "
							+ currentEvent.getParticipants().keySet().toString().replaceAll(",", "-");
				} else {
					eventDetails += "; Gathering " + (i + 1) + " => with others: "
							+ journal.getEvents().get(eventId).getParticipants().size();
					eventOtherDetails += "; Gathering " + (i + 1) + " => MinNumberOfFriends: "
							+ currentEvent.getMinNumberOfFriends() +
							" - MaxNumberOfFriends: " + currentEvent.getMaxNumberOfFriends() +
							" - MinNumberOfColleauges: " + currentEvent.getMinNumberOfColleauges() +
							" - MaxNumberOfColleauges: " + currentEvent.getMaxNumberOfColleauges();
					participantIds += "; Gathering " + (i + 1) + " => participants: "
							+ currentEvent.getParticipants().keySet().toString().replaceAll(",", "-");
				}

				i++;
			}
			line += eventDetails + "," + participantIds + "," + eventOtherDetails;
		} else {
			line += "0,None,None";
		}

		logger.evt1(line);
	}

	public void writeExpenseToFile(double amount, ExpenseType type) {
		logger.evt2(model.schedule.getSteps() + "," + agentId + "," + getSimulationTime() + "," + type + "," + -amount
				+ "," + getLocation().getGeometry());
	}

	/**
	 * 
	 * @param amount
	 * @param incomeSource Use "Wage" if job salary, use "GiveCoin" for prescribed
	 *                     changes, use "RentAdjustment" for money refunded for rent
	 */
	public void writeIncomeToFile(double amount, String incomeSource) {
		logger.evt2(model.schedule.getSteps() + "," + agentId + "," + getSimulationTime() + "," + incomeSource + ","
				+ amount + "," + getLocation().getGeometry());
	}

	public void writeTrajectoryToFile(List<Integer> roads) {
		StringBuilder sb = new StringBuilder("{");
		int pre = Integer.MAX_VALUE;
		for (int i = 0; i < roads.size(); i++) {
			if (pre == roads.get(i))
				continue;
			sb.append(roads.get(i));
			if (i != roads.size() - 1)
				sb.append(",");
			pre = roads.get(i);
		}
		sb.append("}");
		logger.evt6(model.schedule.getSteps() + "\t" + agentId + "\t" + getSimulationTime() + "\t" + sb.toString());
	}

	public void writeAttributesToFile() {
		String line = model.schedule.getSteps() + "," + agentId + "," + getSimulationTime() + "," + age + ",";

		// home
		if (this.shelterNeed.getCurrentShelter() != null) {
			line += this.shelterNeed.getCurrentShelter().getLocation().getGeometry();
		}
		line += ",";

		// work
		if (this.financialSafetyNeed.getJob() != null) {
			line += this.financialSafetyNeed.getJob().getWorkplace().getLocation().getGeometry();
		}
		line += ",";

		// wage
		if (this.financialSafetyNeed.getJob() != null) {
			line += this.financialSafetyNeed.getJob().getHourlyRate();
		}
		line += ",";

		// rent
		if (this.shelterNeed.getCurrentShelter() != null) {
			line += this.shelterNeed.getCurrentShelter().getRentalCostPerPerson();
		}
		line += ",";

		// assets
		line += financialSafetyNeed.getAvailableBalance() + ",";

		// married
		if (family == null) {
			line += "No,0,";
		} else if (family.getNumberOfPeople() == 1) {
			line += "No,0,";
		} else {
			line += "Yes,";
			if (family.haveKids()) {
				line += "1,";
			} else {
				line += "0,";
			}
		}

		// education
		line += educationLevel;

		logger.evt3(line);
	}

	public void writeInterventionLog(String event, String additionalInfo) {
		logger.evt4(model.schedule.getSteps() + "," + agentId + "," + getSimulationTime() + "," + event + ","
				+ additionalInfo);
	}

	public InstructionQueue getInstructionQueue() {
		return instructionQueue;
	}

	public InfectiousDisease getInfectiousDisease() {
		return infectiousDisease;
	}

	public boolean isInInitializationMode() {
		return inInitializationMode;
	}

	public void setJoviality(double joviality) {
		this.joviality = joviality;
	}

	public void setJovialityBase(double jovialityBase) {
		this.jovialityBase = jovialityBase;
		this.joviality = jovialityBase;
	}

	public double getJoviality() {
		return this.joviality;
	}

	public double getJovialityBase() {
		return jovialityBase;
	}

}
