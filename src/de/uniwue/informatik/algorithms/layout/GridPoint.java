package de.uniwue.informatik.algorithms.layout;

import java.awt.geom.Point2D;

public class GridPoint {
	
    public GridPoint(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}

	private int x;
    
    private int y;

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
	
	public void setLocation(int x, int y) {
		setX(x);
		setY(y);
	}
	
	public void setLocation(GridPoint otherGridPoint) {
		setLocation(otherGridPoint.getX(), otherGridPoint.getY());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GridPoint other = (GridPoint) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GridPoint [x=" + x + ", y=" + y + "]";
	}
	
	public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
	
	
	public Point2D.Double transform2Point2D() {
		
		return new Point2D.Double(x, y);
	}
}
