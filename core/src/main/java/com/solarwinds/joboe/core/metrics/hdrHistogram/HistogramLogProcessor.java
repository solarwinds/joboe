/**
 * Written by Gil Tene of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Gil Tene
 */

package com.solarwinds.joboe.core.metrics.hdrHistogram;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link HistogramLogProcessor} will process an input log and
 * [can] generate two separate log files from a single histogram log file: a
 * sequential interval log file and a histogram percentile distribution log file.
 * <p>
 * The sequential interval log file logs a single stats summary line for
 * each reporting interval.
 * <p>
 * The histogram percentile distribution log file includes a detailed percentiles
 * and fine grained distribution of the entire log file range processed.
 * <p>
 * HistogramLogProcessor will process an input log file when provided with
 * the {@code -i <filename>} option. When no -i option is provided, standard input
 * will be processed.
 * <p>
 * When provided with an output file name {@code <logfile>} with the -o option
 * (e.g. "-o mylog"), HistogramLogProcessor will produce both output files
 * under the names {@code <logfile>} and {@code <logfile>.hgrm} (e.g. mylog and mylog.hgrm).
 * <p>
 * When not provided with an output file name, HistogramLogProcessor will
 * produce [only] the histogram percentile distribution log output to
 * standard output.
 * <p>
 * By default, HistogramLogProcessor only processes hlog file lines lines
 * with no tag specified [aka "default tagged" lines]. An optional -tag
 * parameter can be used to process lines of a [single] specific tag. The
 * -listtags option can be used to list all the tags found in the input file.
 * <p>
 * HistogramLogProcessor accepts optional -start and -end time range
 * parameters. When provided, the output will only reflect the portion
 * of the input log with timestamps that fall within the provided start
 * and end time range parameters.
 * <p>
 * HistogramLogProcessor also accepts and optional -csv parameter, which
 * will cause the output formatting (of both output file forms) to use
 * a CSV file format.
 */
public class HistogramLogProcessor extends Thread {

    public static final String versionString = "Histogram Log Processor version " + Version.version;

    private final HistogramLogProcessorConfiguration config;

    private final HistogramLogReader logReader;

    private static class HistogramLogProcessorConfiguration {
        public boolean verbose = false;
        public String outputFileName = null;
        public String inputFileName = null;
        public String tag = null;

        public double rangeStartTimeSec = 0.0;
        public double rangeEndTimeSec = Double.MAX_VALUE;

        public boolean logFormatCsv = false;
        public boolean listTags = false;
        public boolean allTags = false;

        public boolean movingWindow = false;
        public double movingWindowPercentileToReport = 99.0;
        public int movingWindowIntervalCount = 2;

        public int percentilesOutputTicksPerHalf = 5;
        public Double outputValueUnitRatio = 1000000.0; // default to msec units for output.

        public boolean error = false;
        public String errorMessage = "";

