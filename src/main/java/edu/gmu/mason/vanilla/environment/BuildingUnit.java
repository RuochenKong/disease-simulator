package edu.gmu.mason.vanilla.environment;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import edu.gmu.mason.vanilla.*;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;

import edu.gmu.mason.vanilla.log.Characteristics;
import edu.gmu.mason.vanilla.log.Referenceable;
import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.log.State;
import sim.util.geo.MasonGeometry;

/**
 * General description_________________________________________________________
 * A base class to represent units inside a building.
 *
 * @author Hamdi Kavak (hkavak at gmu.edu)
 * 
 */
@Referenceable(keyMethod = "getId", keyType = Long.class)
public abstract class BuildingUnit implements java.io.Serializable {

	private static final long serialVersionUID = 1603203562957744593L;
	private long id;
	@Characteristics
	private int personCapacity;
	@Characteristics
	private int numberOfRooms;
	@Characteristics
	private int neighborhoodId;
	@Characteristics
	private double attractiveness;
	@Characteristics
	private MasonGeometry location;
	@Characteristics
	private long buildingId;
	@Characteristics
	private int blockId;
	@Characteristics
	private int blockGroupId;
	@Characteristics
	private int censusTractId;
	@State
	private int numOfAgents;

	@State
	private Map< Person, ArrayList<LocalDateTime>>infectedAgents;
	@State
	private int numOfVisits = 0;
	@State
	transient private Set<Long> agentSet;
	@State
	private Map<Long, Meeting> meetingMap;
	@Skip
	private long meetingIdIndex;
	@Skip
	private Map<Long, Visit> visitMap;
	@Skip
	private Map<Long, Double> nearestRestaurantCostMap;
	@Skip
	private Map<Long, Double> nearestPubDistanceMap;
	@Skip
	private long meetingIdIndexCounter;
	protected WorldModel model;
	@Characteristics
	protected String type;

	public BuildingUnit(long id, Building building, String type) {
		this.id = id;
		this.model = building.getWorld();
		this.buildingId = building.getId();
		this.neighborhoodId = building.getNeighborhoodId();
		this.visitMap = new TreeMap<Long, Visit>();
		this.meetingMap = new TreeMap<Long, Meeting>();
		this.meetingIdIndexCounter = 0;
		this.type = type;

		this.infectedAgents = new HashMap<Person, ArrayList<LocalDateTime>>();

		// new variable for logging
		agentSet = visitMap.keySet();
		numOfAgents = 0;
		numOfVisits = 0;
	}

	public List<Person> getCurrentAgents() {
		List<Person> agents = new ArrayList<Person>();

		for (long id : visitMap.keySet()) {
			agents.add(model.getAgent(id));
		}

		return agents;
	}

	/**
	 * This method removes the given agent from meetings and visit lists
	 * 
	 * @param agentId
	 */
	public void removeAgentPresence(long agentId) {
		for (Meeting meeting : meetingMap.values()) {
			meeting.removeParticipant(agentId);
		}

		if (visitMap.containsKey(agentId) == true) {
			visitMap.remove(agentId);
		}
		numOfAgents = visitMap.size();
	}

	protected LocalDateTime getAgentArrival(long agentId) {
		return visitMap.get(agentId).getArrivalTime();
	}

	public void agentArrives(Person agent, double visitLength) {
		visitMap.put(agent.getAgentId(), new Visit(agent.getSimulationTime(), visitLength));
		numOfAgents = visitMap.size();

		// Create a mapped pair, key = agent, and values = <arriving time, leaving time>
		if(agent.getDiseaseStatus() == InfectionStatus.Infectious){
			ArrayList<LocalDateTime> times = null;
			infectedAgents.put(agent, times = new ArrayList<LocalDateTime>());
			times.add(agent.getSimulationTime());
		}
	}

	public void agentLeaves(Person agent) {
		if (agent.getLoveNeed().meetingNow() == true) {
			Long meetingId = agent.getLoveNeed().getMeetingId();

			meetingMap.get(meetingId).removeParticipant(agent.getAgentId());
			agent.getLoveNeed().setMeetingId(null);

			// get rid of the meeting object if there is only one agents
			if (meetingMap.get(meetingId).size() == 1) {
				long lastAgentsId = meetingMap.get(meetingId).getParticipants().get(0);
				meetingMap.get(meetingId).removeParticipant(lastAgentsId);
				model.getAgent(lastAgentsId).getLoveNeed().setMeetingId(null);

				meetingMap.remove(meetingId);
			}
		}

		// Store the leaving time of an infected agent
		if(agent.getDiseaseStatus() == InfectionStatus.Infectious){
			ArrayList<LocalDateTime> times = infectedAgents.get(agent);
			if (times != null) times.add(agent.getSimulationTime());
		}


		// Check whether an infected agent exist when the agent is at the Building
		// Agent may get exposed when the infectious agents exist.
		if(agent.getDiseaseStatus() == InfectionStatus.Susceptible){
			for (Person p : infectedAgents.keySet()){
				ArrayList<LocalDateTime> times = infectedAgents.get(p);
				Visit visit = visitMap.get(agent.getAgentId());
				if (times.get(0).isBefore(visit.getArrivalTime()) &&
						(times.size() == 1 || visit.getArrivalTime().isBefore(times.get(1)))){
					Random rand = new Random();
					if (rand.nextDouble() < p.getChanceToSpreat() * agent.getChanceBeInfected() * 0.02){
						agent.beenExposed(agent.getSimulationTime(), p.getAgentId());

						/* DEBUGGER */
						// System.out.println("From Building -- [Agent "+agent.getAgentId()+"] exposed.");
						// System.out.println(agent.getCurrentDiseaseStatus());
					}
					if (agent.getDiseaseStatus() == InfectionStatus.Exposed) break;
				}
			}

		}

		visitMap.remove(agent.getAgentId());
		numOfAgents = visitMap.size();
		numOfVisits++;
	}

