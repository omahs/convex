package convex.core.data.prim;

import java.math.BigInteger;

import convex.core.data.type.AType;
import convex.core.data.type.Types;

/**
 * Abstract base class for CVM Integer values
 */
public abstract class AInteger extends ANumeric {
 
	@Override
	public abstract boolean isCanonical();

	/**
	 * Increments this Integer
	 * @return Incremented value
	 */
	public abstract AInteger inc();
	
	
	/**
	 * Decrements this Integer
	 * @return Decremented value
	 */
	public abstract AInteger dec();
	
	public AType getType() {
		return Types.INTEGER;
	}
	
	/**
	 * Parse an integer value as a canonical value
	 * @param s String to parse
	 * @return AInteger instance
	 */
	public static AInteger parse(String s) {
		int n=s.length();
		if (n<19) return CVMLong.parse(s); // can't be a big integer
		if (n>20) return CVMBigInteger.parse(s); // can't be a long
				
		try {	
			return CVMLong.parse(s);
		} catch (Throwable t) {
			return CVMBigInteger.parse(s);
		}
	}

	/**
	 * Number of bytes in minimal representation of this Integer. Returns 0 if and only if the integer is zero.
	 * @return Number of bytes
	 */
	public abstract long byteLength();

	@Override
	public ANumeric add(ANumeric b) {
		if (b instanceof AInteger) return add((AInteger)b);
		return CVMDouble.create(doubleValue()+b.doubleValue());
	}
	
	/**
	 * Adds another integer to this integer
	 * @param a Integer value to add
	 * @return New integer
	 */
	public abstract AInteger add(AInteger a);
	
	@Override
	public ANumeric sub(ANumeric b) {
		if (b instanceof AInteger) return sub((AInteger)b);
		return CVMDouble.create(doubleValue()-b.doubleValue());
	}
	
	/**
	 * Subtracts another integer from this integer
	 * @param a Integer value to subtract
	 * @return New integer
	 */
	public abstract AInteger sub(AInteger a);

	/**
	 * Converts this integer to a Java BigInteger. WARNING: might be O(n)
	 * @return Java BigInteger
	 */
	public abstract BigInteger big();
	
	@Override
	public AInteger toInteger() {
		return this;
	}
}
