package convex.core.data.type;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import convex.core.data.ACell;
import convex.core.data.prim.CVMByte;
import convex.core.data.prim.CVMDouble;
import convex.core.data.prim.CVMLong;
import convex.core.lang.RT;
import convex.test.Samples;
import convex.test.Samples.ValueArgumentsProvider;

public class TypesTest {


	
	@Test
	public void testNil() {
		AType t=Types.NIL;
		assertTrue(t.check(null));
		assertFalse(t.check(CVMLong.ONE));
	}
	
	@Test
	public void testLong() {
		AType t=Types.LONG;
		assertFalse(t.check(null));
		assertTrue(t.check(CVMLong.ONE));
		assertFalse(t.check(CVMDouble.ONE));
	}
	
	@Test
	public void testAny() {
		AType t=Types.ANY;
		assertTrue(t.check(null));
		assertTrue(t.check(CVMLong.ONE));
		assertTrue(t.check(CVMDouble.ONE));
	}
	
	@Test
	public void testCollection() {
		AType t=Types.COLLECTION;
		assertFalse(t.check(null));
		assertFalse(t.check(CVMLong.ONE));
		assertFalse(t.check(CVMDouble.ONE));
		assertTrue(t.check(Samples.LONG_SET_10));
		assertTrue(t.check(Samples.INT_VECTOR_300));
		assertTrue(t.check(Samples.INT_LIST_10));
	}
	
	@Test
	public void testVector() {
		AType t=Types.VECTOR;
		assertFalse(t.check(null));
		assertFalse(t.check(CVMLong.ONE));
		assertFalse(t.check(CVMDouble.ONE));
		assertFalse(t.check(Samples.LONG_SET_10));
		assertTrue(t.check(Samples.INT_VECTOR_300));
		assertFalse(t.check(Samples.INT_LIST_10));
	}
	
	@Test
	public void testSet() {
		AType t=Types.SET;
		assertFalse(t.check(null));
		assertFalse(t.check(CVMLong.ONE));
		assertTrue(t.check(Samples.LONG_SET_100));
		assertFalse(t.check(Samples.INT_VECTOR_300));
	}
	
	@Test
	public void testList() {
		AType t=Types.LIST;
		assertFalse(t.check(null));
		assertFalse(t.check(CVMDouble.ONE));
		assertFalse(t.check(Samples.INT_VECTOR_300));
		assertTrue(t.check(Samples.INT_LIST_10));
	}
	
	@Test
	public void testNumber() {
		AType t=Types.NUMBER;
		assertFalse(t.check(null));
		assertTrue(t.check(CVMLong.ONE));
		assertTrue(t.check(CVMByte.ONE));
		assertTrue(t.check(CVMDouble.ONE));
	}
	
	@ParameterizedTest
	@ArgumentsSource(ValueArgumentsProvider.class)
	public void testSampleValues(ACell a) {
		AType t=RT.getType(a);
		assertTrue(t.check(a));
		
	}
}
