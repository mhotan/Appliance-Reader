package uw.cse.mag.appliancereader.util;

public class Util {

	/**
	 * 
	 * REquires path is not null
	 * @param path
	 * @return
	 */
	public static String stripPathAndExtension(String path) {
		// Strip away .../something.../<name>.xml
		// to <name>
		String[] split = path.split("/");
		String s = split[split.length-1];
		split = s.split("\\");
		s = split[split.length-1];
		split = s.split(".");
		return split[0];
	}

}
