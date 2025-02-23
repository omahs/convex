package convex.core.data;

import java.nio.ByteBuffer;
import java.util.WeakHashMap;

import convex.core.data.type.AType;
import convex.core.data.type.Types;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;

/**
 * <p>Class representing a Symbol. Symbols are more commonly used in CVM code to refer to functions and values in the
 * execution environment.</p>
 * 
 * <p>Symbols are simply small immutable data Objects, and can be used freely in data structures. They can be used as map
 * keys, however for most normal circumstances Strings or Keywords are more appropriate as keys.
 * </p>
 * 
 * <p>
 * A Symbol comprises:
 * - A name
 * </p>
 *
 * <p>
 * "Becoming sufficiently familiar with something is a substitute for
 * understanding it." - John Conway
 * </p>
 */
public class Symbol extends ASymbolic implements Comparable<Symbol> {
	
	private Symbol(StringShort name) {
		super(name);
	}
	
	public AType getType() {
		return Types.SYMBOL;
	}
	
	protected static final WeakHashMap<AString,Symbol> cache=new WeakHashMap<>(100);

	/**
	 * Creates a Symbol with the given name
	 * @param name Symbol name
	 * @return Symbol instance, or null if the Symbol is invalid
	 */
	public static Symbol create(String name) {
		if (name==null) return null;
		return create(Strings.create(name));
	}

	/**
	 * Creates a Symbol with the given name. Must be an unqualified name.
	 * 
	 * @param name Name for Symbol
	 * @return Symbol instance, or null if the name is invalid for a Symbol.
	 */
	public static Symbol create(AString name) {
		if (!validateName(name)) return null;
		
		Symbol sym= new Symbol((StringShort)name);
		
		synchronized (cache) {
			// TODO: figure out if caching Symbols is a net win or not
			Symbol cached=cache.get(name);
			if (cached!=null) return cached;
			cache.put(name,sym);
		}

		return sym;
	}
	
	/**
	 * Creates a Symbol in an unsafe manner (possibly invalid name), used for testing
	 * @param rawName Raw Symbol name
	 * @return Possibly invalid Keyword
	 */
	public static Symbol unsafeCreate(String rawName) {
		return unsafeCreate((StringShort)Strings.create(rawName));
	}
	
	/**
	 * Creates a Symbol in an unsafe manner (possibly invalid name), used for testing
	 * @param rawName Raw Symbol name
	 * @return Possibly invalid Keyword
	 */
	public static Symbol unsafeCreate(StringShort rawName) {
		return new Symbol(rawName);
	}

	@Override
	public boolean equals(ACell o) {
		if (o==this) return true;
		if (o instanceof Symbol) return equals((Symbol) o);
		return false;
	}

	/**
	 * Tests if this Symbol is equal to another Symbol. Equality is defined by both namespace and name being equal.
	 * @param sym Symbol to compare with
	 * @return true if Symbols are equal, false otherwise
	 */
	public boolean equals(Symbol sym) {
		return sym.name.equals(name);
	}
	
	@Override
	public int compareTo(Symbol sym) {
		return name.compareTo(sym.name);
	}

	@Override
	public int encode(byte[] bs, int pos) {
		bs[pos++]=Tag.SYMBOL;
		return encodeRaw(bs,pos);
	}

	@Override
	public int encodeRaw(byte[] bs, int pos) {
		bs[pos++]=(byte)(name.count());
		return name.writeRawData(bs, pos);
	}

	/**
	 * Reads a Symbol from the given ByteBuffer, assuming tag already consumed
	 * 
	 * @param bb ByteBuffer source
	 * @return The Symbol read
	 * @throws BadFormatException If a Symbol could not be read correctly.
	 */
	public static Symbol read(ByteBuffer bb) throws BadFormatException {
		int len=0xff&bb.get();
		AString name=Format.readUTF8String(bb,len);
		Symbol sym = Symbol.create(name);
		if (sym == null) throw new BadFormatException("Can't read symbol");
		return sym;
	}
	
	public static ACell read(Blob blob, int offset) throws BadFormatException {
		int len=0xff&blob.byteAt(offset+1); // skip tag
		AString name=Format.readUTF8String(blob,offset+2,len);
		Symbol sym = Symbol.create(name);
		if (sym == null) throw new BadFormatException("Can't read symbol");
		sym.attachEncoding(blob.slice(offset, offset+2+len));
		return sym;
	}

	@Override
	public boolean isCanonical() {
		// Always canonical
		return true;
	}
	
	@Override
	public boolean print(BlobBuilder bb, long limit) {
		bb.append(getName());
		return bb.check(limit);
	}
	
	@Override
	public void validateCell() throws InvalidDataException {
		if (!validateName(name)) throw new InvalidDataException("Invalid Symbol name: " + name, this);
	}

	@Override
	public int getRefCount() {
		return 0;
	}

	@Override
	public byte getTag() {
		return Tag.SYMBOL;
	}

	@Override
	public ACell toCanonical() {
		return this;
	}

	@Override
	public AString toCVMString(long limit) {
		return name;
	}


}
