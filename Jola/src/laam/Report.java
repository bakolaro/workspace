package laam;

public class Report {
	private static final int MAX_DEPTH = 20;

	private long[] latest = new long[MAX_DEPTH], total = new long[MAX_DEPTH];

	// private int[] cntr=new int[MAX_DEPTH];

	private final int WIDTH = 72;

	public Report() {
		long ctms = System.currentTimeMillis();
		for (int i = 0; i < MAX_DEPTH; i++)
			latest[i] = ctms;
	}

	public void printFileDetails(String name, double size, char up, char side,
			char down) {
		StringBuffer buff = new StringBuffer(WIDTH + 2);

		buff.append('\n');
		buff.append(' ');
		for (int i = 0; i < WIDTH - 2; i++)
			buff.append(up);
		buff.append(' ');
		buff.append('\n');
		System.out.print(buff);

		System.out.printf(side + " %-56s%7.3f%6s%n", name, size, "  MB  "
				+ side);

		buff = new StringBuffer(WIDTH + 2);
		buff.append(' ');
		for (int i = 0; i < WIDTH - 2; i++)
			buff.append(down);
		buff.append(' ');
		buff.append('\n');
		System.out.print(buff);
	}

	public void start(int depth) {
		this.latest[depth] = System.currentTimeMillis();
	}

	public double printTime(String s, int depth) {
		long time = System.currentTimeMillis();
		this.total[depth] = time - this.latest[depth];
		this.latest[depth] = time;

		s = "- " + s;
		for (int i = 0; i < depth; i++)
			s = "  " + s;
		System.out.printf("  %-56s%7.3f%5s%n", s,
				(double) this.total[depth] / 1000, " сек.");
		return (double) this.total[depth] / 1000;
	}
}