        public HistogramLogProcessorConfiguration(final String[] args) {
            boolean askedForHelp= false;
            try {
                for (int i = 0; i < args.length; ++i) {
                    if (args[i].equals("-csv")) {
                        logFormatCsv = true;
                    } else if (args[i].equals("-v")) {
                        verbose = true;
                    } else if (args[i].equals("-listtags")) {
                        listTags = true;
                    } else if (args[i].equals("-alltags")) {
                        allTags = true;
                    } else if (args[i].equals("-i")) {
                        inputFileName = args[++i];
                    } else if (args[i].equals("-tag")) {
                        tag = args[++i];
                    } else if (args[i].equals("-mpw") && (i + 1) < args.length) {
                        movingWindowPercentileToReport = Double.parseDouble(args[++i]);
                        movingWindow = true;
                    } else if (args[i].equals("-mpwi") && (i + 1) < args.length) {
                        movingWindowIntervalCount = Integer.parseInt(args[++i]);
                        movingWindow = true;
                    } else if (args[i].equals("-start") && (i + 1) < args.length) {
                        rangeStartTimeSec = Double.parseDouble(args[++i]);
                    } else if (args[i].equals("-end") && (i + 1) < args.length) {
                        rangeEndTimeSec = Double.parseDouble(args[++i]);
                    } else if (args[i].equals("-o") && (i + 1) < args.length) {
                        outputFileName = args[++i];
                    } else if (args[i].equals("-percentilesOutputTicksPerHalf") && (i + 1) < args.length) {
                        percentilesOutputTicksPerHalf = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("-outputValueUnitRatio") && (i + 1) < args.length) {
                        outputValueUnitRatio = Double.parseDouble(args[++i]);
                    } else if (args[i].equals("-h")) {
                        askedForHelp = true;
                        throw new Exception("Help: " + args[i]);
                    } else {
                        throw new Exception("Invalid args: " + args[i]);
                    }
                }

            } catch (Exception e) {
                error = true;
                errorMessage = "Error: " + versionString + " launched with the following args:\n";

                for (String arg : args) {
                    errorMessage += arg + " ";
                }
                if (!askedForHelp) {
                    errorMessage += "\nWhich was parsed as an error, indicated by the following exception:\n" + e;
                    System.err.println(errorMessage);
                }

                final String validArgs =
                        "\"[-csv] [-v] [-i inputFileName] [-o outputFileName] [-tag tag] " +
                                "[-start rangeStartTimeSec] [-end rangeEndTimeSec] " +
                                "[-outputValueUnitRatio r] [-listtags]";

                System.err.println("valid arguments = " + validArgs);

                System.err.println(
                        " [-h]                        help\n" +
                                " [-v]                        Provide verbose error output\n" +
                                " [-csv]                      Use CSV format for output log files\n" +
                                " [-i logFileName]            File name of Histogram Log to process (default is standard input)\n" +
                                " [-o outputFileName]         File name to output to (default is standard output)\n" +
                                " [-tag tag]                  The tag (default no tag) of the histogram lines to be processed\n" +
                                " [-start rangeStartTimeSec]  The start time for the range in the file, in seconds (default 0.0)\n" +
                                " [-end rangeEndTimeSec]      The end time for the range in the file, in seconds (default is infinite)\n" +
                                " [-outputValueUnitRatio r]   The scaling factor by which to divide histogram recorded values units\n" +
                                "                             in output. [default = 1000000.0 (1 msec in nsec)]\n" +
                                " [-listtags]                 list all tags found on histogram lines the input file."
                );
                System.exit(1);
            }
        }
    }

    private void outputTimeRange(final PrintStream log, final String title) {
        log.format(Locale.US, "#[%s between %.3f and", title, config.rangeStartTimeSec);
        if (config.rangeEndTimeSec < Double.MAX_VALUE) {
            log.format(" %.3f", config.rangeEndTimeSec);
        } else {
            log.format(" %s", "<Infinite>");
        }
        log.format(" seconds (relative to StartTime)]\n");
    }

    private void outputStartTime(final PrintStream log, final Double startTime) {
        log.format(Locale.US, "#[StartTime: %.3f (seconds since epoch), %s]\n",
                startTime, (new Date((long) (startTime * 1000))));
    }

    int lineNumber = 0;

    private EncodableHistogram getIntervalHistogram() {
        EncodableHistogram histogram = null;
        try {
            histogram = logReader.nextIntervalHistogram(config.rangeStartTimeSec, config.rangeEndTimeSec);
        } catch (RuntimeException ex) {
            System.err.println("Log file parsing error at line number " + lineNumber +
                    ": line appears to be malformed.");
            if (config.verbose) {
                throw ex;
            } else {
                System.exit(1);
            }
        }
        lineNumber++;
        return histogram;
    }

    private EncodableHistogram getIntervalHistogram(String tag) {
        EncodableHistogram histogram;
        if (tag == null) {
            do {
                histogram = getIntervalHistogram();
            } while ((histogram != null) && histogram.getTag() != null);
        } else {
            do {
                histogram = getIntervalHistogram();
            } while ((histogram != null) && !tag.equals(histogram.getTag()));
        }
        return histogram;
    }

