package com.axonivy.solutions.process.analyser.utils;

import java.util.List;

public class ColorUtils {

	public static String calculateColorFromList(Double value, List<String> colors) {
		int index = (int) Math.floor(value * colors.size());
		index = Math.min(Math.max(index, 0), colors.size() - 1);
		return colors.get(index);
	}

	public static String getAccessibleTextColor(String color) {
		int r, g, b;
		if (color.startsWith("#")) {
			int val = Integer.parseInt(color.substring(1), 16);
			r = (val >> 16) & 0xFF;
			g = (val >> 8) & 0xFF;
			b = val & 0xFF;
		} else if (color.startsWith("rgb")) {
			String[] parts = color.replaceAll("[^\\d,]", "").split(",");
			r = Integer.parseInt(parts[0]);
			g = Integer.parseInt(parts[1]);
			b = Integer.parseInt(parts[2]);
		} else {
			throw new IllegalArgumentException("Unsupported color: " + color);
		}
		return (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.5 ? "#000000" : "#FFFFFF";
	}

}
