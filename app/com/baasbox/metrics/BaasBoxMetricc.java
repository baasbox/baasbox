package com.baasbox.metrics;

import com.codahale.metrics.MetricRegistry;

public class BaasBoxMetricc {
	private static MetricRegistry metricRegistry;

    static {
        metricRegistry = new MetricRegistry();
    }

    public static MetricRegistry get() {
        return metricRegistry;
    }
}
