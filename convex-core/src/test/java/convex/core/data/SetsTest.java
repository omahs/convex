package convex.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import convex.core.data.prim.CVMDouble;
import convex.core.data.prim.CVMLong;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;
import convex.core.lang.RT;
import convex.test.Samples;

public class SetsTest {

	@Test
	public void testEmptySet() {
		ASet<ACell> e = Sets.empty();
		assertEquals(0, e.size());
		assertFalse(e.contains(null));
	}

	@Test
	public void testIncludeExclude() {
		ASet<ACell> s = Sets.empty();
		assertEquals("#{}", s.toString());
		s = s.include(RT.cvm(1L));
		assertEquals("#{1}", s.toString());
		s = s.include(RT.cvm(1L));
		assertEquals("#{1}", s.toString());
		s = s.include(RT.cvm(2L));
		assertEquals("#{1,2}", s.toString());
		s = s.exclude(RT.cvm(1L));
		assertEquals("#{2}", s.toString());
		s = s.exclude(RT.cvm(2L));
		assertTrue(s.isEmpty());
		assertSame(s, Sets.empty());
	}

	@Test
	public void testPrimitiveEquality() {
		// different primitive objects with same numeric value should not collide in set
		CVMDouble b=CVMDouble.create(1);
		ASet<ACell> s=Sets.of(1L).include(b);
		assertEquals(2L,s.count());
		
		assertEquals(Sets.of(b, 1L), s);
	}

	@Test
	public void testSetToArray() {
		assertEquals(3, Sets.of(1, 2, 3).toArray().length);
		assertEquals(0, Sets.empty().toArray().length);
	}

	@Test
	public void testContainsAll() {
		assertTrue(Sets.of(1, 2, 3).containsAll(Sets.of(2, 3)));
		assertFalse(Sets.of(1, 2).containsAll(Sets.of(2, 3, 4)));
	}
	
	@Test
	public void testSubsets() {
		ASet<CVMLong> EM=Sets.empty();
		assertTrue(EM.isSubset(EM));
		assertTrue(EM.isSubset(Samples.INT_SET_300));
		assertTrue(EM.isSubset(Samples.INT_SET_10));
		assertFalse(Samples.INT_SET_10.isSubset(EM));
		assertFalse(Samples.INT_SET_300.isSubset(EM));
		
		{
			ASet<CVMLong> s=Samples.createRandomSubset(Samples.INT_SET_300,0.5,1);
			assertTrue(s.isSubset(Samples.INT_SET_300));
		}
		{
			ASet<CVMLong> s=Samples.createRandomSubset(Samples.INT_SET_10,0.5,2);
			assertTrue(s.isSubset(Samples.INT_SET_10));
		}

		assertTrue(Samples.INT_SET_300.isSubset(Samples.INT_SET_300));
		assertTrue(Samples.INT_SET_10.isSubset(Samples.INT_SET_300));
		assertTrue(Samples.INT_SET_10.isSubset(Samples.INT_SET_10));
		assertFalse(Samples.INT_SET_300.isSubset(Samples.INT_SET_10));
	}

	@Test
	public void testMerging() {
		ASet<CVMLong> a = Sets.of(1, 2, 3);
		ASet<CVMLong> b = Sets.of(2, 4, 6);
		assertTrue(a.contains(RT.cvm(3L)));
		assertFalse(b.contains(RT.cvm(3L)));

		assertSame(Sets.empty(), a.disjAll(a));
		assertEquals(Sets.of(1, 2, 3, 4, 6), a.conjAll(b));
		assertEquals(Sets.of(1, 3), a.disjAll(b));
	}
	
	@Test 
	public void regressionRead() throws BadFormatException {
		ASet<CVMLong> v1=Sets.of(43);
		Blob b1 = Format.encodedBlob(v1);
		
		ASet<CVMLong> v2=Format.read(b1);
		Blob b2 = Format.encodedBlob(v2);
		
		assertEquals(v1, v2);
		assertEquals(b1,b2);
	}

	@Test
	public void regressionNils() throws InvalidDataException {
		AMap<ACell, ACell> m = Maps.of(null, null);
		assertEquals(1, m.size());
		assertTrue(m.containsKey(null));

		ASet<ACell> s = Sets.of(m);
		s.validate();
		s = s.include( m);
		s.validate();
	}
	
	

	@Test
	public void testMergingIdentity() {
		ASet<CVMLong> a = Sets.of(1L, 2L, 3L);
		assertSame(a, a.include(RT.cvm(2L)));
		assertSame(a, a.includeAll(Sets.of(1L, 3L)));
	}
	
	@Test
	public void testIntersection() {
		ASet<CVMLong> a = Sets.of(1, 2, 3);
		
		// (intersect a a) => a
		assertSame(a,a.intersectAll(a));
		
		// (intersect a #{}) => #{}
		assertSame(Sets.empty(),a.intersectAll(Sets.of(5,6)));

		// (intersect a b) => a if (subset? a b)
		assertEquals(a,a.intersectAll(Samples.INT_SET_10));
		assertEquals(a,a.intersectAll(Samples.INT_SET_300));
		
		// regular intersection
		assertEquals(Sets.of(2,3),a.intersectAll(Sets.of(2,3,4)));

		assertThrows(Throwable.class,()->a.intersectAll(null));
	}

	@Test
	public void testBigMerging() {
		ASet<CVMLong> s = Sets.create(Samples.INT_VECTOR_300);
		CollectionsTest.doSetTests(s);

		ASet<CVMLong> s2 = s.includeAll(Sets.of(1, 2, 3, 100));
		assertEquals(s, s2);
		assertSame(s, s2);

		ASet<CVMLong> s3 = s.disjAll(Samples.INT_VECTOR_300);
		assertSame(s3, Sets.empty());

		ASet<CVMLong> s4 = s.excludeAll(Sets.of(-1000));
		assertSame(s, s4);

		ASet<CVMLong> s5a = Sets.of(1, 3, 7, -1000);
		ASet<CVMLong> s5 = s5a.disjAll(s);
		assertEquals(Sets.of(-1000), s5);
	}
	
	@Test
	public void testIncrementalBuilding() {
		ASet<CVMLong> set=Sets.empty();
		for (int i=0; i<320; i++) {
			assertEquals(i,set.size());
			
			// extend set with one new element
			CVMLong v=CVMLong.create(i);
			ASet<CVMLong> newSet=set.conj(v);
			
			// new Set contains previous set
			assertTrue(newSet.containsAll(set));
			
			assertNotEquals(set,newSet);
			assertTrue(newSet.contains(v));
			assertFalse(set.contains(v));
			
			// removing element should get back to original set
			assertEquals(set,newSet.exclude(v));
			
			// removing original set should leave one element
			assertEquals(Sets.of(v),newSet.excludeAll(set));
			
			set=newSet;
		}
		
		doSetTests(set);
	}
	
	public static <T extends ACell> void doSetTests(ASet<T> a) {
		
		CollectionsTest.doSetTests(a);
	}
}
