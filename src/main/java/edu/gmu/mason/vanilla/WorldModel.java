package edu.gmu.mason.vanilla;

import java.awt.Color;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
// import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkDGS;
import org.graphstream.ui.view.Viewer;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import at.granul.mason.collector.Collector;
import at.granul.mason.collector.DataCollector;
import edu.gmu.mason.vanilla.DailyRoutines.RoutineType;
import edu.gmu.mason.vanilla.db.Cdf;
import edu.gmu.mason.vanilla.db.Column;
import edu.gmu.mason.vanilla.environment.Apartment;
import edu.gmu.mason.vanilla.environment.Building;
import edu.gmu.mason.vanilla.environment.BuildingType;
import edu.gmu.mason.vanilla.environment.BuildingUnit;
import edu.gmu.mason.vanilla.environment.Census;
import edu.gmu.mason.vanilla.environment.CensusData;
import edu.gmu.mason.vanilla.environment.Classroom;
import edu.gmu.mason.vanilla.environment.DayOfWeek;
import edu.gmu.mason.vanilla.environment.Job;
import edu.gmu.mason.vanilla.environment.NeighborhoodComposition;
import edu.gmu.mason.vanilla.environment.Pub;
import edu.gmu.mason.vanilla.environment.Restaurant;
import edu.gmu.mason.vanilla.environment.SpatialNetwork;
import edu.gmu.mason.vanilla.environment.Workplace;
import edu.gmu.mason.vanilla.log.CdfFlatFormatterForRelation;
import edu.gmu.mason.vanilla.log.CdfMapper;
import edu.gmu.mason.vanilla.log.CdfMapperBuilder;
import edu.gmu.mason.vanilla.log.CdfSchemaFormatter;
import edu.gmu.mason.vanilla.log.CdfValueFormatter;
import edu.gmu.mason.vanilla.log.Characteristics;
import edu.gmu.mason.vanilla.log.DateTimeTypeAdapter;
import edu.gmu.mason.vanilla.log.ExtLogger;
import edu.gmu.mason.vanilla.log.GsonCsvSchemaFormatter;
import edu.gmu.mason.vanilla.log.GsonCsvValueFormatter;
import edu.gmu.mason.vanilla.log.GsonFormatter;
import edu.gmu.mason.vanilla.log.IterativeLogSchedule;
import edu.gmu.mason.vanilla.log.LocalDateTimeTypeAdapter;
import edu.gmu.mason.vanilla.log.LocalDateTypeAdapter;
import edu.gmu.mason.vanilla.log.LocalTimeTypeAdapter;
import edu.gmu.mason.vanilla.log.LogSchedule;
import edu.gmu.mason.vanilla.log.MasonGeometryTypeAdapter;
import edu.gmu.mason.vanilla.log.OutputFormatter;
import edu.gmu.mason.vanilla.log.ReferenceTypeAdapter;
import edu.gmu.mason.vanilla.log.ReflectionValueExtractor;
import edu.gmu.mason.vanilla.log.ReservedLogChannels;
import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.log.State;
import edu.gmu.mason.vanilla.log.SupplierExtractor;
import edu.gmu.mason.vanilla.utils.CollectionUtil;
import edu.gmu.mason.vanilla.utils.ColorUtils;
import edu.gmu.mason.vanilla.utils.Exclusion;
import edu.gmu.mason.vanilla.utils.GeoUtils;
import edu.gmu.mason.vanilla.utils.Manipulation;
import edu.gmu.mason.vanilla.utils.ManipulationLoader;
import edu.gmu.mason.vanilla.utils.MasterScheduler;
import edu.gmu.mason.vanilla.utils.SimulationEvent;
import edu.gmu.mason.vanilla.utils.SimulationTimeStepSetting;
import edu.gmu.mason.vanilla.utils.StringUtils;
import edu.gmu.mason.vanilla.utils.DateTimeUtil;
import edu.gmu.mason.vanilla.utils.EventSchedule;
import scala.Int;
import sim.engine.MakesSimState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.geo.GeomVectorField;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;

/**
 * General description_________________________________________________________
 * This is the model class that contains high level methods such as agent
 * creation/initialization, environment objects creation/initialization, global
 * methods that are commonly used by agents, data logging methods and so on.
 * 
 * @author Hamdi Kavak (hkavak at gmu.edu), Joon-Seok Kim (jkim258 at gmu.edu)
 * 
 */
@SuppressWarnings({ "unused", "serial", "unchecked", "rawtypes" })
public class WorldModel extends SimState {

	// a public reference to simulation parameters
	public WorldParameters params;
	public BiasParameters biasParams;

	public BiasSingleParameters biasSingleParams;

	private static final long serialVersionUID = -7358991191225853449L;

	// java utils
	private final static ExtLogger logger = ExtLogger.create(WorldModel.class);

	// predefined ordering for MASON schedule
	public static final int STEP_BEGIN_PRIORITY = Integer.MIN_VALUE + 10;
	public static final int INTERVENTION_PRIORITY = Integer.MIN_VALUE + 20;
	public static final int PRE_EVENT_PRIORITY = Integer.MIN_VALUE + 30;
	public static final int AGENT_PRIORITY = 0;
	public static final int POST_EVENT_PRIORITY = Integer.MAX_VALUE - 40;
	public static final int DATA_COLLECTION_PRIORITY = Integer.MAX_VALUE - 30;
	public static final int SPATIAL_INDEX_UPDATING_PRIORITY = Integer.MAX_VALUE - 20;
	public static final int LOGGING_PRIORITY = Integer.MAX_VALUE - 10;

	// just for visualization and execution time estimation
	private int day = 0;
	private DateTimeUtil timeUtil = new DateTimeUtil();
	private long simulationSeed;
	private int numOfAbondenedAgents = 0;
	private int numOfDeadAgents = 0;

	// geography components/settings
	public static final int WIDTH = 800;
	public static final int HEIGHT = 800;
	private GeomVectorField agentLayer = new GeomVectorField(WIDTH, HEIGHT);
	private SpatialNetwork spatialNetwork = new SpatialNetwork(WIDTH, HEIGHT);

	// references to agents/objects
	private Map<Integer, List<Building>> neighborhoodBuildingMap;
	private Map<Long, Person> agents;
	private Map<Long, Building> buildings;
	private Map<Long, Classroom> classrooms;
	private Map<Long, Apartment> apartments;
	private Map<Long, Workplace> workplaces;
	private Map<Long, Job> jobs;
	private Map<Long, Restaurant> restaurants;
	private Map<Long, Pub> pubs;
	private Map<Integer, Region> regions;
	private List<Integer> regionsIds;
	private Map<Integer, List<Long>> regionResidentialMap;

	private Map<Integer, List<Long>> regionApartmentMap;
	private List<BuildingUnit> buildingUnitsToSupply;

	// social networks
	private Network friendFamilyNetwork = new Network(true);
	private Network workNetwork = new Network(true);

	// data collection variable used to capture quantities of interests
	private QuantitiesOfInterest quantitiesOfInterest;

	// All reserved logging matter
	private ReservedLogChannels reservedLog;

	private long agentId = 0;
	// Graphs for social network visualization
	private transient Graph visualFriendFamilyGraph;
	private transient Graph visualWorkGraph;
	// Viewer
	private transient Viewer friendFamilyViewer;
	// Files to store the social graph
	private transient FileSink friendFamilyGraphSink;
	private transient FileSink workGraphSink;
	// Paths for the sink files
	private String friendFamilyGraphSinkPath = "FriendFamilyGraph.dgs";
	private String workGraphSinkPath = "WorkGraph.dgs";
	// Manipulation scheduler
	private MasterScheduler<Manipulation> manipulationScheduler = null;
	private MasterScheduler<LogSchedule> logScheduler = null;
	private MasterScheduler<EventSchedule> eventScheduler = null;
	private Object[][] latestBarStatsData = { { 1, Color.BLACK, Color.BLACK,
			Color.BLACK, 0.00, 0.0, 0 } };

	/*
	public WorldModel(long seed, WorldParameters params) throws IOException,
			Exception {
		super(seed);
		manipulationScheduler = new MasterScheduler<Manipulation>();
		logScheduler = new MasterScheduler<LogSchedule>();
		eventScheduler = new MasterScheduler<EventSchedule>();
		this.params = params;
		timeUtil.addEventTime(SimulationEvent.SimulationStart, new DateTime());
		simulationSeed = seed;
		spatialNetwork.loadMapLayers(params.maps, "walkways.shp",
				"buildings.shp", "buildingUnits.shp", params.regions);

		initRegion();
		initPlaces();
		GeoUtils.alignMBRs(spatialNetwork.getAllLayers());

		initVisualGraph();
		reservedLog = new ReservedLogChannels(this);
		startDataCollectionForQoIs();
	}
	*/

