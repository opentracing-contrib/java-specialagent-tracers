package io.opentracing.contrib.specialagent.lightstep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.lightstep.tracer.shared.Options;

public final class TracerParameters {
  private TracerParameters() {}

  private final static Logger logger = Logger.getLogger(TracerParameters.class.getName());

  final static String DEFAULT_CONFIGURATION_FILE_PATH = "tracer.properties";
  final static String CONFIGURATION_FILE_KEY = "ls.configurationFile";

  final static String HTTP = "http";
  final static String HTTPS = "https";

  // TODO: add metaEventLogging, propagator, scopeManager and tags parameters.
  public final static String ACCESS_TOKEN = "ls.accessToken";
  public final static String CLOCK_SKEW_CORRECTION = "ls.clockSkewCorrection";
  public final static String COMPONENT_NAME = "ls.componentName";
  public final static String COLLECTOR_HOST = "ls.collectorHost";
  public final static String COLLECTOR_PORT = "ls.collectorPort";
  public final static String COLLECTOR_PROTOCOL = "ls.collectorProtocol";
  public final static String DEADLINE_MILLIS = "ls.deadlineMillis";
  public final static String DISABLE_REPORTING_LOOP = "ls.disableReportingLoop";
  public final static String MAX_BUFFERED_SPANS = "ls.maxBufferedSpans";
  public final static String MAX_REPORTING_INTERVAL_MILLIS = "ls.maxReportingIntervalMillis";
  public final static String RESET_CLIENT = "ls.resetClient";
  public final static String VERBOSITY = "ls.verbosity";

  public final static String [] ALL = {
    ACCESS_TOKEN,
    CLOCK_SKEW_CORRECTION,
    COMPONENT_NAME,
    COLLECTOR_HOST,
    COLLECTOR_PORT,
    COLLECTOR_PROTOCOL,
    DEADLINE_MILLIS,
    DISABLE_REPORTING_LOOP,
    MAX_BUFFERED_SPANS,
    MAX_REPORTING_INTERVAL_MILLIS,
    RESET_CLIENT,
    VERBOSITY
  };

  // NOTE: we could probably make this prettier
  // if we could use Java 8 Lambdas ;)
  public static Options.OptionsBuilder getOptionsFromParameters() {
    Map<String, String> params = getParameters();
    if (!params.containsKey(ACCESS_TOKEN))
      return null;

    Options.OptionsBuilder opts = new Options.OptionsBuilder()
      .withAccessToken(params.get(ACCESS_TOKEN));

    if (params.containsKey(CLOCK_SKEW_CORRECTION))
      opts.withClockSkewCorrection(toBoolean(params.get(CLOCK_SKEW_CORRECTION)));

    if (params.containsKey(COMPONENT_NAME))
      opts.withComponentName(params.get(COMPONENT_NAME));

    if (params.containsKey(COLLECTOR_HOST)) {
      String value = params.get(COLLECTOR_HOST);
      if (validateNonEmptyString(value))
        opts.withCollectorHost(value);
    }

    if (params.containsKey(COLLECTOR_PROTOCOL)) {
      String value = params.get(COLLECTOR_PROTOCOL);
      if (validateProtocol(value))
        opts.withCollectorProtocol(value);
    }

    if (params.containsKey(COLLECTOR_PORT)) {
      Integer value = toInteger(params.get(COLLECTOR_PORT));
      if (value != null && value > 0)
        opts.withCollectorPort(value);
    }

    if (params.containsKey(DEADLINE_MILLIS)) {
      Long value = toLong(params.get(DEADLINE_MILLIS));
      if (value != null)
        opts.withDeadlineMillis(value);
    }

    if (params.containsKey(DISABLE_REPORTING_LOOP))
      opts.withDisableReportingLoop(toBoolean(params.get(DISABLE_REPORTING_LOOP)));

    if (params.containsKey(MAX_BUFFERED_SPANS)) {
      Integer value = toInteger(params.get(MAX_BUFFERED_SPANS));
      if (value != null)
        opts.withMaxBufferedSpans(value);
    }

    if (params.containsKey(MAX_REPORTING_INTERVAL_MILLIS)) {
      Integer value = toInteger(params.get(MAX_REPORTING_INTERVAL_MILLIS));
      if (value != null)
        opts.withMaxReportingIntervalMillis(value);
    }

    if (params.containsKey(RESET_CLIENT))
      opts.withResetClient(toBoolean(params.get(RESET_CLIENT)));

    if (params.containsKey(VERBOSITY)) {
      Integer value = toInteger(params.get(VERBOSITY));
      if (value != null)
        opts.withVerbosity(value);
    }

    return opts;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, String> getParameters() {
    String filePath = System.getProperty(CONFIGURATION_FILE_KEY);
    if (filePath == null)
      filePath = DEFAULT_CONFIGURATION_FILE_PATH;

    Properties props = loadPropertiesFile(filePath);
    loadSystemProperties(props);

    for (String propName: props.stringPropertyNames())
      logger.log(Level.INFO, "Retrieved Tracer parameter " + propName + "=" + props.getProperty(propName));

    // A Properties object is expected to only contain String keys/values.
    return (Map)props;
  }

  static Properties loadPropertiesFile(String path) {
    File file = new File(path);
    if (!file.isFile()) {
      return new Properties(); 
    }

    Properties props = new Properties();

    try (FileInputStream stream = new FileInputStream(file)) {
      props.load(stream);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to read the Tracer configuration file '" + path + "'");
      logger.log(Level.WARNING, e.toString());
    }

    logger.log(Level.INFO, "Successfully loaded Tracer configuration file " + path);
    return props;
  }

  static void loadSystemProperties(Properties props) {
    for (String paramName: ALL) {
      String paramValue = System.getProperty(paramName);
      if (paramValue != null)
        props.setProperty(paramName, paramValue);
    }
  }

  static Integer toInteger(String value) {
    Integer integer = null;
    try {
      integer = Integer.valueOf(value);
    } catch (NumberFormatException e) {
      logger.log(Level.WARNING, "Failed to convert Tracer parameter value '" + value + "' to int");
    }

    return integer;
  }

  private static Long toLong(String value) {
    Long l = null;
    try {
      l = Long.valueOf(value);
    } catch (NumberFormatException e) {
      logger.log(Level.WARNING, "Failed to convert Tracer parameter value '" + value + "' to long");
    }

    return l;
  }

  private static Boolean toBoolean(String value) {
    return Boolean.valueOf(value);
  }

  private static boolean validateProtocol(String value) {
    if (!HTTPS.equals(value) && !HTTP.equals(value)) {
      logger.log(Level.WARNING, "Failed to validate protocol value '" + value + "'");
      return false;
    }

    return true;
  }

  private static boolean validateNonEmptyString(String value) {
    if (value == null || value.trim().length() == 0) {
      logger.log(Level.WARNING, "Failed to validate Tracer parameter as non-empty String");
      return false;
    }

    return true;
  }
}