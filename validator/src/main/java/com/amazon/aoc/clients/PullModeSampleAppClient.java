package com.amazon.aoc.clients;

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PullModeSampleAppClient<T> {
	private static final int MAX_RETRY_COUNT = 30;

	private final String endpoint;
	private final JavaType type;

	/**
	 * construct PullModeSampleAppClient.
	 */
	public PullModeSampleAppClient(Context context, String expectedResultPath, JavaType type) {
		this.endpoint = context.getEndpoint() + expectedResultPath;
		this.type = type;
	}

	/**
	 * list expected results from the sample app.
	 *
	 * @return returns a list of objects
	 */
	public T getExpectedResults() throws Exception {
		Request request = new Request.Builder().url(endpoint).build();
		return execute(request);
	}

	private T execute(Request request) throws Exception {
		AtomicReference<T> result = new AtomicReference<>();
		RetryHelper.retry(MAX_RETRY_COUNT, () -> {
			OkHttpClient client = new OkHttpClient.Builder().retryOnConnectionFailure(true)
					.connectTimeout(5, TimeUnit.SECONDS).build();
			Response response = client.newCall(request).execute();

			if (response.code() >= 300) {
				throw new BaseException(ExceptionCode.PULL_MODE_SAMPLE_APP_CLIENT_REQUEST_FAILED,
						response.body().string());
			}

			String body = response.body().string();
			result.set(new ObjectMapper().readValue(body, type));
		});
		return result.get();
	}
}
