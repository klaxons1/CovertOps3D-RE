/**
 * Fixed-point math utilities for game engine calculations
 * Uses 16.16 fixed-point format (16 bits integer, 16 bits fractional)
 */
public final class MathUtils {
    // Lookup tables for optimized trigonometric and mathematical operations
    private static int[] sinTable;          // Precomputed sine values (0-360 degrees in fixed-point)
    private static int[] invSqrtTable;      // Precomputed inverse square root values for fast distance calculations

    /**
     * Initialize mathematical lookup tables for optimized calculations
     * Called once during engine initialization
     */
    public static void initializeMathTables() {
        // Initialize sine table with 1609 entries (approximately 0.22 degree resolution)
        sinTable = new int[1609];

        for(int angleIndex = 0; angleIndex < 1609; ++angleIndex) {
            // Convert index to angle in fixed-point and compute sine
            int angle = (angleIndex << 16) + 32768;
            sinTable[angleIndex] = normalizeAngle(fixedPointMultiply(angle, 102943) / 1609);
        }

        // Initialize inverse square root table with 1024 entries
        invSqrtTable = new int[1024];

        for(int tableIndex = 0; tableIndex < 1024; ++tableIndex) {
            // Compute values for fast inverse square root approximation
            int value = (tableIndex << 16) + 32768;  
            invSqrtTable[tableIndex] = fixedPointDivide(65536, normalizeAngle(fastAtan(value / 1024)));
        }
    }

    /**
     * Safe fixed-point division with overflow protection
     * @param dividend Number to be divided (16.16 fixed-point)
     * @param divisor Number to divide by (16.16 fixed-point)
     * @return Quotient in 16.16 fixed-point format
     */
    public static int fixedPointDivide(int dividend, int divisor) {
        int absDividend = dividend >= 0 ? dividend : -dividend;
        int absDivisor = divisor >= 0 ? divisor : -divisor;

        // Check for potential overflow
        if (absDividend >> 14 >= absDivisor) {
            return (dividend ^ divisor) < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        } else {
            // Perform 64-bit division to maintain precision
            return (int)(((long)dividend << 32) / (long)divisor >> 16);
        }
    }

    /**
     * Fixed-point multiplication (16.16 format)
     * @param a First operand in 16.16 fixed-point
     * @param b Second operand in 16.16 fixed-point  
     * @return Product in 16.16 fixed-point format
     */
    public static int fixedPointMultiply(int a, int b) {
        return (int)((long)a * (long)b >> 16);
    }

    /**
     * High-precision fixed-point division
     * Uses 64-bit arithmetic for better accuracy
     * @param dividend Number to be divided
     * @param divisor Number to divide by
     * @return Precise quotient in 16.16 fixed-point
     */
    public static int preciseDivide(int dividend, int divisor) {
        return (int)(((long)dividend << 32) / (long)divisor >> 16);
    }

    /**
     * Fast hypotenuse calculation using lookup table
     * Approximates sqrt(dx² + dy²) without expensive square root
     * @param dx X difference in fixed-point
     * @param dy Y difference in fixed-point
     * @return Approximate distance in fixed-point
     */
    public static int fastHypot(int dx, int dy) {
        dx = dx >= 0 ? dx : -dx;  // Absolute value
        dy = dy >= 0 ? dy : -dy;

        // Special case: equal components
        if (dx == dy) {
            return (int)((long)dx * 92682L >> 16);  // sqrt(2) ≈ 1.414 * dx
        } else {
            long temp;
            int tableValue;

            if (dx < dy) {
                if (dx == 0) {
                    return dy;  // Simple case: vertical line
                } else {
                    // Use lookup table for ratio dx/dy
                    temp = (long)dx << 32;
                    tableValue = invSqrtTable[(int)(temp / (long)dy >> 22) & 1023];
                    return (int)((long)dx * (long)tableValue >> 16);
                }
            } else if (dy == 0) {
                return dx;  // Simple case: horizontal line
            } else {
                // Use lookup table for ratio dy/dx  
                temp = (long)dy << 32;
                tableValue = invSqrtTable[(int)(temp / (long)dx >> 22) & 1023];
                return (int)((long)dy * (long)tableValue >> 16);
            }
        }
    }

    /**
     * More accurate hypotenuse calculation using trigonometric approach
     * @param dx X difference in fixed-point
     * @param dy Y difference in fixed-point  
     * @return Precise distance in fixed-point
     */
    public static int preciseHypot(int dx, int dy) {
        dx = dx >= 0 ? dx : -dx;
        dy = dy >= 0 ? dy : -dy;

        if (dx == dy) {
            return (int)((long)dx * 92682L >> 16);  // sqrt(2) approximation
        } else if (dx < dy) {
            return dx == 0 ? dy : preciseDivide(dx, fastSin(fastAtan(preciseDivide(dx, dy))));
        } else {
            return dy == 0 ? dx : preciseDivide(dy, fastSin(fastAtan(preciseDivide(dy, dx))));
        }
    }

