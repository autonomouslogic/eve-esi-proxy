package com.autonomouslogic.esiproxy;

import io.micronaut.runtime.Micronaut;

/**
 * Entry point for the stand-alone application.
 */
public class Application {
	public static void main(String[] args) {
		Micronaut.build(args).banner(false).start();
	}
}
