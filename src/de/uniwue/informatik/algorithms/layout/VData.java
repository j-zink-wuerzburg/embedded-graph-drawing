package de.uniwue.informatik.algorithms.layout;

public class VData<V> {

	public enum VType {
		REGULAR,
		BEND_POINT,
		CROSSING_POINT;
	}
	
	
	private VType vType;
	private V v;
	

	public VData(VType vType) {
		this.vType = vType;
	}
	
	public VData(V v) {
		this.vType = VType.REGULAR;
		this.v = v;
	}

	public VType getVType() {
		return vType;
	}
	
	public V getV() {
		return v;
	}
	
	@Override
	public String toString() {
		if (vType == VType.BEND_POINT) {
			return "bend_point";
		}
		if (vType == VType.CROSSING_POINT) {
			return "crossing_point";
		}
		if (vType == VType.REGULAR) {
			return v == null ? "regular_point_without_data" : v.toString();
		}
		return super.toString();
	}
}