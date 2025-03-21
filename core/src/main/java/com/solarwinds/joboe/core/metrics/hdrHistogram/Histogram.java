/**
 * Written by Gil Tene of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Gil Tene
 */

package com.solarwinds.joboe.core.metrics.hdrHistogram;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;

/**
 * <h3>A High Dynamic Range (HDR) Histogram</h3>
 * <p>
 * {@link Histogram} supports the recording and analyzing sampled data value counts across a configurable integer value
 * range with configurable value precision within the range. Value precision is expressed as the number of significant
 * digits in the value recording, and provides control over value quantization behavior across the value range and the
 * subsequent value resolution at any given level.
 * <p>
 * For example, a Histogram could be configured to track the counts of observed integer values between 0 and
 * 3,600,000,000 while maintaining a value precision of 3 significant digits across that range. Value quantization
 * within the range will thus be no larger than 1/1,000th (or 0.1%) of any value. This example Histogram could
 * be used to track and analyze the counts of observed response times ranging between 1 microsecond and 1 hour
 * in magnitude, while maintaining a value resolution of 1 microsecond up to 1 millisecond, a resolution of
 * 1 millisecond (or better) up to one second, and a resolution of 1 second (or better) up to 1,000 seconds. At its
 * maximum tracked value (1 hour), it would still maintain a resolution of 3.6 seconds (or better).
 * <p>
 * Histogram tracks value counts in <b><code>long</code></b> fields. Smaller field types are available in the
 * {@link IntCountsHistogram} and {@link ShortCountsHistogram} implementations of
 * {@link AbstractHistogram}.
 * <p>
 * Auto-resizing: When constructed with no specified value range range (or when auto-resize is turned on with {@link
 * Histogram#setAutoResize}) a {@link Histogram} will auto-resize its dynamic range to include recorded values as
 * they are encountered. Note that recording calls that cause auto-resizing may take longer to execute, as resizing
 * incurs allocation and copying of internal data structures.
 * <p>
 * See package description for {@link com.solarwinds.metrics.hdrHistogram} for details.
 */

public class Histogram extends AbstractHistogram {
    long totalCount;
    long[] counts;
    int normalizingIndexOffset;

    @Override
    long getCountAtIndex(final int index) {
        return counts[normalizeIndex(index, normalizingIndexOffset, countsArrayLength)];
    }

    @Override
    long getCountAtNormalizedIndex(final int index) {
        return counts[index];
    }

    @Override
    void incrementCountAtIndex(final int index) {
        counts[normalizeIndex(index, normalizingIndexOffset, countsArrayLength)]++;
    }

    @Override
    void addToCountAtIndex(final int index, final long value) {
        counts[normalizeIndex(index, normalizingIndexOffset, countsArrayLength)] += value;
    }

    @Override
    void setCountAtIndex(int index, long value) {
        counts[normalizeIndex(index, normalizingIndexOffset, countsArrayLength)] = value;
    }

    @Override
    void setCountAtNormalizedIndex(int index, long value) {
        counts[index] = value;
    }

    @Override
    int getNormalizingIndexOffset() {
        return normalizingIndexOffset;
    }

    @Override
    void setNormalizingIndexOffset(int normalizingIndexOffset) {
        this.normalizingIndexOffset = normalizingIndexOffset;
    }

    @Override
    void shiftNormalizingIndexByOffset(int offsetToAdd, boolean lowestHalfBucketPopulated) {
        nonConcurrentNormalizingIndexShift(offsetToAdd, lowestHalfBucketPopulated);
    }

    @Override
    void clearCounts() {
        Arrays.fill(counts, 0);
        totalCount = 0;
    }

    @Override
    public Histogram copy() {
        Histogram copy = new Histogram(this);
        copy.add(this);
        return copy;
    }

    @Override
    public Histogram copyCorrectedForCoordinatedOmission(final long expectedIntervalBetweenValueSamples) {
        Histogram copy = new Histogram(this);
        copy.addWhileCorrectingForCoordinatedOmission(this, expectedIntervalBetweenValueSamples);
        return copy;
    }

    @Override
    public long getTotalCount() {
        return totalCount;
    }

