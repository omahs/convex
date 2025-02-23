package convex.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import convex.core.data.Refs.RefTreeStats;
import convex.core.data.prim.CVMLong;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;
import convex.core.exceptions.MissingDataException;
import convex.core.lang.RT;
import convex.core.lang.Symbols;
import convex.test.Samples;

public class RefTest {
	@Test
	public void testMissingData() {
		// create a Ref using just a bad hash
		Ref<?> ref = Ref.forHash(Samples.BAD_HASH);

		// equals comparison should work
		assertEquals(ref, Ref.forHash(Samples.BAD_HASH));

		// gneric properties of missing Ref
		assertEquals(Samples.BAD_HASH, ref.getHash());
		assertEquals(Ref.UNKNOWN, ref.getStatus()); // shouldn't know anything about this Ref yet
		assertFalse(ref.isDirect());

		// we expect a failure here
		assertThrows(MissingDataException.class, () -> ref.getValue());
	}

	@Test
	public void testRefSet() {
		// 10 element refs
		assertEquals(11, Refs.accumulateRefSet(Samples.INT_VECTOR_10).size());
		assertEquals(11, Refs.totalRefCount(Samples.INT_VECTOR_10));

		// 256 element refs, 16 tree branches
		assertEquals(273, Refs.accumulateRefSet(Samples.INT_VECTOR_256).size());
		assertEquals(273, Refs.totalRefCount(Samples.INT_VECTOR_256));

		// 11 = 10 element refs plus one for enclosing ref
		assertEquals(11, Refs.accumulateRefSet(Samples.INT_VECTOR_10.getRef()).size());
	}

	@Test
	public void testShallowPersist() {
		Blob bb = Blob.createRandom(new Random(), 100); // unique blob but embedded
		assertTrue(bb.isEmbedded());
		
		AVector<ACell> v = Vectors.of(bb,bb,bb,bb); // vector containing big blob four times. Shouldn't be embedded.
		assertFalse(v.isEmbedded());
		assertEquals(5,Refs.totalRefCount(v));
		assertEquals(2,Refs.uniqueRefCount(v));
		
		Hash bh = bb.getHash();
		Hash vh = v.getHash();
		
		// Big vector vv containing two non-embedded copies of v
		AVector<ACell> vv=Vectors.of(v,v);
		assertEquals(11,Refs.totalRefCount(vv));
		assertEquals(3,Refs.uniqueRefCount(vv));
		assertTrue(vv.isEmbedded()); // true because just 2 child Refs
		assertFalse(vv.isCompletelyEncoded()); // false because has non-embedded children
		
		// Shallow persist vv
		Ref<AVector<ACell>> vvr=vv.getRef();
		vvr=vvr.persistShallow();
		assertEquals(Ref.STORED, vvr.getStatus());

		// non-embedded child v shouldn't yet be in store
		assertThrows(MissingDataException.class, () -> Ref.forHash(vh).getValue());
		
		// Shallow persist v
		Ref<AVector<ACell>> vr=v.getRef();
		vr = vr.persistShallow();
		assertEquals(Ref.STORED, vr.getStatus());

		
		// should be able to get v back from store now
		assertEquals(v, Ref.forHash(vh).getValue());
		
		// Now do full persistence of vv
		vvr=ACell.createPersisted(vv);
		assertEquals(Ref.PERSISTED, vvr.getStatus());
		
		// Persistence should extend to child v
		vr=Ref.forHash(vh);
		assertEquals(v,vr.getValue());
		assertEquals(Ref.PERSISTED, vr.getStatus());	
		
		// Now try announcing vv
		vv.announce();
		vvr=vv.getRef();
		assertEquals(Ref.ANNOUNCED, vvr.getStatus());
		assertEquals(Ref.ANNOUNCED, vvr.getValue().getRef(0).getStatus());
		
		// Announce should extend to child v
		vr=Ref.forHash(vh);
		assertEquals(v,vr.getValue());
		assertEquals(Ref.ANNOUNCED, vr.getStatus());	
		
		// child blob still shouldn't be in store after everything else
		assertThrows(MissingDataException.class, () -> Ref.forHash(bh).getValue());
	}
	