	private void readObject(java.io.ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		aInputStream.defaultReadObject();
		if (visitMap != null)
			agentSet = visitMap.keySet();
	}

	public Long createNewMeeting(boolean planned, Long agent1Id, Long agent2Id, LocalDateTime meetingStartTime) {
		this.meetingIdIndexCounter++;
		Long meetingId = new Long(this.meetingIdIndexCounter);
		// meetingId => Site
		Meeting meeting = new Meeting(planned, meetingStartTime, "Site-" + id + "-" + meetingId);

		meeting.addParticipant(agent1Id);
		meeting.addParticipant(agent2Id);

		meetingMap.put(meetingId, meeting);

		return meetingId;
	}

	public Meeting getMeeting(Long meetingId) {
		return meetingMap.get(meetingId);
	}

	public List<Meeting> getAllMeetings() {
		return meetingMap.values().stream().collect(Collectors.toList());
	}

	public boolean isTimeToLeaveForAgent(Person agent) {
		Visit visit = visitMap.get(agent.getAgentId());
		int minuteDiff = Minutes.minutesBetween(visit.getArrivalTime(), agent.getSimulationTime()).getMinutes();

		return minuteDiff >= visit.getVisitLength();
	}

	public MasonGeometry getLocation() {
		return location;
	}

	public void setLocation(MasonGeometry location) {
		this.location = location;
	}

	public int getNeighborhoodId() {
		return neighborhoodId;
	}

	public void setNeighborhoodId(int neighborhoodId) {
		this.neighborhoodId = neighborhoodId;
	}

	public long getId() {
		return id;
	}

	public int getPersonCapacity() {
		return personCapacity;
	}

	public void setPersonCapacity(int personCapacity) {
		this.personCapacity = personCapacity;
	}

	public int getNumberOfRooms() {
		return numberOfRooms;
	}

	public void setNumberOfRooms(int numberOfRooms) {
		this.numberOfRooms = numberOfRooms;
	}

	public double getAttractiveness() {
		return attractiveness;
	}

	public void setAttractiveness(double attractiveness) {
		this.attractiveness = attractiveness;
	}

	public int getBlockId() {
		return blockId;
	}

	public void setBlockId(int blockId) {
		this.blockId = blockId;
	}

	public int getBlockGroupId() {
		return blockGroupId;
	}

	public void setBlockGroupId(int blockGroupId) {
		this.blockGroupId = blockGroupId;
	}

	public int getCensusTractId() {
		return censusTractId;
	}

	public void setCensusTractId(int censusTractId) {
		this.censusTractId = censusTractId;
	}

	public boolean isUsable() {
		return model.getBuilding(buildingId).isUsable();
	}

	public Restaurant getNearestRestaurant(double maxCost) {
		if (nearestRestaurantCostMap == null) {
			nearestRestaurantCostMap = new LinkedHashMap<Long, Double>();
			List<Restaurant> restaurants = model.getNearestRestaurants(getLocation(), 10);

			for (Restaurant rest : restaurants) {
				nearestRestaurantCostMap.put(rest.getId(), rest.getFoodCost());
			}
		}
		for (Entry<Long, Double> entry : nearestRestaurantCostMap.entrySet()) {

			if (entry.getValue() <= maxCost) {
				return model.getRestaurant(entry.getKey());
			}
		}
		return null;
	}

	// TODO: Computational cost is high. Need to optimize.
	public Map<Pub, Double> getNearestPubDistanceMap(int numOfPubs) {
		if (nearestPubDistanceMap == null) {
			nearestPubDistanceMap = new LinkedHashMap<Long, Double>();
			List<Pub> pubs = model.getNearestPubs(getLocation(), numOfPubs);

			for (Pub pub : pubs) {
				double dist = model.getSpatialNetwork().getDistance(getLocation(), pub.getLocation());
				nearestPubDistanceMap.put(pub.getId(), dist);
			}
		}

		Map<Pub, Double> result = new LinkedHashMap<Pub, Double>();

		for (Entry<Long, Double> entry : nearestPubDistanceMap.entrySet()) {
			result.put(model.getPub(entry.getKey()), entry.getValue());
		}

		return result;
	}

	public void resetNearestPubDistanceMap() {
		nearestPubDistanceMap = null;
	}

	public void resetNearestRestaurantDistanceMap() {
		nearestRestaurantCostMap = null;
	}

}