    @Override
    void setTotalCount(final long totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    void incrementTotalCount() {
        totalCount++;
    }

    @Override
    void addToTotalCount(final long value) {
        totalCount += value;
    }

    @Override
    int _getEstimatedFootprintInBytes() {
        return (512 + (8 * counts.length));
    }

    @Override
    void resize(long newHighestTrackableValue) {
        int oldNormalizedZeroIndex = normalizeIndex(0, normalizingIndexOffset, countsArrayLength);

        establishSize(newHighestTrackableValue);

        int countsDelta = countsArrayLength - counts.length;

        counts = Arrays.copyOf(counts, countsArrayLength);

        if (oldNormalizedZeroIndex != 0) {
            // We need to shift the stuff from the zero index and up to the end of the array:
            int newNormalizedZeroIndex = oldNormalizedZeroIndex + countsDelta;
            int lengthToCopy = (countsArrayLength - countsDelta) - oldNormalizedZeroIndex;
            System.arraycopy(counts, oldNormalizedZeroIndex, counts, newNormalizedZeroIndex, lengthToCopy);
            Arrays.fill(counts, oldNormalizedZeroIndex, newNormalizedZeroIndex, 0);
        }
    }

    /**
     * Construct an auto-resizing histogram with a lowest discernible value of 1 and an auto-adjusting
     * highestTrackableValue. Can auto-resize up to track values up to (Long.MAX_VALUE / 2).
     *
     * @param numberOfSignificantValueDigits Specifies the precision to use. This is the number of significant
     *                                       decimal digits to which the histogram will maintain value resolution
     *                                       and separation. Must be a non-negative integer between 0 and 5.
     */
    public Histogram(final int numberOfSignificantValueDigits) {
        this(1, 2, numberOfSignificantValueDigits);
        setAutoResize(true);
    }

    /**
     * Construct a Histogram given the Highest value to be tracked and a number of significant decimal digits. The
     * histogram will be constructed to implicitly track (distinguish from 0) values as low as 1.
     *
     * @param highestTrackableValue          The highest value to be tracked by the histogram. Must be a positive
     *                                       integer that is {@literal >=} 2.
     * @param numberOfSignificantValueDigits Specifies the precision to use. This is the number of significant
     *                                       decimal digits to which the histogram will maintain value resolution
     *                                       and separation. Must be a non-negative integer between 0 and 5.
     */
    public Histogram(final long highestTrackableValue, final int numberOfSignificantValueDigits) {
        this(1, highestTrackableValue, numberOfSignificantValueDigits);
    }

    /**
     * Construct a Histogram given the Lowest and Highest values to be tracked and a number of significant
     * decimal digits. Providing a lowestDiscernibleValue is useful is situations where the units used
     * for the histogram's values are much smaller that the minimal accuracy required. E.g. when tracking
     * time values stated in nanosecond units, where the minimal accuracy required is a microsecond, the
     * proper value for lowestDiscernibleValue would be 1000.
     *
     * @param lowestDiscernibleValue         The lowest value that can be discerned (distinguished from 0) by the
     *                                       histogram. Must be a positive integer that is {@literal >=} 1. May be
     *                                       internally rounded down to nearest power of 2.
     * @param highestTrackableValue          The highest value to be tracked by the histogram. Must be a positive
     *                                       integer that is {@literal >=} (2 * lowestDiscernibleValue).
     * @param numberOfSignificantValueDigits Specifies the precision to use. This is the number of significant
     *                                       decimal digits to which the histogram will maintain value resolution
     *                                       and separation. Must be a non-negative integer between 0 and 5.
     */
    public Histogram(final long lowestDiscernibleValue, final long highestTrackableValue,
                     final int numberOfSignificantValueDigits) {
        this(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits, true);
    }

    /**
     * Construct a histogram with the same range settings as a given source histogram,
     * duplicating the source's start/end timestamps (but NOT its contents)
     * @param source The source histogram to duplicate
     */
    public Histogram(final AbstractHistogram source) {
        this(source, true);
    }

    Histogram(final AbstractHistogram source, boolean allocateCountsArray) {
        super(source);
        if (allocateCountsArray) {
            counts = new long[countsArrayLength];
        }
        wordSizeInBytes = 8;
    }

    Histogram(final long lowestDiscernibleValue, final long highestTrackableValue,
              final int numberOfSignificantValueDigits, boolean allocateCountsArray) {
        super(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
        if (allocateCountsArray) {
            counts = new long[countsArrayLength];
        }
        wordSizeInBytes = 8;
    }

    /**
     * Construct a new histogram by decoding it from a ByteBuffer.
     * @param buffer The buffer to decode from
     * @param minBarForHighestTrackableValue Force highestTrackableValue to be set at least this high
     * @return The newly constructed histogram
     */
    public static Histogram decodeFromByteBuffer(final ByteBuffer buffer,
                                                 final long minBarForHighestTrackableValue) {
        return decodeFromByteBuffer(buffer, Histogram.class, minBarForHighestTrackableValue);
    }

    /**
     * Construct a new histogram by decoding it from a compressed form in a ByteBuffer.
     * @param buffer The buffer to decode from
     * @param minBarForHighestTrackableValue Force highestTrackableValue to be set at least this high
     * @return The newly constructed histogram
     * @throws DataFormatException on error parsing/decompressing the buffer
     */
    public static Histogram decodeFromCompressedByteBuffer(final ByteBuffer buffer,
                                                           final long minBarForHighestTrackableValue) throws DataFormatException {
        return decodeFromCompressedByteBuffer(buffer, Histogram.class, minBarForHighestTrackableValue);
    }

    private void readObject(final ObjectInputStream o)
            throws IOException, ClassNotFoundException {
        o.defaultReadObject();
    }

    @Override
    synchronized void fillCountsArrayFromBuffer(final ByteBuffer buffer, final int length) {
        buffer.asLongBuffer().get(counts, 0, length);
    }
}