	@Test 
	public void testNonStored() throws BadFormatException {
		Blob r=Blob.createRandom(new Random(), 2*Blob.CHUNK_LENGTH+100); // 2 chunks + an embedded Blob of length 100
		assertEquals(4,Refs.totalRefCount(r));
		assertEquals(4,Refs.uniqueRefCount(r));
		Blob enc=r.getEncoding();
		
		// Should be able to read incomplete encoding
		ABlob b=Format.read(enc);
		
		assertEquals(enc,b.getEncoding());
		assertEquals(b,r); // Shouldn't hit store!
		
		assertThrows(MissingDataException.class,()->b.get(0));
		assertThrows(MissingDataException.class,()->b.get(2*Blob.CHUNK_LENGTH-1));
		
		assertEquals(b.get(8192),r.get(2*Blob.CHUNK_LENGTH)); // should be embedded so OK
	}

	@Test
	public void testEmbedded() {
		assertTrue(Ref.get(RT.cvm(1L)).isEmbedded()); // a primitive
		assertTrue(Ref.NULL_VALUE.isEmbedded()); // singleton null ref
		assertTrue(List.EMPTY_REF.isEmbedded()); // singleton null ref
		assertFalse(Blob.create(new byte[Format.MAX_EMBEDDED_LENGTH]).getRef().isEmbedded()); // too big to embed
		assertTrue(Samples.LONG_MAP_10.getRef().isEmbedded()); // a ref container
		assertTrue(Vectors.of(Samples.NON_EMBEDDED_BLOB).isEmbedded()); // an embeddable vector with non-embedded child
	}

	@Test
	public void testPersistEmbeddedNull() throws InvalidDataException {
		Ref<ACell> nr = Ref.get(null);
		assertSame(Ref.NULL_VALUE, nr);
		assertSame(nr, nr.persist());
		nr.validate();
		assertTrue(nr.isEmbedded());
	}

	@Test
	public void testPersistEmbeddedLong() {
		ACell val=RT.cvm(10001L);
		Ref<ACell> nr = Ref.get(val);
		Ref<ACell> nrp = nr.persist();
		
		assertSame(nr.getValue(), nrp.getValue());
		assertTrue(nr.isEmbedded());
		
		RefTreeStats rs=Refs.getRefTreeStats(nr);
		assertEquals(1,rs.embedded);
		assertEquals(0,rs.persisted);// original ref
		// assertEquals(1,rs.persisted); TODO: why this fail?
	}
	
	@Test
	public void testPersistNestedBlob() {
		ABlob bigBlob=Blobs.createRandom(17*Blob.CHUNK_LENGTH); // 16 full chunks plus one extra (3 levels)
		RefTreeStats rs=Refs.getRefTreeStats(bigBlob.getRef());
		assertEquals(19,rs.total);
		assertEquals(0,rs.persisted);
		assertEquals(1,rs.embedded); // top level only is embedded with 2 children
		Ref<ABlob> rb=ACell.createPersisted(bigBlob);
		
		RefTreeStats rs2=Refs.getRefTreeStats(rb);
		assertEquals(19,rs2.total);
		assertEquals(19,rs2.persisted);
		assertEquals(1,rs2.embedded);
	}
	
	@Test
	public void testGoodData() {
		AVector<ASymbolic> value = Vectors.of(Keywords.FOO, Symbols.FOO);
		// a good ref
		Ref<?> orig = value.getRef();
		assertEquals(Ref.UNKNOWN, orig.getStatus());
		assertFalse(orig.isPersisted());
		orig = orig.persist();
		assertTrue(orig.isPersisted());

		// a ref using the same hash
		if (!(value.isEmbedded())) {
			Ref<?> ref = Ref.forHash(orig.getHash());
			assertEquals(orig, ref);
			assertEquals(value, ref.getValue());
		}
	}

	@Test
	public void testCompare() {
		assertEquals(0, Ref.get(RT.cvm(1L)).compareTo(ACell.createPersisted(RT.cvm(1L))));
		assertEquals(1, Ref.get(RT.cvm(1L)).compareTo(
				Ref.forHash(Hash.fromHex("0000000000000000000000000000000000000000000000000000000000000000"))));
		assertEquals(-1, Ref.get(RT.cvm(1L)).compareTo(
				Ref.forHash(Hash.fromHex("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"))));
	}
	
