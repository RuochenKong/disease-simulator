package edu.gmu.mason.vanilla;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;
import org.joda.time.LocalDateTime;

import edu.gmu.mason.vanilla.log.Characteristics;
import edu.gmu.mason.vanilla.log.Skip;

/**
 * General description_________________________________________________________
 * A data structure class to represent the meeting of multiple agents at a
 * location.
 * 
 * @author Hamdi Kavak (hkavak at gmu.edu), Joon-Seok Kim (jkim258 at gmu.edu)
 * 
 */
@SuppressWarnings({"unused"})
public class Meeting implements java.io.Serializable {
	private static final long serialVersionUID = -4990129507126182596L;
	@Skip
	private boolean planned;
	private String meetingId;
	private LocalDateTime startTime;
	private List<Long> participants;

	private List<Double> infectionChances;
	private List<Long> infectionAgents;


	public Meeting(boolean planned, LocalDateTime startTime, String meetingId) {
		this.infectionChances = new ArrayList<>();
		this.infectionAgents = new ArrayList<>();
		this.planned = planned;
		this.startTime = startTime;
		participants = new ArrayList<>();
		this.meetingId = meetingId;
	}

	/**
	 * 
	 * @return {@code true} if planned, {@code false} if spontaneous.
	 */
	public boolean isPlanned() {
		return planned;
	}

	public void addParticipant(long agentId) {
		if (agentExists(agentId) == false) {
			this.participants.add(agentId);
		}
	}

	public void removeParticipant(Long agentId) {
		Iterator<Long> participantsIter = participants.iterator();
		while (participantsIter.hasNext()) {
			Long personId = participantsIter.next();
			if (personId.longValue() == agentId.longValue()) {
				participantsIter.remove();
				break;
			}
		}
		int idx = infectionAgents.indexOf(agentId);
		if (idx != -1){
			infectionAgents.remove(idx);
			infectionChances.remove(idx);
		}
	}

	private boolean agentExists(long personId) {
		for (Long prsId : participants) {
			if (prsId == personId) {
				return true;
			}
		}
		return false;
	}

	public int size() {
		return participants.size();
	}

	public List<Long> getParticipants() {
		return participants;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public double getInfectionChance(){
		if (this.infectionChances.isEmpty())
			return 0;
		return (double)Collections.max(this.infectionChances);
	}

	public void infectedAgentJoin(Long agentID, double agentInfectionChance){
		this.infectionChances.add(agentInfectionChance);
		this.infectionAgents.add(agentID);
	}

}
