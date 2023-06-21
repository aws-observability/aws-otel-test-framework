package com.amazon.aoc.fileconfigs;

import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class PredefinedExpectedTemplateTest {
	@Test
	public void ensureTemplatesAreExisting() throws IOException {
		for (PredefinedExpectedTemplate predefinedExpectedTemplate : PredefinedExpectedTemplate.values()) {
			URL path = predefinedExpectedTemplate.getPath();
			// also check if tostring can return a valid filepath
			IOUtils.toString(new URL(path.toString()));
		}
	}
}
