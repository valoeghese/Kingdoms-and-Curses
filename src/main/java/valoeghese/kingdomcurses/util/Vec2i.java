package valoeghese.kingdomcurses.util;

import java.util.Objects;

public class Vec2i {
	public Vec2i(int x, int y) {
		this.x = x;
		this.y = y;
	}

	private final int x;
	private final int y;

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int manhattan(int x, int y) {
		return manhattan(this.x, this.y, x, y);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}

		Vec2i other = (Vec2i) o;

		return this.x == other.x && this.y == other.y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.x, this.y, 3);
	}

	@Override
	public String toString() {
		return "Vec2i(" + this.x
				+ ", " + this.y
				+ ')';
	}

	private static int manhattan(int x0, int y0, int x1, int y1) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		return dx + dy;
	}
}