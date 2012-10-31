public class SudoQ {
	int p, cubeSize, cubeBase, cubeVolume, solutionsCount;
	int[][][] cube;

	public SudoQ(int innerSquareSize){
		p = innerSquareSize;
		cubeSize = p * p;
		cubeBase = cubeSize * cubeSize;
		cubeVolume = cubeBase * cubeSize;
		solutionsCount = 0;
		cube = new int[cubeSize][cubeSize][cubeSize];
	}
	
	public void printCubeBase() {
		for (int row = 0; row < cubeSize; row++) {
			for (int column = 0; column < cubeSize; column++) {
				String on = new String();
				for (int layer = 0; layer < cubeSize; layer++) {
					if (cube[row][column][layer] > 0) {
						on += (layer + 1);
					}
				}
				System.out.printf("%1$" + cubeSize + "s", on);
			}
			System.out.println();
		}
		System.out.println();
	}

	public boolean turnOn(int row, int column, int layer,
			boolean constant) {
		int id;
		if (constant) {
			id = cubeVolume + 1;
		} else {
			id = row * cubeBase + column * cubeSize + layer + 1;
		}

		if (cube[row][column][layer] == 0) {
			int r = (row / p) * p;
			int c = (column / p) * p;
			for (int x = 0; x < cubeSize; x++) {
				if (cube[x][column][layer] == 0) {
					cube[x][column][layer] = (x == row ? id : -id);
				}
				if (cube[row][x][layer] == 0) {
					cube[row][x][layer] = (x == column ? id : -id);
				}
				if (cube[row][column][x] == 0) {
					cube[row][column][x] = (x == layer ? id : -id);
				}
				if (cube[r + x / p][c + x % p][layer] == 0) {
					cube[r + x / p][c + x % p][layer] = ((r + x / p == row)
							&& (c + x % p == column) ? id : -id);
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean turnOff(int row, int column, int layer,
			boolean constant) {
		int id;
		if (constant) {
			id = cubeVolume + 1;
		} else {
			id = row * cubeBase + column * cubeSize + layer + 1;
		}

		if (cube[row][column][layer] > 0) {
			int r = (row / p) * p;
			int c = (column / p) * p;
			for (int x = 0; x < cubeSize; x++) {
				if (cube[x][column][layer] == -id
						|| cube[x][column][layer] == id) {
					cube[x][column][layer] = 0;
				}
				if (cube[row][x][layer] == -id || cube[row][x][layer] == id) {
					cube[row][x][layer] = 0;
				}
				if (cube[row][column][x] == -id || cube[row][column][x] == id) {
					cube[row][column][x] = 0;
				}
				if (cube[r + x / p][c + x % p][layer] == -id
						|| cube[r + x / p][c + x % p][layer] == id) {
					cube[r + x / p][c + x % p][layer] = 0;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean setConstants(int[][] constants) {
		int row, column, layer;
		for (int i = 0; i < constants.length; i++) {
			row = constants[i][0];
			column = constants[i][1];
			layer = constants[i][2];
			if (!turnOn(row, column, layer, true)) {
				return false;
			}
		}
		return true;
	}

	public int printSolutionsCount(int row, int column, boolean printSolutions) {
		int count = 0;
		row += column / cubeSize;
		column %= cubeSize;
		if (row < cubeSize) {
			for (int layer = 0; layer < cubeSize; layer++) {
				if (cube[row][column][layer] == cubeVolume + 1) {
					count += printSolutionsCount(row, column + 1, printSolutions);
				} else if (turnOn(row, column, layer, false)) {
					count += printSolutionsCount(row, column + 1, printSolutions);
					turnOff(row, column, layer, false);
				}
			}
		} else {
			count = 1;
			if (printSolutions) {
				System.out.println(++solutionsCount);
				printCubeBase();
			}
		}
		return count;
	}

	public static void main(String[] args) {
		System.out.println("Hello, Sudoku! I am SudoQ.");
		System.out.println();

		SudoQ g = new SudoQ(3);
		if (g.setConstants(new int[][] {
				{ 0, 0, 5 },
				{ 0, 1, 1 },
				{ 0, 8, 8 },
				{ 1, 0, 2 },
				{ 1, 2, 0 },
				{ 1, 3, 8 },
				{ 1, 5, 5 },
				{ 1, 7, 6 },
				{ 1, 8, 1 },
				{ 2, 3, 4 },
				{ 2, 4, 1 },
				{ 3, 0, 6 },
				{ 3, 1, 4 },
				{ 4, 0, 0 },
				{ 4, 1, 8 },
				{ 4, 3, 2 },
				{ 4, 4, 5 },
				{ 4, 5, 7 },
				{ 4, 7, 4 },
				{ 4, 8, 6 },
				{ 5, 7, 0 },
				{ 5, 8, 3 },
				{ 6, 4, 0 },
				{ 6, 5, 3 },
				{ 7, 0, 3 },
				{ 7, 1, 0 },
				{ 7, 3, 1 },
				{ 7, 5, 6 },
				{ 7, 6, 8 },
				{ 7, 8, 2 },
				{ 8, 0, 7 },
				{ 8, 7, 3 },
				{ 8, 8, 4 } })) {
			g.printCubeBase();

			System.out.println();
			long then = System.currentTimeMillis();
			System.out.printf("Total count: %1$s (P = %2$d)",
					g.printSolutionsCount(0, 0, true), g.p);
			System.out.println();
			long now = System.currentTimeMillis();
			System.out.printf("Time elapsed: %1$d milliseconds.", now - then);
			System.out.println();
			
		} else {
			System.out.println("Invalid input!");
		}
		
		System.out.println("Bye, SudoQ!");
	}
}