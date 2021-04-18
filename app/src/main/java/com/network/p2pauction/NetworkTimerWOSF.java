package com.network.p2pauction;

public class NetworkTimerWOSF {
    private double delay;

    public void startTimer() {
        delay = 0.0;
    }

    public void waitForAck() {
        return;
    }

    public void updateDelayOnAckReceived(String s) {
        if(s.contains("leaderboard") || s.contains("position"))
            delay += Math.random() * (0.0019 - 0.0013 + 1) + 0.0013;
        else
            delay += Math.random() * (0.0005 - 0.0003 + 1) + 0.0003;
    }
    public double getDelay() {
        return this.delay;
    }
}
