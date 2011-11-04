package org.jboss.as.console.client.shared.runtime.vm;

/**
 * @author Heiko Braun
 * @date 10/7/11
 */
public interface VMMetricsManagement {
    void loadVMStatus();
    void keepPolling(boolean b);

    void onVMSelection(String vmKey);
}