    /**
     * Normalize angle to specific range and apply polynomial approximation
     * Used for sine/cosine calculations
     * @param angle Input angle in fixed-point degrees
     * @return Normalized angle value
     */
    private static int normalizeAngle(int angle) {
        byte sign = 1;

        // Handle different angle quadrants
        if (angle > 102943 && angle <= 205887) {      // 90-180 degrees
            angle = 205887 - angle;  // Mirror around 90 degrees
        } else {
            if (angle > 205887 && angle <= 308830) {  // 180-270 degrees  
                angle = angle - 205887;
                sign = -1;
            } else if (angle > 308830) {              // 270-360 degrees
                angle = 411774 - angle;
                sign = -1;
            }
        }

        // Polynomial approximation (likely Taylor series for sine/cosine)
        int angleSquared = fixedPointMultiply(angle, angle);
        int result = fixedPointMultiply(498, angleSquared);  // Coefficient 498
        result -= 10880;                                    // Constant term
        result = fixedPointMultiply(fixedPointMultiply(result, angleSquared) + 65536, angle);

        return sign * result;
    }

    /**
     * Fast arctangent approximation using rational function
     * @param value Input value (ratio) in fixed-point
     * @return Angle in fixed-point degrees (0-90 range)
     */
    public static int fastAtan(int value) {
        byte quadrant = 0;

        // Handle different quadrants by taking reciprocal
        if (value > 65536) {        // Value > 1.0
            value = preciseDivide(65536, value);  // Reciprocal
            quadrant = 1;
        } else if (value < -65536) { // Value < -1.0  
            value = preciseDivide(65536, value);  // Reciprocal
            quadrant = 2;
        }

        // Polynomial approximation for arctan
        int valueSquared = fixedPointMultiply(value, value);
        int result = fixedPointMultiply(1365, valueSquared);  // Coefficient 1365
        result -= 5579;                                      // Constant
        result = fixedPointMultiply(result, valueSquared);
        result += 11805;                                     // Constant  
        result = fixedPointMultiply(result, valueSquared);
        result -= 21646;                                     // Constant
        result = fixedPointMultiply(fixedPointMultiply(result, valueSquared) + '\ufff7', value);

        // Adjust result based on original quadrant
        switch(quadrant) {
            case 1:  // Original value > 1.0
                return 102943 - result;  // 90 degrees - atan(1/value)
            case 2:  // Original value < -1.0  
                return -102943 - result; // -90 degrees - atan(1/value)
            default: // Original value in [-1, 1]
                return result;
        }
    }

    /**
     * Fast sine calculation using lookup table
     * @param angle Input angle in fixed-point degrees (0-360)
     * @return Sine value in fixed-point format
     */
    public static int fastSin(int angle) {
        // Normalize angle to 0-360 degree range
        if ((angle %= 411775) < 0) {
            angle += 411775;
        }

        // Use symmetry of sine function to reduce table size
        if (angle > 102943 && angle <= 205887) {      // 90-180 degrees
            angle = 205887 - angle;  // sin(x) = sin(180-x)
        } else {
            if (angle > 205887 && angle <= 308830) {  // 180-270 degrees
                angle -= 205887;
                return -sinTable[angle >> 6];  // sin(x) = -sin(x-180)
            }

            if (angle > 308830) {                    // 270-360 degrees  
                angle = 411774 - angle;
                return -sinTable[angle >> 6];  // sin(x) = -sin(360-x)
            }
        }

        // Look up value from precomputed table (angle >> 6 for table indexing)
        return sinTable[angle >> 6];
    }

    /**
     * Fast cosine calculation using sine lookup with 90-degree phase shift
     * @param angle Input angle in fixed-point degrees (0-360)  
     * @return Cosine value in fixed-point format
     */
    public static int fastCos(int angle) {
        // cos(x) = sin(x + 90 degrees)
        if ((angle = (angle + 102943) % 411775) < 0) {
            angle += 411775;
        }

        // Use same symmetry reduction as sine
        if (angle > 102943 && angle <= 205887) {      // 90-180 degrees
            angle = 205887 - angle;
        } else {
            if (angle > 205887 && angle <= 308830) {  // 180-270 degrees
                angle -= 205887;
                return -sinTable[angle >> 6];
            }

            if (angle > 308830) {                    // 270-360 degrees
                angle = 411774 - angle;
                return -sinTable[angle >> 6];
            }
        }

        return sinTable[angle >> 6];
    }
}