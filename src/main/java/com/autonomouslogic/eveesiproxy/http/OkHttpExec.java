package com.autonomouslogic.eveesiproxy.http;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class OkHttpExec {
	/**
	 * Executes the supplied call using OkHttp's async API and blocks the current thread to wait for it.
	 * @param call
	 * @return
	 */
	public static Response execute(Call call) {
		var future = new CompletableFuture<Response>();
		call.enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException e) {
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
				future.complete(response);
			}
		});
		return future.join();
	}
}
