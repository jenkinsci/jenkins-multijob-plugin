package com.tikal.jenkins.plugins.multijob;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public final class Utils {

    private Utils() {
    }

    public static @Nonnull Properties parseProperties(final String properties) throws IOException {
        Properties props = new Properties();

        if (null != properties) {
            try {
                props.load(new StringReader(properties));
            } catch (NoSuchMethodError e) {
                props.load(new ByteArrayInputStream(properties.getBytes()));
            }
        }
        return props;
    }
}
