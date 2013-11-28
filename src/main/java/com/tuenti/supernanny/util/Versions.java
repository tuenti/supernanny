package com.tuenti.supernanny.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import com.tuenti.supernanny.SuperNannyError;

public class Versions {
	private static final Pattern splitter = Pattern.compile("[.]");

	/**
	 * Sort versions. Return latest version.
	 * 
	 * @param versions
	 *            list of versions.
	 * @return most recent version.
	 */
	public static Version getLatestVersion(Version[] versions) {
		if (versions.length == 0) {
			return null;
		}
		if (versions.length == 1) {
			return versions[0];
		}
		// default order is descending - so chose min
		return Collections.min(Arrays.asList(versions), getVersionComparator());
	}

	/**
	 * Get a version comparator for descending order
	 * 
	 * @return Descending order version comparator
	 */
	public static Comparator<Version> getVersionComparator() {
		return getVersionComparator(true);
	}

	/**
	 * Comparator for versions where versions are strings (may contain numeric
	 * or alphanumeric version elements) separated by [.,-] Numeric comparison
	 * is applied if both components in the same position are numeric.
	 * 
	 * @param descending
	 *            If the order is descending or ascending
	 * 
	 * @return
	 */
	public static Comparator<Version> getVersionComparator(final boolean descending) {
		return new Comparator<Version>() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public int compare(Version v1, Version v2) {
				int order = (descending ? -1 : 1);
				if (v1.equals(v2)) {
					return 0;
				}
				Comparable[] l1 = v1.getParsedVersion();
				Comparable[] l2 = v2.getParsedVersion();
				int minLength = Math.min(l1.length, l2.length);
				for (int i = 0; i < minLength; i++) {
					Comparable c1 = l1[i];
					Comparable c2 = l2[i];

					int tmp;
					if (c2.getClass() == c1.getClass()) {
						tmp = c1.compareTo(c2);
					} else {
						// this case shouldn't occur often
						tmp = c1.toString().compareTo(c2.toString());
					}
					if (tmp != 0) {
						return order * tmp;
					}
				}
				// if they're equal until the minimum length, the longer one is
				// greater
				return order * new Integer(l1.length).compareTo(l2.length);
			}
		};
	}

	public static Version getNextVersion(String format, Version latest) {
		String[] formatParts = splitter.split(format);
		@SuppressWarnings("rawtypes")
		Comparable[] latestParts = latest.getParsedVersion();

		if (latestParts.length < formatParts.length) {
			System.out
					.println(MessageFormat
							.format("Entered format has too many parts -- current latest version is {0}, which has only {1} parts, while the given format {2} has {3}.",
									latest.toString(), latestParts.length, format,
									formatParts.length));
		}

		StringBuilder nextVersion = new StringBuilder();
		boolean didIncrease = false;

		for (int i = 0; i < latestParts.length; i++) {
			if (didIncrease) {
				if (formatParts.length > i) {
					throw new SuperNannyError(
							"Given format is not correct; cannot contain anything after the first +.");
				}
				// already increased, just pad 0
				nextVersion.append(0);
			} else if (formatParts[i].equals("x")) {
				// use
				nextVersion.append(latestParts[i]);
			} else if (formatParts[i].equals("+")) {
				// increase
				if (!didIncrease) {
					nextVersion.append(1 + Integer.parseInt(latestParts[i].toString()));
					didIncrease = true;
				} else {
					throw new SuperNannyError(
							"Given format is not correct; can only contain a single +.");
				}
			} else {
				throw new SuperNannyError(
						"Entered format is wrong; can only contain delimiters, + and x.");
			}

			// add the format delimiter if not last iteration
			if (latestParts.length > i + 1) {
				nextVersion.append(".");
			}
		}

		return new Version(nextVersion.toString());
	}

	public static boolean isDifferentMajor(Version version, Version version2) {
		String[] v1Parts = splitter.split(version.getVersionString());
		String[] v2Parts = splitter.split(version2.getVersionString());
		return !v1Parts[0].equals(v2Parts[0]);
	}

	@SuppressWarnings("rawtypes")
	public static Comparable[] parse(String version) {
		if ("".equals(version)) {
			return new Long[]{};
		}
		String[] splitVersion = splitter.split(version);
		Comparable[] v = new Comparable[splitVersion.length];

		for (int j = 0; j < splitVersion.length; j++) {
			try {
				// try to parse as numbers
				v[j] = Long.parseLong(splitVersion[j]);
			} catch (NumberFormatException e) {
				v[j] = splitVersion[j];
			}
		}
		return v;
	}

	@SuppressWarnings("rawtypes")
	public static boolean startsWith(Version version, Version prefix) {
		Comparable[] v = version.getParsedVersion();
		Comparable[] p = prefix.getParsedVersion();

		if (v.length < p.length) {
			return false;
		}

		for (int i = 0; i < p.length; i++) {
			if (!v[i].equals(p[i])) {
				return false;
			}
		}

		return true;
	}

	public static Version[] sort(Version[] versions) {
		List<Version> list = Arrays.asList(versions);
		Collections.sort(list ,getVersionComparator());
		return list.toArray(new Version[]{});
	}
}
