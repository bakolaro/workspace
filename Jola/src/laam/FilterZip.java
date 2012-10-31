package laam;

import java.io.File;
import java.io.FilenameFilter;

public class FilterZip implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return name.toLowerCase().endsWith(".zip");
	}
}
