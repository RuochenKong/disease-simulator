package edu.gmu.mason.vanilla;

/**
 * General description_________________________________________________________
 * An enumeration to represent agent interests. Letters from A to J represents
 * unique interests while NA represents no interest.
 * 
 * @author Hamdi Kavak (hkavak at gmu.edu)
 * 
 */
public enum AgeGroup {
	Age15to19(0), Age20to24(1), Age25to29(2), Age30to34(3), Age35to39(4), Age40to44(5),
	Age45to49(6), Age50to54(7), Age55to59(8), Age60to64(9);

	int index;

	private AgeGroup(int index) {
		this.index = index;
	}

	public static AgeGroup valueOf(int index) {
		for (AgeGroup b : AgeGroup.values()) {
			if (b.index == index)
				return b;
		}
		return null;
	}
}
