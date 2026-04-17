package com.advocacy.advocacy_media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogging {
    private static final Logger logger = LoggerFactory.getLogger(TestLogging.class);

    public static void main(String[] args) {
        logger.info("This is an info log");
        logger.warn("This is a warning log");
        logger.error("This is an error log");
    }
}
