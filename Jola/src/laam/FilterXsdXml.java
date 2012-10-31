package laam;

import java.io.File;
import java.io.FilenameFilter;

public class FilterXsdXml implements FilenameFilter {
	public boolean accept(File dir, String name) {
		String s = name.toLowerCase();
		return s.endsWith(".xsd") || s.endsWith(".xml");
	}
}
