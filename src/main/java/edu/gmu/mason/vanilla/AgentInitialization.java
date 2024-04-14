package edu.gmu.mason.vanilla;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import edu.gmu.mason.vanilla.environment.NeighborhoodComposition;
import edu.gmu.mason.vanilla.utils.MersenneTwisterWrapper;
import scala.util.Random;
import sim.app.lsystem.LSystem;



/**
 * General description_________________________________________________________
 * This class used to specify the initial values of agent parameters.
 * 
 * @author Hamdi Kavak (hkavak at gmu.edu), Umar Manzoor (umanzoor at
 *         tulane.edu)
 * 
 */
public class AgentInitialization {

	private MersenneTwisterWrapper rng;
	private WorldParameters params;

	public AgentInitialization(WorldModel model) {
		this.rng = new MersenneTwisterWrapper(model.random);
		this.params = model.params;
	}

	public double generateInitialBalance(EducationLevel level) {
		UniformRealDistribution uRNG = new UniformRealDistribution(rng,
				params.initialAdditionalBalanceLowerBound,
				params.initialAdditionalBalanceUpperBound);

		return params.baseInitialBalance + uRNG.sample()
				* NeighborhoodComposition.getPayScale(level);
	}

	public AgentInterest getAgentInterest() {
		UniformIntegerDistribution uRNG = new UniformIntegerDistribution(rng,
				1, params.numOfAgentInterests);
		return AgentInterest.valueOf(uRNG.sample());
	}

	/**
	 * Generates joviality (socialble) value for agents. Possible values are
	 * 0.0, 0.5, and 1.0
	 * 
	 * @return
	 */
	public double generateJovialityValue() {
		UniformRealDistribution uRNG = new UniformRealDistribution(rng, 0,1);

		return uRNG.sample();
	}

	public double generateAppetiteNumber() {
		UniformRealDistribution uRNG = new UniformRealDistribution(rng,
				params.appetiteLowerBound, params.appetiteUpperBound);

		return uRNG.sample();
	}

	// Random Assigned Attributes
	public int generateAgentAge() {
		UniformIntegerDistribution uRNG = new UniformIntegerDistribution(rng,
				params.additionalAgentAgeMin, params.additionalAgentAgeMax);

		return params.baseAgentAge + uRNG.sample();
	}

	public EducationLevel generateEducationLevel() {
		UniformRealDistribution uRNG = new UniformRealDistribution(rng, 0.0,
				params.EDUCATION_REQ_GRADUATE);
		double percentile = uRNG.sample();

		if (percentile <= params.EDUCATION_REQ_LOW) {
			return EducationLevel.Low;
		} else if (percentile <= params.EDUCATION_REQ_HS_COLLEGE) {
			return EducationLevel.HighSchoolOrCollege;
		} else if (percentile <= params.EDUCATION_REQ_BACHELORS) {
			return EducationLevel.Bachelors;
		} else if (percentile <= params.EDUCATION_REQ_GRADUATE) {
			return EducationLevel.Graduate;
		}
		return EducationLevel.Unknown;
	}


	// Census Data Based assigning attributes
	public int generateAgentAge(Region region) {
		AgeGroup ageGroup = region.newAgeGroupAssigned();
		int minAge = 15 + ageGroup.index * 5;
		UniformIntegerDistribution uRNG = new UniformIntegerDistribution(rng, minAge, minAge+4);
		return uRNG.sample();
	}

	public Race generateAgentRace(Region region){
		return region.newRaceAssigned();
	}

	public EducationLevel generateEducationLevel(Region region){
		return region.newEduLevelAssigned();
	}

	public int generateAgentHHCensusIncome(Region region){
		return region.newIncomeAssigned();
	}

	public int generateAgentIndivCensusIncome(Region region){
		return region.newIndivIncomeAssigned();
	}

	public boolean isMale(Region region){
		return region.newGender();
	}

	public boolean isHispanic(Region region){
		return region.newHispanic();
	}

	public boolean inProvince(Region region){
		return region.newInProvince();
	}
}