	@Test
	public void testVectorRefCounts() {
		AVector<CVMLong> v=Vectors.of(1,2,3);
		assertEquals(3,v.getRefCount());
		
		AVector<CVMLong> zv=Vectors.repeat(CVMLong.create(0), 16);
		assertEquals(16,zv.getRefCount());
		
		// 3 tail elements after prefix ref
		AVector<CVMLong> zvv=zv.concat(v);
		assertEquals(4,zvv.getRefCount());

	}
	
	@Test
	public void testToString() {
		AVector<CVMLong> v=Vectors.of(1,2,3,4);
		Ref<AVector<CVMLong>> ref=v.getRef();
		assertNotNull(ref.toString());
	}
	
	@Test 
	public void testMissing() {
		Hash bad=Hash.fromHex("0000000000000000000000000000000000000000000000000000000000000000");
		Ref<?> ref=Ref.forHash(bad);
		assertTrue(ref.isMissing());
	}

	@Test
	public void testDiabolicalDeep() {
		// Main purpose is to test we don't hit stack overflows
		
		Ref<ACell> a = Samples.DIABOLICAL_MAP_2_10000.getRef();

		Set<Ref<?>> refs=Refs.accumulateRefSet(a);
		assertEquals(10003,refs.size()); // 10000 levels plus two keys and top level
		
		assertTrue(a.isEmbedded());
		
		// TODO: fix this stack overflow
		// assertEquals(Long.MAX_VALUE,a.getMemorySize());
		
		// Too big most likely		
		//HashSet<Hash> hs=new HashSet<>();
		//a.findMissing(hs,100);
		//assertTrue(hs.isEmpty());
	}

	@Test
	public void testDiabolicalWide() {
		// Main purpose is to test we deduplicate correctly
		
		Ref<ACell> a = Samples.DIABOLICAL_MAP_30_30.getRef();
		// OK since we manage de-duplication
		Set<Ref<?>> set = Refs.accumulateRefSet(a);
		assertEquals(31 + 30 * 16, set.size()); // 16 refs at each level after de-duping
		assertFalse(a.isEmbedded());
		
		assertEquals(Long.MAX_VALUE,a.getMemorySize());
		
		// Too big most likely
		//HashSet<Hash> hs=new HashSet<>();
		//a.findMissing(hs,100);
		//assertTrue(hs.isEmpty());
	}
	
	@Test public void testAllRefsVisitor() {
		ACell a=Samples.INT_VECTOR_10;
		Ref<?> root=a.getRef();
		
		ArrayList<ACell> al=new ArrayList<>();
		Refs.visitAllRefs(root, r->al.add(r.getValue()));
		
		assertEquals(11,al.size());
		assertSame(a,al.get(0));
		assertSame(CVMLong.ZERO,al.get(1));
		// TODO: fix this?
		// assertSame(a.getRef(9),al.get(10).getRef());
		
		Refs.RefTreeStats rts=Refs.getRefTreeStats(root);
		assertEquals(11,rts.total);
		assertEquals(11,rts.embedded);
		assertEquals(0,rts.persisted);
	}

	@Test
	public void testNullRef() {
		Ref<?> nullRef = Ref.get(null);
		assertSame(Ref.NULL_VALUE,nullRef);
		assertNotNull(nullRef);
		assertSame(nullRef.getHash(), Hash.NULL_HASH);
		assertTrue(nullRef.isEmbedded());
		assertFalse(nullRef.isMissing());
	}
	
	@Test 
	public void testReadRefSoft() throws BadFormatException {
		Hash h=Hash.wrap(Blobs.createRandom(32));
		RefSoft<?> ref=RefSoft.createForHash(h);
		ref.markEmbedded(false); // needed to ensure indirect encoding is assumed
		Blob b=ref.getEncoding();
		assertEquals(Ref.INDIRECT_ENCODING_LENGTH,b.count());
		assertEquals(ref,Format.readRef(b, 0));
	}
	
	@Test 
	public void testReadRefEmbedded() throws BadFormatException {
		CVMLong a=CVMLong.create(678575875);
		Ref<?> ref=a.getRef();
		assertTrue(a.isEmbedded());
		Blob b=ref.getEncoding();
		assertSame(a.getEncoding(),b); // ref encoding should just be the embedded value encoding
		assertEquals(ref,Format.readRef(b, 0));
	}
}
