package edu.gmu.mason.vanilla;

/**
 * General description_________________________________________________________
 * An enumeration to represent agent interests. Letters from A to J represents
 * unique interests while NA represents no interest.
 * 
 * @author Hamdi Kavak (hkavak at gmu.edu)
 * 
 */
public enum Race {
	WhiteOnly(0), BlackOnly(1), AmerIndianOnly(2), AsianOnly(3), PacIslandOnly(4), Other(5), Plus2Races(6);

	private int index;

	private Race(int index) {
		this.index = index;
	}

	public static Race valueOf(int index) {
		for (Race b : Race.values()) {
			if (b.index == index)
				return b;
		}
		return null;
	}

	public int getValue() {
		return this.index;
	}
}
