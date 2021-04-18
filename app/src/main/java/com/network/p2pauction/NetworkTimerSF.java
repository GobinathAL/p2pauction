package com.network.p2pauction;

public class NetworkTimerSF {
    private double delay;

    public void startTimer() {
        delay = 0.0;
    }

    public void waitForAck() {
        return;
    }

    public void updateDelayOnAckReceived(String s) {
        if(s.contains("initial"))
            delay += Math.random() * (0.0019 - 0.0013) + 0.0013;
        else
            delay += Math.random() * (0.0005 - 0.0003) + 0.0003;
    }
    public double getDelay() {
        return this.delay;
    }
}