    /**
     * Run the log processor with the currently provided arguments.
     */
    @Override
    public void run() {
        PrintStream timeIntervalLog = null;
        PrintStream movingWindowLog = null;
        PrintStream histogramPercentileLog = System.out;
        double firstStartTime = 0.0;
        boolean timeIntervalLogLegendWritten = false;
        boolean movingWindowLogLegendWritten = false;

        EncodableHistogram[] movingWindow = new EncodableHistogram[config.movingWindowIntervalCount];
        EncodableHistogram movingWindowSumHistogram = null;
        int movingWindowIndex = 0;

        if (config.listTags) {
            Set<String> tags = new TreeSet<String>();
            EncodableHistogram histogram;
            boolean nullTagFound = false;
            while ((histogram = getIntervalHistogram()) != null) {
                String tag = histogram.getTag();
                if (tag != null) {
                    tags.add(histogram.getTag());
                } else {
                    nullTagFound = true;
                }
            }
            System.out.println("Tags found in input file:");
            if (nullTagFound) {
                System.out.println("[NO TAG (default)]");
            }
            for (String tag : tags) {
                System.out.println(tag);
            }
            // listtags does nothing other than list tags:
            return;
        }

        final String logFormat;
        final String movingWindowLogFormat;
        if (config.logFormatCsv) {
            logFormat = "%.3f,%d,%.3f,%.3f,%.3f,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n";
            movingWindowLogFormat = "%.3f,%d,%.3f,%.3f\n";
        } else {
            logFormat = "%4.3f: I:%d ( %7.3f %7.3f %7.3f ) T:%d ( %7.3f %7.3f %7.3f %7.3f %7.3f %7.3f )\n";
            movingWindowLogFormat = "%4.3f: I:%d P:%7.3f M:%7.3f\n";
        }

        try {
            if (config.outputFileName != null) {
                try {
                    timeIntervalLog = new PrintStream(new FileOutputStream(config.outputFileName), false);
                    outputTimeRange(timeIntervalLog, "Interval percentile log");
                } catch (FileNotFoundException ex) {
                    System.err.println("Failed to open output file " + config.outputFileName);
                }
                String hgrmOutputFileName = config.outputFileName + ".hgrm";
                try {
                    histogramPercentileLog = new PrintStream(new FileOutputStream(hgrmOutputFileName), false);
                    outputTimeRange(histogramPercentileLog, "Overall percentile distribution");
                } catch (FileNotFoundException ex) {
                    System.err.println("Failed to open percentiles histogram output file " + hgrmOutputFileName);
                }
                if (config.movingWindow) {
                    String movingWindowOutputFileName = config.outputFileName + ".mpw";
                    try {
                        movingWindowLog = new PrintStream(new FileOutputStream(movingWindowOutputFileName), false);
                        outputTimeRange(movingWindowLog, "Moving window log for " +
                                config.movingWindowPercentileToReport + " percentile");
                    } catch (FileNotFoundException ex) {
                        System.err.println("Failed to open moving window output file " + movingWindowOutputFileName);
                    }
                }
            }

            EncodableHistogram intervalHistogram = getIntervalHistogram(config.tag);

            Histogram accumulatedRegularHistogram = null;
            DoubleHistogram accumulatedDoubleHistogram = null;

            if (intervalHistogram != null) {
                // Shape the accumulated histogram like the histograms in the log file (but clear their contents):
                if (intervalHistogram instanceof DoubleHistogram) {
                    accumulatedDoubleHistogram = ((DoubleHistogram) intervalHistogram).copy();
                    accumulatedDoubleHistogram.reset();
                    accumulatedDoubleHistogram.setAutoResize(true);
                    movingWindowSumHistogram = new DoubleHistogram(3);
                } else {
                    accumulatedRegularHistogram = ((Histogram) intervalHistogram).copy();
                    accumulatedRegularHistogram.reset();
                    accumulatedRegularHistogram.setAutoResize(true);
                    movingWindowSumHistogram = new Histogram(3);
                }
            }

            while (intervalHistogram != null) {

                // handle accumulated histogram:
                if (intervalHistogram instanceof DoubleHistogram) {
                    if (accumulatedDoubleHistogram == null) {
                        throw new IllegalStateException("Encountered a DoubleHistogram line in a log of Histograms.");
                    }
                    accumulatedDoubleHistogram.add((DoubleHistogram) intervalHistogram);
                } else {
                    if (accumulatedRegularHistogram == null) {
                        throw new IllegalStateException("Encountered a Histogram line in a log of DoubleHistograms.");
                    }
                    accumulatedRegularHistogram.add((Histogram) intervalHistogram);
                }

                // handle moving window:
                if (config.movingWindow) {
                    EncodableHistogram prevHist = movingWindow[movingWindowIndex];
                    movingWindow[movingWindowIndex] = intervalHistogram;
                    if (movingWindowSumHistogram instanceof DoubleHistogram) {
                        ((DoubleHistogram) movingWindowSumHistogram).add((DoubleHistogram) intervalHistogram);
                        if (prevHist != null) {
                            ((DoubleHistogram) movingWindowSumHistogram).subtract((DoubleHistogram) prevHist);
                        }
                    } else {
                        ((Histogram) movingWindowSumHistogram).add((Histogram) intervalHistogram);
                        if (prevHist != null) {
                            ((Histogram) movingWindowSumHistogram).subtract((Histogram) prevHist);
                        }
                    }
                    movingWindowIndex++;
                    movingWindowIndex %= config.movingWindowIntervalCount;
                }

                if ((firstStartTime == 0.0) && (logReader.getStartTimeSec() != 0.0)) {
                    firstStartTime = logReader.getStartTimeSec();

                    outputStartTime(histogramPercentileLog, firstStartTime);

                    if (timeIntervalLog != null) {
                        outputStartTime(timeIntervalLog, firstStartTime);
                    }
                }

                if (timeIntervalLog != null) {
                    if (!timeIntervalLogLegendWritten) {
                        timeIntervalLogLegendWritten = true;
                        if (config.logFormatCsv) {
                            timeIntervalLog.println("\"Timestamp\",\"Int_Count\",\"Int_50%\",\"Int_90%\",\"Int_Max\",\"Total_Count\"," +
                                    "\"Total_50%\",\"Total_90%\",\"Total_99%\",\"Total_99.9%\",\"Total_99.99%\",\"Total_Max\"");
                        } else {
                            timeIntervalLog.println("Time: IntervalPercentiles:count ( 50% 90% Max ) TotalPercentiles:count ( 50% 90% 99% 99.9% 99.99% Max )");
                        }
                    }

                    if (intervalHistogram instanceof DoubleHistogram) {
                        timeIntervalLog.format(Locale.US, logFormat,
                                ((intervalHistogram.getEndTimeStamp() / 1000.0) - logReader.getStartTimeSec()),
                                // values recorded during the last reporting interval
                                ((DoubleHistogram) intervalHistogram).getTotalCount(),
                                ((DoubleHistogram) intervalHistogram).getValueAtPercentile(50.0) / config.outputValueUnitRatio,
                                ((DoubleHistogram) intervalHistogram).getValueAtPercentile(90.0) / config.outputValueUnitRatio,
                                ((DoubleHistogram) intervalHistogram).getMaxValue() / config.outputValueUnitRatio,
                                // values recorded from the beginning until now
                                accumulatedDoubleHistogram.getTotalCount(),
                                accumulatedDoubleHistogram.getValueAtPercentile(50.0) / config.outputValueUnitRatio,
                                accumulatedDoubleHistogram.getValueAtPercentile(90.0) / config.outputValueUnitRatio,
                                accumulatedDoubleHistogram.getValueAtPercentile(99.0) / config.outputValueUnitRatio,
                                accumulatedDoubleHistogram.getValueAtPercentile(99.9) / config.outputValueUnitRatio,
                                accumulatedDoubleHistogram.getValueAtPercentile(99.99) / config.outputValueUnitRatio,
                                accumulatedDoubleHistogram.getMaxValue() / config.outputValueUnitRatio
                        );
                    } else {
                        timeIntervalLog.format(Locale.US, logFormat,
                                ((intervalHistogram.getEndTimeStamp() / 1000.0) - logReader.getStartTimeSec()),
                                // values recorded during the last reporting interval
                                ((Histogram) intervalHistogram).getTotalCount(),
                                ((Histogram) intervalHistogram).getValueAtPercentile(50.0) / config.outputValueUnitRatio,
                                ((Histogram) intervalHistogram).getValueAtPercentile(90.0) / config.outputValueUnitRatio,
                                ((Histogram) intervalHistogram).getMaxValue() / config.outputValueUnitRatio,
                                // values recorded from the beginning until now
                                accumulatedRegularHistogram.getTotalCount(),
                                accumulatedRegularHistogram.getValueAtPercentile(50.0) / config.outputValueUnitRatio,
                                accumulatedRegularHistogram.getValueAtPercentile(90.0) / config.outputValueUnitRatio,
                                accumulatedRegularHistogram.getValueAtPercentile(99.0) / config.outputValueUnitRatio,
                                accumulatedRegularHistogram.getValueAtPercentile(99.9) / config.outputValueUnitRatio,
                                accumulatedRegularHistogram.getValueAtPercentile(99.99) / config.outputValueUnitRatio,
                                accumulatedRegularHistogram.getMaxValue() / config.outputValueUnitRatio
                        );
                    }
                }

                if (movingWindowLog != null) {
                    if (!movingWindowLogLegendWritten) {
                        movingWindowLogLegendWritten = true;
                        if (config.logFormatCsv) {
                            movingWindowLog.println("\"Timestamp\",\"Window_Count\",\"" +
                                    config.movingWindowPercentileToReport +"%'ile\",\"Max\"");
                        } else {
                            movingWindowLog.println("Time: WindoCount " + config.movingWindowPercentileToReport + "%'ile Max");
                        }
                    }
                    if (intervalHistogram instanceof DoubleHistogram) {
                        movingWindowLog.format(Locale.US, movingWindowLogFormat,
                                ((intervalHistogram.getEndTimeStamp() / 1000.0) - logReader.getStartTimeSec()),
                                // values recorded during the last reporting interval
                                ((DoubleHistogram) movingWindowSumHistogram).getTotalCount(),
                                ((DoubleHistogram) movingWindowSumHistogram).getValueAtPercentile(config.movingWindowPercentileToReport) / config.outputValueUnitRatio,
                                ((DoubleHistogram) movingWindowSumHistogram).getMaxValue() / config.outputValueUnitRatio
                                );
                    } else {
                        movingWindowLog.format(Locale.US, movingWindowLogFormat,
                                ((intervalHistogram.getEndTimeStamp() / 1000.0) - logReader.getStartTimeSec()),
                                // values recorded during the last reporting interval
                                ((Histogram) movingWindowSumHistogram).getTotalCount(),
                                ((Histogram) movingWindowSumHistogram).getValueAtPercentile(config.movingWindowPercentileToReport) / config.outputValueUnitRatio,
                                ((Histogram) movingWindowSumHistogram).getMaxValue() / config.outputValueUnitRatio
                                );
                    }

                }

                intervalHistogram = getIntervalHistogram(config.tag);
            }

            if (accumulatedDoubleHistogram != null) {
                accumulatedDoubleHistogram.outputPercentileDistribution(histogramPercentileLog,
                        config.percentilesOutputTicksPerHalf, config.outputValueUnitRatio, config.logFormatCsv);
            } else {
                if (accumulatedRegularHistogram == null) {
                    // If there were no histograms in the log file, we still need an empty histogram for the
                    // one line output (shape/range doesn't matter because it is empty):
                    accumulatedRegularHistogram = new Histogram(1000000L, 2);
                }
                accumulatedRegularHistogram.outputPercentileDistribution(histogramPercentileLog,
                        config.percentilesOutputTicksPerHalf, config.outputValueUnitRatio, config.logFormatCsv);
            }
        } finally {
            if (timeIntervalLog != null) {
                timeIntervalLog.close();
            }

            if (movingWindowLog != null) {
                movingWindowLog.close();
            }

            if (config.outputFileName != null) {
                histogramPercentileLog.close();
            }
        }
    }