	private void collectSingleBiasTypes() throws Exception {
		if (!this.biasSingleParams.activate) return;
		int idxColon;
		String prefix;
		if (!this.biasSingleParams.ageProb.equals("")){
			Person.addBiasType("Age");
			String[] paramSplit = biasSingleParams.ageProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				if (prefix.equals("other")){
					procParam.add(-1.0);
				} else {
					int idxDash = prefix.indexOf("-");
					if (idxDash == -1) throw new Exception("Invalid Age Probability Config");
					double low = Double.parseDouble(prefix.substring(1,idxDash));
					double high = Double.parseDouble(prefix.substring(idxDash+1,prefix.length()-1));
					if (low >= high || high <= 0) throw new Exception("Invalid Age Probability Config");
					procParam.add(low);
					procParam.add(high);
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}

		if (!this.biasSingleParams.genderProb.equals("")) {
			Person.addBiasType("Gender");
			String[] paramSplit = biasSingleParams.genderProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				if (prefix.equals("Male")){
					procParam.add(1.0);
				} else if (prefix.equals("Female")){
					procParam.add(0.0);
				} else  {
					throw new Exception("Invalid Gender Probability Config");
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}

		if (!this.biasSingleParams.eduProb.equals("")) {
			Person.addBiasType("Education");
			String[] paramSplit = biasSingleParams.eduProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				switch (prefix) {
					case "Unknown":
						procParam.add(0.0);
						break;
					case "Low":
						procParam.add(1.0);
						break;
					case "HighSchoolOrCollege":
						procParam.add(2.0);
						break;
					case "Bachelors":
						procParam.add(3.0);
						break;
					case "Graduate":
						procParam.add(4.0);
						break;
					case "other":
						procParam.add(-1.0);
						break;
					default:
						throw new Exception("Invalid Education Probability Config");
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}

		if (!this.biasSingleParams.incomeProb.equals("")) {
			Person.addBiasType("Income");
			String[] paramSplit = biasSingleParams.incomeProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				if (prefix.equals("other")){
					procParam.add(-1.0);
				} else {
					int idxDash = prefix.indexOf("-");
					if (idxDash == -1) throw new Exception("Invalid Income Probability Config");
					double low = Double.parseDouble(prefix.substring(1,idxDash));
					String highStr = prefix.substring(idxDash+1,prefix.length()-1);
					double high = (highStr.equals("Inf")) ? Double.MAX_VALUE : Double.parseDouble(highStr);
					if (low >= high || high <= 0) throw new Exception("Invalid Income Probability Config");
					procParam.add(low);
					procParam.add(high);
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}

		if (!this.biasSingleParams.hhIncomeProb.equals("")) {
			Person.addBiasType("Household Income");
			String[] paramSplit = biasSingleParams.hhIncomeProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				if (prefix.equals("other")){
					procParam.add(-1.0);
				} else {
					int idxDash = prefix.indexOf("-");
					if (idxDash == -1) throw new Exception("Invalid Household Income Probability Config");
					double low = Double.parseDouble(prefix.substring(1,idxDash));
					String highStr = prefix.substring(idxDash+1,prefix.length()-1);
					double high = (highStr.equals("Inf")) ? Double.MAX_VALUE : Double.parseDouble(highStr);
					if (low >= high || high <= 0) throw new Exception("Invalid Household Income Probability Config");
					procParam.add(low);
					procParam.add(high);
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}

		if (!this.biasSingleParams.raceProb.equals("")) {
			Person.addBiasType("Race");
			String[] paramSplit = biasSingleParams.raceProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				switch (prefix) {
					case "White":
						procParam.add(0.0);
						break;
					case "Black":
						procParam.add(1.0);
						break;
					case "American Indian":
						procParam.add(2.0);
						break;
					case "Asian":
						procParam.add(3.0);
						break;
					case "Pacific Island":
						procParam.add(4.0);
						break;
					case "Plus 2 Race":
						procParam.add(5.0);
						break;
					case "Other Race":
						procParam.add(6.0);
						break;
					case "other":
						procParam.add(-1.0);
						break;
					default:
						throw new Exception("Invalid Race Probability Config");
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}

		if (!this.biasSingleParams.hispanicProb.equals("")){
			Person.addBiasType("Hispanic");
			String[] paramSplit = biasSingleParams.hispanicProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				if (prefix.equals("Hispanic")){
					procParam.add(1.0);
				} else if (prefix.equals("Non-Hispanic")){
					procParam.add(0.0);
				} else  {
					throw new Exception("Invalid Hispanic Probability Config");
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}

		if (!this.biasSingleParams.residencyProb.equals("")){
			Person.addBiasType("Residency");
			String[] paramSplit = biasSingleParams.residencyProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				if (prefix.equals("Inside")){
					procParam.add(1.0);
				} else if (prefix.equals("Outside")){
					procParam.add(0.0);
				} else  {
					throw new Exception("Invalid Residency Probability Config");
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}

		if (!this.biasSingleParams.livingAreaProb.equals("")){
			Person.addBiasType("LivingArea");
			String[] paramSplit = biasSingleParams.livingAreaProb.split("/");
			List<List<Double>> typeParam = new ArrayList<>();
			for (String paramPair: paramSplit){
				List<Double> procParam = new ArrayList<>();
				idxColon = paramPair.indexOf(":");
				prefix = paramPair.substring(0,idxColon);
				double probVal = Double.parseDouble(paramPair.substring(idxColon+1));
				if (prefix.equals("other")){
					procParam.add(-1.0);
				} else {
					int idxDash = prefix.indexOf("-");
					if (idxDash == -1) throw new Exception("Invalid Living Area Probability Config");
					double low = Double.parseDouble(prefix.substring(1,idxDash));
					String highStr = prefix.substring(idxDash+1,prefix.length()-1);
					double high = (highStr.equals("Inf")) ? Double.MAX_VALUE : Double.parseDouble(highStr);
					if (low >= high || high <= 0) throw new Exception("Invalid Living Area Probability Config");
					procParam.add(low);
					procParam.add(high);
				}
				procParam.add(probVal);
				typeParam.add(procParam);
			}
			Person.processedSingleBiasParams.add(typeParam);
		}
	}

	public WorldModel(long seed, WorldParameters params, BiasParameters biasParameters, BiasSingleParameters biasSingleParameters) throws IOException,
			Exception {
		super(seed);
		manipulationScheduler = new MasterScheduler<Manipulation>();
		logScheduler = new MasterScheduler<LogSchedule>();
		eventScheduler = new MasterScheduler<EventSchedule>();
		this.params = params;
		this.biasParams = biasParameters;
		this.biasSingleParams = biasSingleParameters;
		timeUtil.addEventTime(SimulationEvent.SimulationStart, new DateTime());
		simulationSeed = seed;
		spatialNetwork.loadMapLayers(params.maps, "walkways.shp",
				"buildings.shp", "buildingUnits.shp", params.regions);

		initRegion();
		initPlaces();
		GeoUtils.alignMBRs(spatialNetwork.getAllLayers());

		initVisualGraph();
		reservedLog = new ReservedLogChannels(this);
		startDataCollectionForQoIs();
	}

	/**
	 * Start method that creates and initializes agents and objects
	 */
	@Override
	public void start() {
		timeUtil.addEventTime(SimulationEvent.AgentInitStart, new DateTime());
		super.start();

		try {
			this.collectSingleBiasTypes();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		String strInfo = Person.singleBiasTypes.toString();
		if (this.biasSingleParams.activate) logger.info("Considered single bias types: "+strInfo.substring(1,strInfo.length()-1));
		logger.info("Considered component bias types: "+biasParams.biasConsideration.replace("/",", "));

		agentLayer.clear(); // clear any existing agents from previous runs
		addSchedulingAgents();
		addHumanAgents();
		// addSupplyChainAgents();
		agentLayer.setMBR(spatialNetwork.getWalkwayLayer().getMBR());

		timeUtil.addEventTime(SimulationEvent.AgentInitEnd, new DateTime());
		timeUtil.logTimeSpent(SimulationEvent.AgentInitStart,
				SimulationEvent.AgentInitEnd,
				"Agent population create/initialize time");
		// Ensure that the spatial index is made aware of the new agent
		// positions. Scheduled to guaranteed to run after all agents moved.
		// let's put all schedules here so that we can easily track them and order by
		// priority
		// lower priority first
		schedule.scheduleRepeating(logScheduler, LOGGING_PRIORITY, 1);
		schedule.scheduleRepeating(agentLayer.scheduleSpatialIndexUpdater(), SPATIAL_INDEX_UPDATING_PRIORITY, 1);
		schedule.scheduleRepeating(dataCollector, DATA_COLLECTION_PRIORITY, 1);
		schedule.scheduleRepeating(eventScheduler, PRE_EVENT_PRIORITY, 1);
		schedule.scheduleRepeating(manipulationScheduler, INTERVENTION_PRIORITY, 1);
		schedule.scheduleRepeating(new Steppable() {
			@Override
			public void step(SimState state) {
				if (visualFriendFamilyGraph != null && visualWorkGraph != null) {
					visualFriendFamilyGraph.stepBegins(state.schedule.getSteps());
					visualWorkGraph.stepBegins(state.schedule.getSteps());
				}
			}
		}, STEP_BEGIN_PRIORITY, 1);

		// manipulation
		// manipulate(ManipulationLoader.loadFromConfig(params.initialManipulationFilePath));
		reservedLog.loggingSetup();
		reservedLog.loggingSchedule();
	}

	@Override
	public void awakeFromCheckpoint() {
		super.awakeFromCheckpoint();
		reloadVisualGraph();
		manipulate(ManipulationLoader
				.loadFromConfig(params.additionalManipulationFilePath));
	}

	@Override
	public void finish() {
		super.finish();
		try {
			if (visualFriendFamilyGraph != null && visualWorkGraph != null) {
				friendFamilyGraphSink.end();
				workGraphSink.end();
				logger.info("Social network graph saved.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		try{
			String timeStamp = new SimpleDateFormat("MM-dd").format(new java.util.Date());
			File file = new File("./logs/DiseaseData_" + agentId + "_" + timeStamp + ".tsv");
			if (!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);

			bw.write("Agent\tOriginalLoc\tByAgent\tExposedTime\tExposedLoc\tExposedCheckIn\tInfectiousTime\tInfectiousLoc\tInfectiousCheckIn\tRecoverTime\n");

			for (Person person : this.agents.values())
				bw.write(person.getFinalDiseaseState()+"\n");

			bw.close();
		} catch (Exception e){
			System.out.println(e);
		}
		*/


		timeUtil.addEventTime(SimulationEvent.SimulationEnd, new DateTime());
		timeUtil.logTimeSpent(SimulationEvent.SimulationStart,
				SimulationEvent.SimulationEnd, "Total simulation time");
	}

	// ALL INITIALIZATION-RELATED METHODS - called only during the early phases
	// of a simulation run

	private void initPlaces() {

		// NEIGHBORHOOD IDENTIFICATION
		Iterator<MasonGeometry> iter = spatialNetwork.getBuildingLayer()
				.getGeometries().iterator();

		this.neighborhoodBuildingMap = new TreeMap<Integer, List<Building>>();
		this.regionResidentialMap = new TreeMap<Integer, List<Long>>();
		this.regionApartmentMap = new TreeMap<Integer, List<Long>>();
		this.buildings = new TreeMap<Long, Building>();
		this.classrooms = new TreeMap<Long, Classroom>();
		this.apartments = new TreeMap<Long, Apartment>();
		this.workplaces = new TreeMap<Long, Workplace>();
		this.jobs = new TreeMap<Long, Job>();
		this.restaurants = new TreeMap<Long, Restaurant>();
		this.pubs = new TreeMap<Long, Pub>();

		// db.Buildings = buildings;

		long buildingId = 0;
		int neighborhoodId = 0;
		int regionId = 0;
		int bType = 0;
		int blockId = 0;
		int blockGroupId = 0;
		int censusTractId = 0;
		double degree;

		// load building information
		while (iter.hasNext()) {
			MasonGeometry geom = iter.next();

			neighborhoodId = geom.getIntegerAttribute("neighbor");
			try{
				regionId = geom.getIntegerAttribute("region");
			} catch (Exception e){
				regionId = -1;
			}
			buildingId = geom.getIntegerAttribute("id");
			bType = geom.getIntegerAttribute("function");
			degree = geom.getDoubleAttribute("degree");

			Building place = new Building(this, buildingId);
			place.setLocation(geom);
			place.setBuildingType(BuildingType.valueOf(bType));
			place.setNeighborhoodId(neighborhoodId);
			place.setAttractiveness(degree);

			// Entirely covered
			if (regionId == -1) {
				for (Integer rId : regions.keySet()) {
					Region region = regions.get(rId);
					if (region.getLocation().geometry.covers(geom.geometry)) {
						regionId = rId;
						break;
					}
				}
			}

			// At the boundary
			if (regionId == -1){
				for (Integer rId: regions.keySet()){
					Region region = regions.get(rId);
					if (region.getLocation().geometry.intersects(geom.geometry)){
						regionId = rId;
						break;
					}
				}
			}

			// A little outside
			if (regionId == -1){

				// Find the nearest region
				double distance = Double.POSITIVE_INFINITY;
				for (Integer rId: regions.keySet()){
					Region region = regions.get(rId);
					if (distance > region.getLocation().geometry.distance(geom.geometry)){
						regionId = rId;
						distance = region.getLocation().geometry.distance(geom.geometry);
					}
				}
			}

			place.setRegionId(regionId);

			List<Building> neighbors = null;
			if (neighborhoodBuildingMap.containsKey(neighborhoodId)) {
				neighbors = neighborhoodBuildingMap.get(neighborhoodId);
			} else {
				neighbors = new ArrayList<Building>();
				neighborhoodBuildingMap.put(neighborhoodId, neighbors);
			}

			if (BuildingType.valueOf(bType) == BuildingType.Residental){
				List<Long> regionRes = null;
				if (regionResidentialMap.containsKey(regionId)){
					regionRes = regionResidentialMap.get(regionId);
				} else {
					regionRes = new ArrayList<Long>();
					regionResidentialMap.put(regionId,regionRes);
				}
				regionRes.add(buildingId);
			}


			buildings.put(place.getId(), place);
			neighbors.add(place);
		}
		// END NEIGHBORHOOD IDENTIFICATION
		logger.info("Number of neighborhoods: " + neighborhoodBuildingMap.size());

		// INITIALIZE THE ENVIROMENT NEIGHBORHOOD BY NEIGHBORHOOD AND CREATE
		// UNITS
		Map<Integer, Integer> numOfAgentsPerNeighborhood = numberOfAgentsPerNeighborhood(this.neighborhoodBuildingMap,
				params.numOfAgents);
		//Map<Integer, Integer> numOfAgentsPerNeighborhood = numberOfAgentsPerNeighborhood(this.neighborhoodBuildingMap);
		int nIndex = 0;

		NeighborhoodComposition neighborhoodComposition;
		long unitId = 0;
		long jobId = 0;

		for (Integer nId : neighborhoodBuildingMap.keySet()) {
			List<Building> neighborhoodBuildings = neighborhoodBuildingMap.get(nId);
			int numberOfNeighborhoodBuildings = neighborhoodBuildings.size();

			// calculate attractiveness percentile of each building per neighborhood
			double[] attractivenessValues = new double[numberOfNeighborhoodBuildings];
			for (int i = 0; i < attractivenessValues.length; i++) {
				attractivenessValues[i] = neighborhoodBuildings.get(i).getAttractiveness();
			}
			EmpiricalDistribution distribution = new EmpiricalDistribution(attractivenessValues.length);
			distribution.load(attractivenessValues);

			for (Building bld : neighborhoodBuildings) {
				double percentile = distribution.cumulativeProbability(bld.getAttractiveness());
				bld.setAttractivenessPercentile(percentile);
			}

			// number of units and random number generators are set.
			neighborhoodComposition = new NeighborhoodComposition(random, params);
			neighborhoodComposition.calculate(numOfAgentsPerNeighborhood.get(nId));
			// neighborhoodComposition.calculateConstant(numOfAgentsPerNeighborhood.get(nId));

			logger.info("-------Buildings----------");
			logger.info("Total number of buildings: "
					+ numberOfNeighborhoodBuildings);

			int numberOfSchoolBuildings = neighborhoodComposition
					.getNumberOfSchools();
			int remainingBuildings = numberOfNeighborhoodBuildings
					- numberOfSchoolBuildings;
			int numberOfResidentialBuildings = (int) Math
					.ceil(remainingBuildings * 2.0 / 3.0);
			int numberOfCommercialBuildings = remainingBuildings
					- numberOfResidentialBuildings;

			logger.info("# of School Buildings needed: "
					+ neighborhoodComposition.getNumberOfSchools());
			logger.info("# of Residential Buildings needed: "
					+ numberOfResidentialBuildings);
			logger.info("# of Commercial Buildings needed: "
					+ numberOfCommercialBuildings);
			logger.info("--------Units---------");
			logger.info("# of Apartments Needed: "
					+ neighborhoodComposition.getNumberOfApartments());
			logger.info("# of Schools Needed: "
					+ neighborhoodComposition.getNumberOfSchools());
			logger.info("# of Pubs Needed: "
					+ neighborhoodComposition.getNumberOfPubs());
			logger.info("# of Workplace Needed: "
					+ neighborhoodComposition.getNumberOfWorkplaces());
			logger.info("# of Restaurant Needed: "
					+ neighborhoodComposition.getNumberOfRestaurants());

			// dedicate building(s) for schools and add a classroom per school
			int numOfSchools = neighborhoodComposition.getNumberOfSchools() == 0 ? 1
					: neighborhoodComposition.getNumberOfSchools();

			for (int i = 0; i < numOfSchools; i++) {
				long selectedBuildingId = neighborhoodComposition
						.getRandomBuildingId(neighborhoodBuildings,
								BuildingType.Residental);

				Building bld = buildings.get(selectedBuildingId);

				bld.setBuildingType(BuildingType.School);
				bld.setAttractiveness(neighborhoodComposition
						.generateAttractivenessNumber());
				bld.getLocation().addIntegerAttribute("function",
						BuildingType.School.ordinal());

				Classroom classroom = new Classroom(unitId++, bld);

				classroom.setAttractiveness(neighborhoodComposition
						.generateAttractivenessNumber());
				classroom.setNumberOfRooms(1);
				classroom.setPersonCapacity((int) neighborhoodComposition
						.generateSchoolCapacity());
				classroom.setMonthlyCost(neighborhoodComposition
						.generateSchoolCost(classroom.getAttractiveness()));
				classroom.setBlockId(bld.getBlockId());
				classroom.setBlockGroupId(bld.getBlockGroupId());
				classroom.setCensusTractId(bld.getCensusTractId());

				bld.addUnit(classroom);
				classrooms.put(classroom.getId(), classroom);
			}

			// distribute apartment units in residential buildings
			for (int i = 0; i < neighborhoodComposition.getNumberOfApartments(); i++) {

				long selectedBuildingId = neighborhoodComposition
						.getRandomBuildingId(neighborhoodBuildings,
								BuildingType.Residental);

				Building bld = buildings.get(selectedBuildingId);

				Apartment apartment = new Apartment(unitId++, bld);
				int rooms = neighborhoodComposition.generateNumberOfRoomsForApartments();
				apartment.setAttractiveness(neighborhoodComposition
						.generateAttractivenessNumber());
				apartment.setNumberOfRooms(neighborhoodComposition
						.generateNumberOfRoomsForApartments());
				apartment.setPersonCapacity(rooms);

				// based on the attractiveness/degree percentile, rent can be as twice as the
				// regular value
				double rent = neighborhoodComposition.generateApartmentRentalPrice(rooms);
				rent *= (1 + bld.getAttractivenessPercentile());
				apartment.setRentalCost(rent);

				apartment.setBlockId(bld.getBlockId());
				apartment.setRegionId(bld.getRegionID());
				apartment.setBlockGroupId(bld.getBlockGroupId());
				apartment.setCensusTractId(bld.getCensusTractId());

				bld.addUnit(apartment);
				apartments.put(apartment.getId(), apartment);
			}

			// distribute workplace units in commercial buildings
			for (int i = 0; i < neighborhoodComposition.getNumberOfWorkplaces(); i++) {
				long selectedBuildingId = neighborhoodComposition
						.getRandomBuildingId(neighborhoodBuildings,
								BuildingType.Commercial);
				Building bld = buildings.get(selectedBuildingId);

				Workplace workplace = new Workplace(unitId++, bld);

				workplace.setAttractiveness(neighborhoodComposition
						.generateAttractivenessNumber());
				workplace.setBlockId(bld.getBlockId());
				workplace.setBlockGroupId(bld.getBlockGroupId());
				workplace.setCensusTractId(bld.getCensusTractId());

				// add jobs to workplace
				int numberOfJobs = neighborhoodComposition
						.generateNumberOfJobsAtAWorkplaceUnit();

				for (int j = 0; j < numberOfJobs; j++) {
					EducationLevel educationLevel = neighborhoodComposition
							.generateEducationLevelRequirementForJobs();
					double hourlyRate = neighborhoodComposition
							.generateHourlyRate(educationLevel);
					int hour = 7 + random.nextInt(2);
					int minute = random.nextInt(60);
					LocalTime jobStartTime = new LocalTime(hour, minute);
					LocalTime jobEndTime = jobStartTime
							.plusHours(params.workHoursPerDay);

					List<DayOfWeek> daysToWork = neighborhoodComposition
							.generateWorkDays(educationLevel);

					// System.out.println("Hourly rate: "+ hourlyRate);

					Job job = new Job(workplace, jobId++);

					job.setHourlyRate(hourlyRate);
					job.setStartTime(jobStartTime);
					job.setEndTime(jobEndTime);
					job.setEducationRequirement(educationLevel);
					job.addWorkDays(daysToWork);
					job.setNeighborhoodId(nId);

					workplace.addJob(job);
					jobs.put(job.getId(), job);
				}

				bld.addUnit(workplace);
				workplaces.put(workplace.getId(), workplace);
			}

			// distribute pub units in commercial buildings
			for (int i = 0; i < neighborhoodComposition.getNumberOfPubs(); i++) {
				long selectedBuildingId = neighborhoodComposition
						.getRandomBuildingId(neighborhoodBuildings,
								BuildingType.Commercial);
				Building bld = buildings.get(selectedBuildingId);

				Pub pub = new Pub(unitId++, bld);

				pub.setHourlyCost(neighborhoodComposition
						.generatePubHourlyCharge());
				pub.setAttractiveness(neighborhoodComposition
						.generateAttractivenessNumber());
				pub.setPersonCapacity(neighborhoodComposition.getSiteCapacity());
				pub.setBlockId(bld.getBlockId());
				pub.setBlockGroupId(bld.getBlockGroupId());
				pub.setCensusTractId(bld.getCensusTractId());

				bld.addUnit(pub);
				pubs.put(pub.getId(), pub);
			}

			// distribute restaurant units in commercial buildings
			for (int i = 0; i < neighborhoodComposition
					.getNumberOfRestaurants(); i++) {
				long selectedBuildingId = neighborhoodComposition
						.getRandomBuildingId(neighborhoodBuildings,
								BuildingType.Commercial);
				Building bld = buildings.get(selectedBuildingId);

				Restaurant restaurant = new Restaurant(unitId++, bld);

				restaurant.setFoodCost(neighborhoodComposition
						.generateRestaurantCostCharge());
				restaurant.setAttractiveness(neighborhoodComposition
						.generateAttractivenessNumber());
				restaurant.setPersonCapacity(neighborhoodComposition
						.getSiteCapacity());
				restaurant.setBlockId(bld.getBlockId());
				restaurant.setBlockGroupId(bld.getBlockGroupId());
				restaurant.setCensusTractId(bld.getCensusTractId());

				bld.addUnit(restaurant);
				restaurants.put(restaurant.getId(), restaurant);
			}

			// building unit location setting
			List<Long> emptyBuilding = new ArrayList<>();
			for (Building bld : neighborhoodBuildings) {
				bld.setAttractiveness(neighborhoodComposition
						.generateAttractivenessNumber());
				List<MasonGeometry> units = spatialNetwork
						.getBuildingUnitTable().get((int) bld.getId());
				if (units == null){
					System.out.println((int) bld.getId());
					continue;
				}
				int index = 0;
				for (BuildingUnit unit : bld.getUnits()) {
					unit.setLocation(units.get(index));
					index++;
					index %= units.size();
				}
			}

			break;
		}
	}

	private void initRegion(){
		Iterator<MasonGeometry> iter = spatialNetwork.getRegionLayer()
				.getGeometries().iterator();
		this.regions = new TreeMap<Integer,Region>();
		this.regionsIds = new ArrayList<Integer>();

		int regionID;
		int totalNumAgents = params.numOfAgents;
		double percentage;
		int countedAgents = 0;

		while (iter.hasNext()) {
			MasonGeometry geom = iter.next();

			regionID = geom.getIntegerAttribute("id");
			percentage = geom.getDoubleAttribute("TotPop");
			regionsIds.add(regionID);

			Region place = new Region(regionID, percentage);

			place.setIsMale(geom.getDoubleAttribute("Male"));
			place.setLocation(geom);
			for (int i = 0; i < 10; i++) place.addAgeGroup(geom.getDoubleAttribute("AgeGroup"+i));

			for (int i = 0; i < 5; i++) place.addEduLevel(geom.getDoubleAttribute("EduLevel"+i));

			try{
				place.setIsHispanic(geom.getDoubleAttribute("Hispanic"));
			} catch (Exception ignore){ }

			try{
				for (int i = 0; i < 7; i++) place.addRace(geom.getDoubleAttribute("Race"+i));
			} catch (Exception ignore){ }

			try{
				for (int i = 0; i < 7; i++) place.addIncomeLevel(geom.getDoubleAttribute("Income"+i));
			} catch (Exception ignore){ }

			try{
				for (int i = 0; i < 6; i++) place.addIndivIncomeLevel(geom.getDoubleAttribute("IndiInc"+i));
			} catch (Exception ignore){ }

			try{
				place.setInProvince(1-geom.getDoubleAttribute("Foreign"));
			} catch (Exception ignore){ }

			try{
				place.setAreaPerAgent(geom.getDoubleAttribute("Area/Person"));
			} catch (Exception ignore){ }

			place.initCounts();
			regions.put(place.getRegionID(), place);
		}
	}

	private void initVisualGraph() {
		// change graph directory
		String friendFamilyPath = ReservedLogChannels
				.fullDirectory(ReservedLogChannels.DEFAULT_DIRECTORY + friendFamilyGraphSinkPath);
		String workPath = ReservedLogChannels.fullDirectory(ReservedLogChannels.DEFAULT_DIRECTORY + workGraphSinkPath);

		URL url = WorldModel.class
				.getResource("/stylesheet/NodeColoringBasedOnInterest.css");
		String css = "url(" + url.toString() + ")";
		// Friend Family Graph
		visualFriendFamilyGraph = new SingleGraph(
				"Social Network: Friend Family");
		visualFriendFamilyGraph.addAttribute("ui.stylesheet", css);
		if (params.isFriendFamilyGraphVisible)
			friendFamilyViewer = visualFriendFamilyGraph.display();
		friendFamilyGraphSink = new FileSinkDGS();
		visualFriendFamilyGraph.addSink(friendFamilyGraphSink);
		try {
			friendFamilyGraphSink.begin(friendFamilyPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Work Graph
		visualWorkGraph = new SingleGraph("Social Network: Work");
		visualWorkGraph.addAttribute("ui.stylesheet", css);
		if (params.isWorkGraphVisible)
			visualWorkGraph.display();
		workGraphSink = new FileSinkDGS();
		visualWorkGraph.addSink(workGraphSink);
		try {
			workGraphSink.begin(workPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void reloadVisualGraph() {
		String friendFamilyPath = ReservedLogChannels
				.fullDirectory(ReservedLogChannels.DEFAULT_DIRECTORY + friendFamilyGraphSinkPath);
		String workPath = ReservedLogChannels.fullDirectory(ReservedLogChannels.DEFAULT_DIRECTORY + workGraphSinkPath);

		URL url = WorldModel.class
				.getResource("/stylesheet/NodeColoringBasedOnInterest.css");
		String css = "url(" + url.toString() + ")";
		if (visualFriendFamilyGraph == null) {
			// Friend Family Graph
			visualFriendFamilyGraph = new SingleGraph(
					"Social Network: Friend Family");
			visualFriendFamilyGraph.addAttribute("ui.stylesheet", css);
			if (params.isFriendFamilyGraphVisible)
				friendFamilyViewer = visualFriendFamilyGraph.display();
			friendFamilyGraphSink = new FileSinkDGS();
			visualFriendFamilyGraph.addSink(friendFamilyGraphSink);
			try {
				friendFamilyGraphSink.begin(friendFamilyPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			visualFriendFamilyGraph.clear();
		}
		Bag nodes = friendFamilyNetwork.getAllNodes();
		for (Object obj : nodes) {
			String id = obj.toString();
			visualFriendFamilyGraph.addNode(id);
			visualFriendFamilyGraph.getNode(id).addAttribute("ui.class",
					agents.get(Long.parseLong(id)).getInterest().toString());
		}
		Edge[][] edges = friendFamilyNetwork.getAdjacencyMatrix();
		for (int i = 0; i < edges.length; i++) {
			for (int j = 0; j < edges[i].length; j++) {
				Edge edge = edges[i][j];
				if (edge != null) {
					String me = String.valueOf(edge.getFrom());
					String other = String.valueOf(edge.getTo());
					visualFriendFamilyGraph.addEdge(me + "--" + other, me,
							other, true);
				}
			}
		}

		if (visualWorkGraph == null) {
			// Work Graph
			visualWorkGraph = new SingleGraph("Social Network: Work");
			visualWorkGraph.addAttribute("ui.stylesheet", css);
			if (params.isWorkGraphVisible)
				visualWorkGraph.display();
			workGraphSink = new FileSinkDGS();
			visualWorkGraph.addSink(workGraphSink);
			try {
				workGraphSink.begin(workPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		nodes = workNetwork.getAllNodes();
		for (Object obj : nodes) {
			String id = obj.toString();
			visualWorkGraph.addNode(id);
			visualWorkGraph.getNode(id).addAttribute("ui.class",
					agents.get(Long.parseLong(id)).getInterest().toString());
		}
		edges = workNetwork.getAdjacencyMatrix();
		for (int i = 0; i < edges.length; i++) {
			for (int j = 0; j < edges[i].length; j++) {
				Edge edge = edges[i][j];
				if (edge != null) {
					String me = String.valueOf(edge.getFrom());
					String other = String.valueOf(edge.getTo());
					visualWorkGraph.addEdge(me + "--" + other, me, other, true);
				}
			}
		}
	}

	// ID sequence used for new pubs
	long unitId = 0;

	public void openNewPub(Double building) {
		// new id starts from
		if (unitId == 0)
			unitId = 20000;
		Building bld = getBuilding(building.longValue());
		if (bld == null)
			return;
		NeighborhoodComposition neighborhoodComposition = new NeighborhoodComposition(random, params);
		Pub pub = new Pub(unitId++, bld);

		pub.setHourlyCost(neighborhoodComposition.generatePubHourlyCharge());
		pub.setAttractiveness(neighborhoodComposition.generateAttractivenessNumber());
		pub.setPersonCapacity(neighborhoodComposition.getSiteCapacity());
		pub.setBlockId(bld.getBlockId());
		pub.setBlockGroupId(bld.getBlockGroupId());
		pub.setCensusTractId(bld.getCensusTractId());

		bld.addUnit(pub);
		pubs.put(pub.getId(), pub);

		List<MasonGeometry> units = spatialNetwork.getBuildingUnitTable().get((int) bld.getId());
		pub.setLocation(units.get(bld.getUnits().size() % units.size()));

		updateNearestPubCache();
	}

	public void updateNearestPubCache() {
		// update nearest pubs cache
		List<BuildingUnit> places = new ArrayList<BuildingUnit>();
		places.addAll(apartments.values());
		places.addAll(workplaces.values());
		places.addAll(restaurants.values());
		places.addAll(pubs.values());
		for (BuildingUnit unit : places) {
			unit.resetNearestPubDistanceMap();
		}
	}

	public void updateNearestRestaurantCache() {
		// update nearest restaurants cache
		List<BuildingUnit> places = new ArrayList<BuildingUnit>();
		places.addAll(apartments.values());
		places.addAll(workplaces.values());
		places.addAll(restaurants.values());
		places.addAll(pubs.values());
		for (BuildingUnit unit : places) {
			unit.resetNearestRestaurantDistanceMap();
		}
	}

	public void openNewPubs(Double numberOfNewPubs) {
		// new id starts from
		if (unitId == 0)
			unitId = 20000;
		// distribute pub units in commercial buildings
		for (int i = 0; i < numberOfNewPubs; i++) {
			Set<Integer> neighbors = neighborhoodBuildingMap.keySet();
			List<Building> neighborhoodBuildings = neighborhoodBuildingMap.get(random.nextInt(neighbors.size()));
			int numberOfNeighborhoodBuildings = neighborhoodBuildings.size();

			// number of units and random number generators are set.
			NeighborhoodComposition neighborhoodComposition = new NeighborhoodComposition(random, params);
			neighborhoodComposition.calculate(params.numOfAgents);
			// neighborhoodComposition.calculateConstant(params.numOfAgents);
			long selectedBuildingId = neighborhoodComposition.getRandomBuildingId(neighborhoodBuildings,
					BuildingType.Commercial);
			Building bld = buildings.get(selectedBuildingId);

			Pub pub = new Pub(unitId++, bld);

			pub.setHourlyCost(neighborhoodComposition.generatePubHourlyCharge());
			pub.setAttractiveness(neighborhoodComposition.generateAttractivenessNumber());
			pub.setPersonCapacity(neighborhoodComposition.getSiteCapacity());
			pub.setBlockId(bld.getBlockId());
			pub.setBlockGroupId(bld.getBlockGroupId());
			pub.setCensusTractId(bld.getCensusTractId());

			bld.addUnit(pub);
			pubs.put(pub.getId(), pub);

			List<MasonGeometry> units = spatialNetwork.getBuildingUnitTable().get((int) bld.getId());
			pub.setLocation(units.get(bld.getUnits().size() % units.size()));
		}
		updateNearestPubCache();
	}

	public void manipulate(List<Manipulation> events) {
		if (events != null) {

			manipulationScheduler.add(events);
			System.out.println("HOSSEIN: Manipulation events are added.");
		} else {

			System.out.println("HOSSEIN: Manipulation events are null.");
		}
	}

	private void addSchedulingAgents() {
		// add periodic functions schedule first

		// there are routines executed at every 24 hours.
		// we calculate how many ticks we need to start that routine

		// calculate 24-hour in terms of ticks
		double interval = 24.0 * 60.0 / params.oneStepTime;
		double delayedStartTickForMidnight = 0;

		LocalDateTime dt = this.getSimulationTime();
		LocalDateTime midnight = dt.minusHours(dt.getHourOfDay()).minusMinutes(
				dt.getMinuteOfHour());

		int minDiffBetweenNowAndMidnight = Minutes.minutesBetween(dt, midnight)
				.getMinutes();

		if (minDiffBetweenNowAndMidnight != 0) { // the time is different than
													// midnight
			if (midnight.isBefore(dt) == true) { // means simulation time is
													// after midnight
				minDiffBetweenNowAndMidnight = 60 * 24 - minDiffBetweenNowAndMidnight; // decrease
																						// it
																						// from
																						// 24
																						// hours
																						// (equivalent
																						// minutes)
			}
		}

		delayedStartTickForMidnight = minDiffBetweenNowAndMidnight
				/ params.oneStepTime * 1.0;

		schedule.scheduleRepeating(delayedStartTickForMidnight,
				PRE_EVENT_PRIORITY, new DailyRoutines(), interval);

		double delayedStartTickForEvening = 0;
		// add routine for 7pm schedules.

		LocalDateTime evening = dt.minusHours(dt.getHourOfDay())
				.minusMinutes(dt.getMinuteOfHour()).plusHours(19);

		int minDiffBetweenNowAndEvening = Minutes.minutesBetween(dt, evening)
				.getMinutes();

		if (minDiffBetweenNowAndEvening != 0) { // the time is different than
												// 7pm
			if (evening.isBefore(dt) == true) { // means simulation time is
												// after 7pm
				minDiffBetweenNowAndEvening = 60 * 24 - minDiffBetweenNowAndEvening; // decrease
																						// it
																						// from
																						// 24
																						// hours
																						// (equivalent
																						// minutes)
			}
		}
		delayedStartTickForEvening = minDiffBetweenNowAndEvening
				/ params.oneStepTime * 1.0;

		schedule.scheduleRepeating(delayedStartTickForEvening,
				PRE_EVENT_PRIORITY, new DailyRoutines(RoutineType.Evening),
				interval);
	}

	private void addHumanAgents() {
		agents = new TreeMap<Long, Person>();

		// add approx equal number of agents for each neighborhood.
		Map<Integer, Integer> numOfAgentsPerNeighborhood = numberOfAgentsPerNeighborhood(this.neighborhoodBuildingMap,
				params.numOfAgents);
		//Map<Integer, Integer> numOfAgentsPerNeighborhood = numberOfAgentsPerNeighborhood(this.neighborhoodBuildingMap);

		int nIndex = 0;
		logger.info("Total number of agents: " + params.numOfAgents);

		int agentCount = params.numOfAgents;
		int targetNumAgent = params.numOfAgents;
		for (int nId : this.neighborhoodBuildingMap.keySet()) {
			System.out.println("Number of agents in neighborhood #" + nId + ": " + numOfAgentsPerNeighborhood.get(nId));

			NeighborhoodComposition neighborhoodComposition = new NeighborhoodComposition(random, params);
			neighborhoodComposition.calculate(numOfAgentsPerNeighborhood.get(nId));
			// neighborhoodComposition.calculateConstant(numOfAgentsPerNeighborhood.get(nId));

			for (int rId : this.regions.keySet()) {
				Region curRegion = regions.get(rId);

				int numAgentInRegion = (int)(curRegion.getPerPopulation() * targetNumAgent + 0.5);
				if (agentCount - numAgentInRegion < 0) numAgentInRegion = agentCount;
				agentCount -= numAgentInRegion;
				curRegion.setPopulation(numAgentInRegion);
				curRegion.calNumAgents(params.numOfSingleAgentsPer1000, params.numOfFamilyAgentsWithKidsPer1000);

				logger.info("Number of agents in Region #" + rId + ": " + curRegion.getPopulation());

				// create family agents with kids
				for (long i = 0; i < curRegion.getNumberOfFamilyAgentsWKids(); i++) {
					addAgent(agentId++, nId, true, true, 3, curRegion);
				}

				// create family agents with no kid
				for (long i = 0; i < curRegion.getNumberOfFamilyAgentsWOKids(); i++) {
					addAgent(agentId++, nId, true, false, 2, curRegion);
				}

				// create single agents
				for (long i = 0; i < curRegion.getNumberOfSingleAgents(); i++) {
					addAgent(agentId++, nId, false, false, 1, curRegion);
				}
			}
		}

		// spatialNetwork.clearPrecomputedPaths();
		logger.info("Human agents are added.");

		logger.info("Number of Regions: "+regions.size());
		for (Integer rid: regions.keySet()){
			logger.info("-------Region "+rid+": "+regions.get(rid).getPopulation()+" Agents----------");
			logger.info(regions.get(rid).getAgeGroup());
			logger.info(regions.get(rid).getEduLevel());
			logger.info(regions.get(rid).getGender());
			logger.info(regions.get(rid).getHispanic());

			String info = regions.get(rid).getRace();
			if (info != null) logger.info(info);
			info = regions.get(rid).getIncomeLevel();
			if (info != null) logger.info(info);
			info = regions.get(rid).getIndivIncomeLevel();
			if (info != null) logger.info(info);
		}
	}

	private void addAgent(long agentId, int nId, boolean family, boolean kids,
			int numOfPeople, Region region) {

		Person agent = new Person(this, agentId);

		AgentInitialization initialization = new AgentInitialization(this);
		// initialize the agent

		int agentAge = initialization.generateAgentAge(region);
		EducationLevel education = initialization.generateEducationLevel(region);
		Race race = initialization.generateAgentRace(region);
		int hh_income = initialization.generateAgentHHCensusIncome(region);
		int indiv_income = initialization.generateAgentIndivCensusIncome(region);
		boolean isMale = initialization.isMale(region);
		boolean isHispanic = initialization.isHispanic(region);
		boolean inProvince = initialization.inProvince(region);
		region.incrementInitiated();

		String gender = (isMale) ? "Male" : "Female";
		StringBuilder res = new StringBuilder();
		res.append(" Agent #").append(agentId).append(" ")
				.append(agentAge).append(" years old ")
				.append(gender).append(" ");
		try{
			res.append(race.toString()).append(" ");
		} catch (Exception ignore){}
		res.append(education.toString());
		if (hh_income != -1)
			res.append(" with annual household income ").append((double)hh_income/1000.0).append("k");
		if (indiv_income != -1)
			res.append(" (").append((double)indiv_income/1000.0).append("k ind.)");
		logger.info(res.toString());

		// System.out.println("Education level: " + education);
		double initialBalance = initialization
				.generateInitialBalance(education);
		double joviality = initialization.generateJovialityValue();

		agent.setOriginRegion(region.getRegionID());
		agent.setNeighborhoodId(nId);
		agent.setAge(agentAge);
		agent.setEducationLevel(education);
		agent.setGender(isMale);

		agent.setRace(race);
		agent.setHHCensusIncome(hh_income);
		agent.setIndivCensusIncome(indiv_income);
		agent.setHispanic(isHispanic);
		agent.setHouseholdInProvince(inProvince);

		agent.setReportingRate();
		agent.setReportingRates_Single();

		agent.setInterest(initialization.getAgentInterest());
		agent.getFoodNeed()
				.setAppetite(initialization.generateAppetiteNumber());
		agent.getFinancialSafetyNeed().depositMoney(initialBalance);
		agent.setJoviality(joviality);
		agent.setWalkingSpeed(params.agentWalkingSpeed);

		agent.makeFamily(kids, numOfPeople);

		agents.put(agent.getAgentId(), agent);

		// add agent to networks...
		friendFamilyNetwork.addNode(agent.getAgentId());
		workNetwork.addNode(agent.getAgentId());

		if (visualFriendFamilyGraph.getNode(String.valueOf(agentId)) == null)
			visualFriendFamilyGraph.addNode(String.valueOf(agent.getAgentId()));
		if (visualWorkGraph.getNode(String.valueOf(agentId)) == null)
			visualWorkGraph.addNode(String.valueOf(agent.getAgentId()));

		visualFriendFamilyGraph.getNode(String.valueOf(agent.getAgentId()))
				.addAttribute("ui.class", agent.getInterest().toString());

		// TODO: potential bottleneck
		// place the agent in the neighborhood
		agent.placeInNeighborhood();

		agentLayer.addGeometry(agent.getLocation());
		agent.jitter();

		Stoppable stp = schedule.scheduleRepeating(agent);
		agent.setStoppable(stp);
		// logger.info("Agent #" + agentId + " added.");
	}

	/**
	 * Returns an array that keeps number of agents per neighborhood.
	 * 
	 * @param neighborhoodBuildingMap
	 * @param numOfAgents
	 * @return
	 */
	public Map<Integer, Integer> numberOfAgentsPerNeighborhood(Map<Integer, List<Building>> neighborhoodBuildingMap,
			int numOfAgents) {
		Map<Integer, Integer> numberOfAgentsPerNeighborhood = new TreeMap<Integer, Integer>();
		int apprxNumber = (int) ((double) numOfAgents / (double) neighborhoodBuildingMap.size());
		int[] nids = new int[neighborhoodBuildingMap.size()];

		int j = 0;
		for (Integer id : neighborhoodBuildingMap.keySet()) {
			nids[j++] = id;
		}

		for (int i = 0; i < nids.length - 1; i++) {
			numberOfAgentsPerNeighborhood.put(nids[i], apprxNumber);
		}
		numberOfAgentsPerNeighborhood.put(nids[nids.length - 1],
				numOfAgents - (neighborhoodBuildingMap.size() - 1) * apprxNumber);

		return numberOfAgentsPerNeighborhood;
	}

	public Map<Integer, Integer> numberOfAgentsPerNeighborhood(Map<Integer, List<Building>> neighborhoodBuildingMap) {
		Map<Integer, Integer> numberOfAgentsPerNeighborhood = new TreeMap<Integer, Integer>();
		for (Integer rId : regions.keySet()){
			numberOfAgentsPerNeighborhood.put(rId, regions.get(rId).getPopulation());
		}

		return numberOfAgentsPerNeighborhood;
	}

	private void addSupplyChainAgents() {

		// find points on very left and very top of the spatial network and
		// place agents on those locations

		Bag walkwayGeometries = spatialNetwork.getWalkwayLayer()
				.getGeometries();
		double xMin = 0, yMax = 0;
		Point mostLeft, topMost;

		GeometryFactory fact = new GeometryFactory();
		Iterator geometriesIterator = walkwayGeometries.iterator();
		MasonGeometry geom = (MasonGeometry) geometriesIterator.next();
		xMin = geom.getGeometry().getCoordinates()[0].x;
		yMax = geom.getGeometry().getCoordinates()[0].y;
		mostLeft = fact.createPoint(new Coordinate(xMin, yMax));
		topMost = fact.createPoint(new Coordinate(xMin, yMax));

		// identifies top and left locations on the map
		while (geometriesIterator.hasNext()) {
			geom = (MasonGeometry) geometriesIterator.next();
			for (int i = 0; i < geom.getGeometry().getCoordinates().length; i++) {
				if (geom.getGeometry().getCoordinates()[i].x < xMin) {
					xMin = geom.getGeometry().getCoordinates()[i].x;
					mostLeft = fact.createPoint(new Coordinate(xMin, geom
							.getGeometry().getCoordinates()[i].y));
				}

				if (geom.getGeometry().getCoordinates()[i].y > yMax) {
					yMax = geom.getGeometry().getCoordinates()[i].y;
					topMost = fact.createPoint(new Coordinate(geom
							.getGeometry().getCoordinates()[i].x, yMax));
				}
			}
		}
	}

	// INITIALIZATION METHODS END

	// ROUTINES/PERIODIC METHODS

	public void monthlyRoutine() {
		for (Person person : this.agents.values()) {

			// time to pay the rent
			if (person.getShelterNeed().isSatisfied() == true) { // if person
																	// already
																	// rents a
																	// place
				person.payForShelter();
			}

			// and the education cost for kids
			if (person.hasFamily() && person.getFamily().haveKids()) {
				if (person.getFamily().getClassroom() != null) { // it is
																	// possible
																	// that
																	// families
																	// might not
																	// have a
																	// classroom
																	// in any
																	// neighborhoods
					double cost = person.getFamily().getClassroom()
							.getMonthlyCost();
					cost = cost * (person.getFamily().getNumberOfPeople() - 2); // multiple
																				// the
																				// cost
																				// per
																				// kid
					person.getFinancialSafetyNeed().withdrawMoney(cost, ExpenseType.Education);
				}
			}

			// make the agent older
			person.increaseAge(1.0 / 12);
		}
	}

	public void nightlyRoutine() {

		// update day display on the screen
		day = day + 1;
		logger.info("Night routine:" + getFormattedDateTime());

		// logger.info("NIGHTLY VISITOR PROFILE UPDATE " +
		// getFormattedDateTime());
		DecimalFormat formatter = new DecimalFormat("#0.00");
		// pubs nightly routines

		latestBarStatsData = new Object[this.pubs.size()][5 + params.numberOfInterestsToConsider];
		latestBarStatsData[0][0] = 1;

		int k;
		for (k = 0; k < params.numberOfInterestsToConsider; k++) {
			latestBarStatsData[0][k + 1] = Color.BLACK;
		}
		latestBarStatsData[0][++k] = 0.00;
		latestBarStatsData[0][++k] = 0.0;
		latestBarStatsData[0][++k] = 0;

		List<Pub> pubs = this.pubs.values().stream()
				.collect(Collectors.toList());

		for (int i = 0; i < pubs.size(); i++) {
			Pub pub = pubs.get(i);

			pub.housekeeping();
			if (pub.getVisitorProfile() != null) {

				latestBarStatsData[i][0] = pub.getId();
				int j;
				for (j = 0; j < params.numberOfInterestsToConsider; j++) {
					latestBarStatsData[i][j + 1] = ColorUtils
							.getInterestColorMap().get(
									pub.getVisitorProfile().getInterests()
											.get(j));
				}
				latestBarStatsData[i][++j] = StringUtils.trimDecimals(pub
						.getVisitorProfile().getAverageAge(), 2);
				latestBarStatsData[i][++j] = StringUtils.trimDecimals(pub
						.getVisitorProfile().getAverageIncome(), 2);
				latestBarStatsData[i][++j] = pub.getVisitorProfile().getTotal();

			} else {
				latestBarStatsData[i][0] = pub.getId();
				;
				latestBarStatsData[i][1] = Color.BLACK;
				latestBarStatsData[i][2] = Color.BLACK;
				latestBarStatsData[i][3] = Color.BLACK;
				latestBarStatsData[i][4] = 0.00;
				latestBarStatsData[i][5] = 0.0;
				latestBarStatsData[i][6] = 0;
			}

			latestBarStatsData[i][7] = pub.isUsable();
		}

		loggingVisitorProfile();

		Edge[][] matrix = friendFamilyNetwork.getAdjacencyList(true);
		List<Edge> linksToDelete = new ArrayList<Edge>();

		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				double newWeight = (double) matrix[i][j].getInfo() * params.networkEdgeDecayFactor;
				friendFamilyNetwork.updateEdge(matrix[i][j], matrix[i][j].from(), matrix[i][j].to(), newWeight);

				if (newWeight < params.networkEdgeDeletionThreshold) {
					linksToDelete.add(matrix[i][j]);
				}
			}
		}

		// delete weak ties
		for (Edge edge : linksToDelete) {
			if(agents.get((long) edge.from()) == null ) continue;
			agents.get((long) edge.from()).getLoveNeed().lostFriend();
			friendFamilyNetwork.removeEdge(friendFamilyNetwork.getEdge(
					edge.from(), edge.to()));
			try {
				visualFriendFamilyGraph.removeEdge(edge.from().toString()
						+ "--" + edge.to().toString());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		// clear the memory
		matrix = null;
		linksToDelete.clear();

		// agents' nightly routines
		double y = params.initialNetworkEdgeWeight;
		int steps = params.stableRelationshipPeriodInMin / getMinutePerStep();
		for (int i = 0; i < steps; i++) {
			y += params.networkEdgeWeightStrengtheningRate * (1 - y / WorldParameters.NETWORK_WEIGHT_UPPER_BOUND);
			y = Math.min(y, WorldParameters.NETWORK_WEIGHT_UPPER_BOUND);
		}
		double expectedHealthyRelationshipWeight = Math.max(params.networkEdgeDeletionThreshold,
				Math.pow(params.networkEdgeDecayFactor, params.maxLonelyDays) * y);
		// let's make sure we first update the financial safety needs of agents
		for (Person person : this.agents.values()) {
			person.writeAttributesToFile();
			person.getFinancialSafetyNeed().update(); // update financial status
			person.getFoodNeed().resetNumberOfMealsTaken();

			Bag outEdges = friendFamilyNetwork.getEdgesOut(person.getAgentId());

			// calculate social happiness as the average of friendship weight
			double happiness = 0;
			if (outEdges.size() > 0) {
				for (Object edgeObj : outEdges) {
					Edge edge = (Edge) edgeObj;
					double weight = (double) edge.getInfo();
					happiness += weight;
				}
				// ratio of summed relationship weights over expectation
				happiness /= params.maxNumOfFriends * expectedHealthyRelationshipWeight;
			}
			happiness -= params.networkEdgeDeletionThreshold;
			person.getLoveNeed().setSocialHappiness(happiness);
		}

	}

	public void eveningRoutine() {
		logger.info("Evening routine:" + getFormattedDateTime());
		// let's make sure we first update social status value
		for (Person person : this.agents.values()) {
			person.getLoveNeed().update(); // update love need
		}

		// agents plans
		for (Person person : this.agents.values()) {
			DailyPlan plan = person.planForSpecificDay(1);
			LocalDateTime tomorrowDT = getSimulationTime().plusDays(1);
			person.addPlan(tomorrowDT, plan);
		}
	}

	// ROUTINES/PERIODIC METHODS END

	// PLACE SEARCH METHODS

	public List<Restaurant> getNearestRestaurants(MasonGeometry geom, int numberOfRestaurants) {
		List<Restaurant> restaurants = getUsableRestaurants();

		if (restaurants == null) {
			return null;
		}

		restaurants.sort(new Comparator<BuildingUnit>() {
			@Override
			public int compare(BuildingUnit o1, BuildingUnit o2) {
				double a = spatialNetwork.getDistance(geom, o1.getLocation());
				double b = spatialNetwork.getDistance(geom, o2.getLocation());
				if (a == b)
					return Long.compare(o1.getId(), o2.getId());
				return Double.compare(a, b);
			}
		});

		return restaurants.size() > numberOfRestaurants ? restaurants.subList(0, numberOfRestaurants) : restaurants;
	}

	public Restaurant getRestaurant(long id) {
		return restaurants.get(id);
	}

	// public Map<Pub, Double> getAllUsablePubDistanceMap(MasonGeometry geom) {
	// Map<Pub, Double> pubs = new HashMap<Pub, Double>();
	// for (Pub pub : getUsablePubs()) {
	// pubs.put(pub, spatialNetwork.getDistance(geom, pub.getLocation()));
	// }
	// return pubs;
	// }

	public List<Pub> getNearestPubs(MasonGeometry geom, int numberOfPubs) {
		List<Pub> pubs = getUsablePubs();

		if (pubs == null) {
			return null;
		}

		pubs.sort(new Comparator<Pub>() {
			@Override
			public int compare(Pub o1, Pub o2) {
				double a = spatialNetwork.getDistance(geom, o1.getLocation());
				double b = spatialNetwork.getDistance(geom, o2.getLocation());
				if (a == b)
					return Long.compare(o1.getId(), o2.getId());
				return Double.compare(a, b);
			}
		});
		return pubs.size() > numberOfPubs ? pubs.subList(0, numberOfPubs)
				: pubs;
	}

	public Pub getPub(long id) {
		return pubs.get(id);
	}

	public BuildingUnit getClosestBuildingUnitToSupply(MasonGeometry geom) {

		if (buildingUnitsToSupply == null || buildingUnitsToSupply.size() == 0) {
			return null;
		}

		double minDistance = Double.MAX_VALUE;
		double threshold = 10000;
		BuildingUnit unitToReturn = null;

		for (BuildingUnit unit : buildingUnitsToSupply) {

			double dist = getSpatialNetwork().getDistance(unit.getLocation(),
					geom);
			if (dist < minDistance && dist < threshold) {
				minDistance = dist;
				unitToReturn = unit;
			}
		}

		if (unitToReturn != null) {
			buildingUnitsToSupply.remove(unitToReturn);
		}

		return unitToReturn;
	}

	// PLACE SEARCH METHODS END

	// GETTER/SETTER METHODS
	public int getMinutePerStep() {
		SimulationTimeStepSetting timeUnit = params.timeStepUnit;
		if (timeUnit == SimulationTimeStepSetting.MinutePerStep) {
			return params.oneStepTime;
		} else if (timeUnit == SimulationTimeStepSetting.HourPerStep) {
			return params.oneStepTime * 60;
		} else {
			logger.error("Only minute and hour supported");
			return 0;
		}
	}

	public LocalDateTime getSimulationTime() {
		int stepSize = ((Long) this.schedule.getSteps()).intValue();
		int totalTimePassed = stepSize * params.oneStepTime;

		switch (params.timeStepUnit) {
			case MinutePerStep:
				return params.initialSimulationTime.plusMinutes(totalTimePassed);
			case SecondPerStep:
				return params.initialSimulationTime.plusSeconds(totalTimePassed);
			case HourPerStep:
				return params.initialSimulationTime.plusHours(totalTimePassed);
			case DayPerStep:
				return params.initialSimulationTime.plusDays(totalTimePassed);
		}

		return new LocalDateTime();
	}

	public GeomVectorField getAgentLayer() {
		return agentLayer;
	}

	public Building getBuilding(long id) {
		return buildings.get(id);
	}

	/**
	 * Returns all the buildings whether they are usable or not.
	 * 
	 * @return
	 */
	public List<Building> getAllBuildings() {
		return this.buildings.values().stream().collect(Collectors.toList());
	}

	/**
	 * Returns usable buildings.
	 * 
	 * @return
	 */
	public List<Building> getUsableBuildings() {
		return this.buildings.values().stream()
				.filter(p -> p.isUsable() == true).collect(Collectors.toList());
	}

	/**
	 * Returns all the buildings in given neighborhood whether they are usable
	 * or not.
	 * 
	 * @param neighboodId
	 * @return
	 */
	public List<Building> getAllBuildings(int neighboodId) {
		return this.buildings.values().stream()
				.filter(p -> p.getNeighborhoodId() == neighboodId)
				.collect(Collectors.toList());
	}

	/**
	 * Returns usable buildings in given neighborhood.
	 * 
	 * @param neighboodId
	 * @return
	 */
	public List<Building> getUsableBuildings(int neighboodId) {
		return this.buildings
				.values()
				.stream()
				.filter(p -> p.getNeighborhoodId() == neighboodId
						&& p.isUsable() == true)
				.collect(Collectors.toList());
	}

	/**
	 * Returns usable buildings in given neighborhood.
	 * 
	 * @param neighboodId
	 * @return
	 */
	public List<Building> getUsableBuildings(int neighboodId, BuildingType type) {
		return this.buildings
				.values()
				.stream()
				.filter(p -> p.getNeighborhoodId() == neighboodId
						&& p.isUsable() == true
						&& p.getBuildingType().equals(type))
				.collect(Collectors.toList());
	}


	public List<Building> getUsableBuildings(int neighboodId,int regionId, BuildingType type) {
		return this.buildings
				.values()
				.stream()
				.filter(p -> p.getNeighborhoodId() == neighboodId
						&& p.isUsable() == true
						&& p.getBuildingType().equals(type)
						&& p.getRegionID() == regionId)
				.collect(Collectors.toList());
	}


	/**
	 * Returns usable buildings.
	 * 
	 * @param neighboodId
	 * @return
	 */
	public List<Building> getUsableBuildings(BuildingType type) {
		return this.buildings
				.values()
				.stream()
				.filter(p -> p.isUsable() == true
						&& p.getBuildingType().equals(type))
				.collect(Collectors.toList());
	}

	/**
	 * Returns all workplaces.
	 * 
	 * @return
	 */
	public List<Workplace> getAllWorkplaces() {
		return workplaces.values().stream().collect(Collectors.toList());
	}

	/**
	 * Returns all the apartments whether they are usable or not.
	 * 
	 * @return
	 */
	public List<Apartment> getAllApartments() {
		return apartments.values().stream().collect(Collectors.toList());
	}

	/**
	 * Returns usable apartments.
	 * 
	 * @return
	 */
	public List<Apartment> getUsableApartments() {
		return apartments.values().stream().filter(p -> p.isUsable() == true)
				.collect(Collectors.toList());
	}

	/**
	 * Returns usable apartments.
	 * 
	 * @return
	 */
	public List<Apartment> getUsableApartmentsWithAvailableCapacity() {
		return apartments
				.values()
				.stream()
				.filter(p -> p.isUsable() == true
						&& p.getRemainingPersonCapacity() > 0)
				.collect(Collectors.toList());
	}


	public List<Apartment> getUsableApartmentsWithAvailableCapacity(int regionId) {
		return apartments
				.values()
				.stream()
				.filter(p -> p.isUsable() == true
						&& p.getRemainingPersonCapacity() > 0)
				.filter(p -> p.getRegionId() == regionId)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all the classrooms whether they are usable or not.
	 * 
	 * @return
	 */
	public List<Classroom> getAllClassrooms() {
		return classrooms.values().stream().collect(Collectors.toList());
	}

	/**
	 * Returns usable classrooms.
	 * 
	 * @return
	 */
	public List<Classroom> getUsableClassrooms() {
		return classrooms.values().stream().filter(p -> p.isUsable() == true)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all the jobs whether they are available or not.
	 * 
	 * @return
	 */
	public List<Job> getAllJobs() {
		return jobs.values().stream().collect(Collectors.toList());
	}

	/**
	 * Returns only available jobs. Includes filled jobs.
	 * 
	 * @return
	 */
	public List<Job> getAvailableJobs() {
		return jobs.values().stream().filter(p -> p.isAvailable() == true)
				.collect(Collectors.toList());
	}

	/**
	 * Returns only unfilled available jobs.
	 * 
	 * @return
	 */
	public List<Job> getAvailableUnfilledJobs() {
		return jobs.values().stream()
				.filter(p -> p.isAvailable() == true && p.getWorker() == null)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all the pubs whether they are available or not.
	 * 
	 * @return
	 */
	public List<Pub> getAllPubs() {
		return pubs.values().stream().collect(Collectors.toList());
	}

	public List<Pub> getUsablePubs() {
		return pubs.values().stream().filter(p -> p.isUsable() == true)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all the restaurants whether they are available or not.
	 * 
	 * @return
	 */
	public List<Restaurant> getAllRestaurants() {
		return restaurants.values().stream().collect(Collectors.toList());
	}

	public List<Restaurant> getUsableRestaurants() {
		return restaurants.values().stream().filter(p -> p.isUsable() == true)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all the agents.
	 * 
	 * @return
	 */
	public List<Person> getAgents() {
		return this.agents.values().stream().collect(Collectors.toList());
	}

	public List<Person> getAgentsCheckin() {
		return this.agents.values().stream()
				.filter(p -> p.getCurrentUnit() != null && p.getVisitReason() != VisitReason.None)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all the agents in a treemap.
	 * 
	 * @return
	 */
	public Map<Long, Person> getAgentsMap() {
		return this.agents;
	}

	/**
	 * Return an agent by its id.
	 * 
	 * @param id
	 *           agent id
	 * @return
	 */
	public Person getAgent(Long id) {
		return this.agents.get(id);
	}

	public Region getRegion(Integer id) {return  this.regions.get(id);}

	public int getDay() {
		return day;
	}

	public Network getFriendFamilyNetwork() {
		return friendFamilyNetwork;
	}

	public Network getWorkNetwork() {
		return workNetwork;
	}

	public Graph getVisualFriendFamilyGraph() {
		return visualFriendFamilyGraph;
	}

	public Graph getVisualWorkGraph() {
		return visualWorkGraph;
	}

	public SpatialNetwork getSpatialNetwork() {
		return spatialNetwork;
	}

	public long getSimulationSeed() {
		return simulationSeed;
	}

	// GETTER/SETTER METHODS END

	// DATA COLLECTION AND LOGGING-RELATED METHODS
	public void loggingVisitorProfile() {
		for (Pub pub : pubs.values()) {
			if (pub.getVisitorProfile() != null) {
				double age = pub.getVisitorProfile().getAverageAge();
				double income = pub.getVisitorProfile().getAverageIncome();
				List<AgentInterest> interests = pub.getVisitorProfile().getInterests();

				// step, time, site-id, average age, average income, top 3 interests
				String line = schedule.getSteps() + "\t" + getSimulationTime() + "\t" + pub.getId() + "\t" + age + "\t"
						+ income + "\t" + "{" + interests.get(0) + "," + interests.get(1) + "," + interests.get(2)
						+ "}";
				logger.evt5(line);
			}
		}
	}

	public DataCollector dataCollector = new DataCollector();

	public void startDataCollectionForQoIs() {

		quantitiesOfInterest = new QuantitiesOfInterest(getMinutePerStep());

		dataCollector.addWatcher("Infectious", new Collector() {
			private static final long serialVersionUID = 311152044370201L;

			public Double getData() {
				long loggingInterval;

				// percentage infectious agents
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.PERCENTAGE_OF_INFECTIOUS_AGENTS);
				if (schedule.getSteps() % loggingInterval == 0) {

					double value = (double) agents.values().stream()
							.map(Person::getDiseaseStatus)
							.filter(p -> p == InfectionStatus.Infectious).count()
							* 100.0 / agents.size();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.PERCENTAGE_OF_INFECTIOUS_AGENTS,
							value, schedule.getSteps());
					quantitiesOfInterest.percentageInfectious = value;
				}

				return (double) quantitiesOfInterest.percentageInfectious;
			}
		});

		dataCollector.addWatcher("Exposed", new Collector() {
			private static final long serialVersionUID = 311152044370202L;

			public Double getData() {
				long loggingInterval;

				// percentage exposed agents
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.PERCENTAGE_OF_EXPOSED_AGENTS);
				if (schedule.getSteps() % loggingInterval == 0) {

					double value = (double) agents.values().stream()
							.map(Person::getDiseaseStatus)
							.filter(p -> p == InfectionStatus.Exposed).count()
							* 100.0 / agents.size();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.PERCENTAGE_OF_EXPOSED_AGENTS,
							value, schedule.getSteps());
					quantitiesOfInterest.percentageExposed = value;
				}

				return (double) quantitiesOfInterest.percentageExposed;
			}
		});

		dataCollector.addWatcher("Susceptible", new Collector() {
			private static final long serialVersionUID = 311152044370201L;

			public Double getData() {
				long loggingInterval;

				// percentage susceptible agents
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.PERCENTAGE_OF_SUSCEPTIBLE_AGENTS);
				if (schedule.getSteps() % loggingInterval == 0) {

					double value = (double) agents.values().stream()
							.map(Person::getDiseaseStatus)
							.filter(p -> p == InfectionStatus.Susceptible).count()
							* 100.0 / agents.size();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.PERCENTAGE_OF_SUSCEPTIBLE_AGENTS,
							value, schedule.getSteps());
					quantitiesOfInterest.percentageSusceptible = value;
				}

				return (double) quantitiesOfInterest.percentageSusceptible;
			}
		});

		dataCollector.addWatcher("Recovered", new Collector() {
			private static final long serialVersionUID = 311152044370201L;

			public Double getData() {
				long loggingInterval;

				// percentage infectious agents
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.PERCENTAGE_OF_RECOVERED_AGENTS);
				if (schedule.getSteps() % loggingInterval == 0) {

					double value = (double) agents.values().stream()
							.map(Person::getDiseaseStatus)
							.filter(p -> p == InfectionStatus.Recovered).count()
							* 100.0 / agents.size();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.PERCENTAGE_OF_RECOVERED_AGENTS,
							value, schedule.getSteps());
					quantitiesOfInterest.percentageRecovered = value;
				}

				return (double) quantitiesOfInterest.percentageRecovered;
			}
		});

		/*
		dataCollector.addWatcher("Wearing Masks", new Collector() {
			private static final long serialVersionUID = 311152044370201L;

			public Double getData() {
				long loggingInterval;

				// percentage infectious agents
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.PERCENTAGE_OF_WEARING_MASKS);
				if (schedule.getSteps() % loggingInterval == 0) {

					double value = (double) agents.values().stream()
							.map(Person::isWearingMask)
							.filter(p -> p).count()
							* 100.0 / agents.size();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.PERCENTAGE_OF_WEARING_MASKS,
							value, schedule.getSteps());
					quantitiesOfInterest.percentageWearingMasks = value;
				}

				return (double) quantitiesOfInterest.percentageWearingMasks;
			}
		});
		*/
		/*
		// captures all Quantities of Interest
		dataCollector.addWatcher("QoIs", new Collector() {
			private static final long serialVersionUID = 311152044373854L;

			public Double getData() {
				long loggingInterval;

				// average network degree
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.AVERAGE_SOCIAL_NETWORK_DEGREE);
				if (schedule.getSteps() % loggingInterval == 0) {
					Set<Long> keySet = agents.keySet();

					double sum = 0;
					for (Long agentId : keySet) {
						sum += (double) friendFamilyNetwork
								.getEdgesIn(agentId).size();
					}
					double value = sum / keySet.size();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.AVERAGE_SOCIAL_NETWORK_DEGREE,
							value, schedule.getSteps());
					quantitiesOfInterest.avgNetworkDegree = value;
				}

				// average balance
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.AVERAGE_BALANCE);
				if (schedule.getSteps() % loggingInterval == 0) {

					double value = agents
							.values()
							.stream()
							.map(Person::getFinancialSafetyNeed)
							.mapToDouble(
									FinancialSafetyNeed::getAvailableBalance)
							.average().getAsDouble();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.AVERAGE_BALANCE, value,
							schedule.getSteps());
					quantitiesOfInterest.avgBalance = value;
				}

				// percentage of unhappy agents
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.PERCENTAGE_OF_UNHAPPY_AGENTS);
				if (schedule.getSteps() % loggingInterval == 0) {

					double value = (double) agents.values().stream()
							.map(Person::getLoveNeed)
							.filter(p -> p.isSatisfied() == false).count()
							* 100.0 / agents.size();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.PERCENTAGE_OF_UNHAPPY_AGENTS,
							value, schedule.getSteps());
					quantitiesOfInterest.percentageUnhappy = value;
				}

				// pub visits per agent
				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.PUB_VISITS_PER_AGENT);
				if (schedule.getSteps() % loggingInterval == 0) {

					double value = quantitiesOfInterest.getPubVisitCount();
					value /= agents.size();
					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.PUB_VISITS_PER_AGENT, value,
							schedule.getSteps());
					quantitiesOfInterest.resetPubVisitorCount();
					quantitiesOfInterest.pubVisitPerAgent = value;
				}

				// number of interactions
				// we first capture all existing interactions
				for (Pub pub : getAllPubs()) {
					List<Meeting> meetings = pub.getAllMeetings();
					for (Meeting meeting : meetings) {
						quantitiesOfInterest.captureInteractions(meeting,
								schedule.getSteps());
					}
				}

				for (Restaurant restaurant : getAllRestaurants()) {
					List<Meeting> meetings = restaurant.getAllMeetings();
					for (Meeting meeting : meetings) {
						quantitiesOfInterest.captureInteractions(meeting,
								schedule.getSteps());
					}
				}

				loggingInterval = quantitiesOfInterest
						.getLoggingInterval(QuantitiesOfInterest.NUM_OF_SOCIAL_INTERACTIONS);
				if (schedule.getSteps() % loggingInterval == 0) {
					List<AgentInteraction> agentInteractions = quantitiesOfInterest
							.getAgentInteractions();

					quantitiesOfInterest.addValue(
							QuantitiesOfInterest.NUM_OF_SOCIAL_INTERACTIONS,
							(double) agentInteractions.size(),
							schedule.getSteps());
					quantitiesOfInterest.numOfSocialInteractions = agentInteractions.size();
					agentInteractions.clear();
				}

				// For the sake of performance, return 1.0.
				return 1.0;
				// You can return a value you are interested.
				// For instance, if you want to monitor average balance, use the following code.
				// return (double) quantitiesOfInterest.avgBalance;
				// return (double) quantitiesOfInterest.percentageInfectious;
			}
		});
		 */

		dataCollector.addWatcher("barStats", new Collector() {
			private static final long serialVersionUID = 7722307416790950096L;

			public Object[][] getData() {
				return latestBarStatsData;
			}
		});
	}

	@Skip
	private EventJournalSettings journalSetting;

	public EventJournalSettings getJournalSettings() {

		if (journalSetting == null) {
			// do not add anything below to not to capture anything
			journalSetting = new EventJournalSettings()
					.addMode(PersonMode.AtRecreation)
					.addMode(PersonMode.AtRestaurant)
					.addMode(PersonMode.AtWork)
					.addMode(PersonMode.AtHome);
		}
		return journalSetting;
	}

	// DATA COLLECTION AND LOGGING-RELATED METHODS END

	// MISC METHODS

	public int getNumberOfPeopleByPlaceId(long id) {
		return (int) this.getAgents().stream()
				.filter(p -> p.getCurrentUnit() != null && p.getCurrentUnit().getId() == id).count();
	}

	public void incrementNumberOfAbondenedAgents() {
		numOfAbondenedAgents++;
	}

	public void incrementNumberOfDeadAgents() {
		numOfDeadAgents++;
	}

	public String getFormattedDateTime() {
		return "Day #" + day + " " + getSimulationTime().toString("E @ HH:mm");
	}

	// MISC METHODS END

	// Since visibility problem, we redefine this method.
	static String argumentForKey(String key, String[] args) {
		for (int x = 0; x < args.length - 1; x++)
			// if a key has an argument, it can't be the last string
			if (args[x].equalsIgnoreCase(key))
				return args[x + 1];
		return null;
	}

	public static void doLoop(final Class c, String[] args) {
		doLoop(new MakesSimState() {
			public SimState newInstance(long seed, String[] args) {
				try {
					String configurationPath = argumentForKey("-configuration",
							args);
					WorldParameters params = new WorldParameters();
					if (configurationPath != null) {
						params = new WorldParameters(configurationPath);
					}

					String biasConfigPath = argumentForKey("-bias.config",
							args);
					BiasParameters biasParameters = new BiasParameters();
					if (biasConfigPath!= null) {
						biasParameters = new BiasParameters(biasConfigPath);
					}


					String biasSingleConfigPath = argumentForKey("-bias.single.config",
							args);
					BiasSingleParameters biasSingleParameters = new BiasSingleParameters();
					if (biasConfigPath!= null) {
						biasSingleParameters = new BiasSingleParameters(biasSingleConfigPath);
					}

					return (SimState) (c.getConstructor(new Class[] {
							Long.TYPE, WorldParameters.class, BiasParameters.class, BiasSingleParameters.class }).newInstance(new Object[] {
							Long.valueOf(params.seed), params , biasParameters, biasSingleParameters}));

				} catch (Exception e) {
					throw new RuntimeException(
							"Exception occurred while trying to construct the simulation "
									+ c + "\n" + e);
				}
			}

			public Class simulationClass() {
				return c;
			}
		}, args);
	}

	public static void main(String[] args) {
		doLoop(WorldModel.class, args);
		System.exit(0);
	}

	public Viewer networkViewer() {
		return friendFamilyViewer;
	}

	public QuantitiesOfInterest getQuantitiesOfInterest() {
		return quantitiesOfInterest;
	}

	public Map<Integer, List<Building>> getNeighborhoodBuildingMap() {
		return neighborhoodBuildingMap;
	}

	public void addLogSchedule(LogSchedule schedule) {
		logScheduler.add(schedule);
	}

	public void changeRandomGeneratorState(Double times) {
		// This will change the seed of the random number generator
		random.setSeed(times.longValue());
	}
}
