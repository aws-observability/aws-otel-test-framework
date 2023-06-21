package com.amazon.aoc.validators;

import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.fileconfigs.LocalPathExpectedTemplate;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;

@Log4j2
public class ContainerInsightPrometheusStructuredLogValidator extends AbstractStructuredLogValidator {

	/**
	 * We need a bigger retry (12 instead of 6) because we have multiple tasks
	 * running on a single EC2 instance. It takes a while for the EC2 instance to
	 * register itself as ECS container instance.
	 */
	private static final int MAX_RETRY_COUNT = 12;
	private List<CloudWatchContext.App> validateApps;

	@Override
	void init(Context context, FileConfig expectedDataTemplate) throws Exception {
		logGroupName = String.format("/aws/containerinsights/%s/prometheus",
				context.getCloudWatchContext().getClusterName());
		validateApps = getAppsToValidate(context.getCloudWatchContext());
		MustacheHelper mustacheHelper = new MustacheHelper();

		for (CloudWatchContext.App app : validateApps) {
			FileConfig fileConfig = new LocalPathExpectedTemplate(
					FilenameUtils.concat(expectedDataTemplate.getPath().toString(), app.getName() + ".json"));
			String templateInput = mustacheHelper.render(fileConfig, context);
			schemasToValidate.put(app.getNamespace(), parseJsonSchema(templateInput));
			logStreamNames.add(app.getJob());
		}
	}

	@Override
	public int getMaxRetryCount() {
		return MAX_RETRY_COUNT;
	}

	@Override
	String getJsonSchemaMappingKey(JsonNode logEventNode) {
		return logEventNode.get("Namespace").asText();
	}

	private static List<CloudWatchContext.App> getAppsToValidate(CloudWatchContext cwContext) {
		List<CloudWatchContext.App> apps = new ArrayList<>();
		if (cwContext.getAppMesh() != null) {
			apps.add(cwContext.getAppMesh());
		}
		if (cwContext.getNginx() != null) {
			apps.add(cwContext.getNginx());
		}
		if (cwContext.getHaproxy() != null) {
			apps.add(cwContext.getHaproxy());
		}
		if (cwContext.getJmx() != null) {
			apps.add(cwContext.getJmx());
		}
		if (cwContext.getMemcached() != null) {
			apps.add(cwContext.getMemcached());
		}
		return apps;
	}

}