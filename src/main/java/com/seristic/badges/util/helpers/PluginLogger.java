package com.seristic.badges.util.helpers;

import java.util.logging.Logger;

public class PluginLogger {
    private static final Logger logger = Logger.getLogger("Badges");

    public static Logger getLogger() {
        return logger;
    }
}
