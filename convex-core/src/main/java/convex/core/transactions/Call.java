package convex.core.transactions;

import java.nio.ByteBuffer;

import convex.core.Constants;
import convex.core.data.ACell;
import convex.core.data.AList;
import convex.core.data.AVector;
import convex.core.data.Address;
import convex.core.data.Format;
import convex.core.data.Keyword;
import convex.core.data.Keywords;
import convex.core.data.IRefFunction;
import convex.core.data.Ref;
import convex.core.data.Symbol;
import convex.core.data.Tag;
import convex.core.data.Vectors;
import convex.core.data.prim.CVMLong;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;
import convex.core.lang.Context;
import convex.core.lang.impl.RecordFormat;

/**
 * Transaction representing a Call to an Actor.
 * 
 * The signer of the transaction will be both the *origin* and *caller* for the Actor code.
 * 
 * This is the most efficient way to execute Actor code directly as a client, and is roughly equivalent to invoking
 * (call actor offer (function-name arg1 arg2 .....))
 */
public class Call extends ATransaction {

	protected final Address target;
	protected final long offer;
	protected final Symbol functionName;
	protected final AVector<ACell> args;

	private static final Keyword[] KEYS = new Keyword[] { Keywords.CALL, Keywords.OFFER, Keywords.ORIGIN, Keywords.SEQUENCE,
		                                           Keywords.TARGET };
	private static final RecordFormat FORMAT = RecordFormat.of(KEYS);

	protected Call(Address address, long sequence, Address target, long offer,Symbol functionName,AVector<ACell> args) {
		super(FORMAT,address,sequence);
		this.target=target;
		this.functionName=functionName;
		this.offer=offer;
		this.args=args;
	}
	
	public static Call create(Address address, long sequence, Address target, long offer,Symbol functionName,AVector<ACell> args) {
		return new Call(address,sequence,target,offer,functionName,args);
	}

	
	public static Call create(Address address, long sequence, Address target, Symbol functionName,AVector<ACell> args) {
		return create(address,sequence,target,0,functionName,args);
	}
	
	@Override
	public int encode(byte[] bs, int pos) {
		bs[pos++] = Tag.CALL;
		return encodeRaw(bs,pos);
	}

	@Override
	public int encodeRaw(byte[] bs, int pos) {
		pos = super.encodeRaw(bs,pos); // sequence
		pos = Format.write(bs,pos, target);
		pos=Format.writeVLCLong(bs,pos, offer);
		pos=Format.write(bs,pos, functionName);
		pos=Format.write(bs,pos, args);
		return pos;
	}
	
	public static ATransaction read(ByteBuffer bb) throws BadFormatException {
		Address address=Address.create(Format.readVLCLong(bb));
		long sequence = Format.readVLCLong(bb);
		Address target=Format.read(bb);
		long offer = Format.readVLCLong(bb);
		Symbol functionName=Format.read(bb);
		AVector<ACell> args = Format.read(bb);
		return create(address,sequence, target, offer, functionName,args);
	}

	@Override
	public int estimatedEncodingSize() {
		return 100;
	}

	@Override
	public <T extends ACell> Context<T> apply(Context<?> ctx) {
		return ctx.actorCall(target, offer, functionName, args.toCellArray());
	}

	@Override
	public Long getMaxJuice() {
		return Constants.MAX_TRANSACTION_JUICE;
	}

	@Override
	public void validateCell() throws InvalidDataException {
		target.validateCell();
	}
	
	@Override
	public int getRefCount() {
		return args.getRefCount();
	}
	
	@Override
	public <T extends ACell> Ref<T> getRef(int i) {
		return args.getRef(i);
	}

	@Override
	public Call updateRefs(IRefFunction func) {
		AVector<ACell> newArgs=args.updateRefs(func);
		if (args==newArgs) return this;
		return new Call(origin,sequence,target,offer,functionName,newArgs);
	}

	@Override
	public Call withSequence(long newSequence) {
		if (newSequence==this.sequence) return this;
		return create(origin,newSequence,target,offer,functionName,args);
	}
	
	@Override
	public Call withOrigin(Address newAddress) {
		if (newAddress==this.origin) return this;
		return create(newAddress,sequence,target,offer,functionName,args);
	}

	@Override
	public byte getTag() {
		return Tag.CALL;
	}

	@Override
	public ACell get(ACell key) {
		if (Keywords.CALL.equals(key)) return args.cons(functionName);
		if (Keywords.OFFER.equals(key)) return CVMLong.create(offer);
		if (Keywords.ORIGIN.equals(key)) return origin;
		if (Keywords.SEQUENCE.equals(key)) return CVMLong.create(sequence);
		if (Keywords.TARGET.equals(key)) return target;

		return null;
	}

	@Override
	public Call updateAll(ACell[] newVals) {
		@SuppressWarnings("unchecked")
		AList<ACell> call = (AList<ACell>)newVals[0];
		long offer = ((CVMLong)newVals[1]).longValue();
		Address origin = (Address)newVals[2];
		long sequence = ((CVMLong)newVals[3]).longValue();
		Address target = (Address)newVals[4];

		Symbol functionName = (Symbol)call.get(0);
		AVector<ACell> args = Vectors.create(call.next());

		return new Call(origin, sequence, target, offer, functionName, args);
	}
}