    /**
     * Construct a {@link HistogramLogProcessor} with the given arguments
     * (provided in command line style).
     * <pre>
     * [-h]                        help
     * [-csv]                      Use CSV format for output log files
     * [-i logFileName]            File name of Histogram Log to process (default is standard input)
     * [-o outputFileName]         File name to output to (default is standard output)
     *                             (will replace occurrences of %pid and %date with appropriate information)
     * [-start rangeStartTimeSec]  The start time for the range in the file, in seconds (default 0.0)
     * [-end rangeEndTimeSec]      The end time for the range in the file, in seconds (default is infinite)
     * [-outputValueUnitRatio r]   The scaling factor by which to divide histogram recorded values units
     *                             in output. [default = 1000000.0 (1 msec in nsec)]"
     * </pre>
     * @param args command line arguments
     * @throws FileNotFoundException if specified input file is not found
     */
    public HistogramLogProcessor(final String[] args) throws FileNotFoundException {
        this.setName("HistogramLogProcessor");
        config = new HistogramLogProcessorConfiguration(args);
        if (config.inputFileName != null) {
            logReader = new HistogramLogReader(config.inputFileName);
        } else {
            logReader = new HistogramLogReader(System.in);
        }
    }

    /**
     * main() method.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args)  {
        final HistogramLogProcessor processor;
        try {
            processor = new HistogramLogProcessor(args);
            processor.start();
        } catch (FileNotFoundException ex) {
            System.err.println("failed to open input file.");
        }
    }
}
