package convex.core.util;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for tree handling functions
 */
public class Trees {

	/**
	 * Visits elements on a stack, popping one off from the end each time. 
	 * Visitor function MAY edit the stack. Will terminate when stack is empty.
	 * 
	 * IMPORTANT: O(1) usage of JVM stack, may be necessary to use a function like this when 
	 * visiting deeply nested trees in CVM code.
	 * 
	 * @param <T> Type of element to visit
	 * @param stack Stack of values to visit, must be a mutable List
	 * @param visitor Visitor function to call for each stack element.
	 */
	public static <T> void visitStack(List<T> stack, Consumer<T> visitor) {
		while(!stack.isEmpty()) {
			int pos=stack.size()-1;
			T r=stack.remove(pos);
			visitor.accept(r);
		}
	}
}
