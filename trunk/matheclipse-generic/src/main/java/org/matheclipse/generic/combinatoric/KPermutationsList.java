package org.matheclipse.generic.combinatoric;

import java.util.Iterator;
import java.util.List;

import org.matheclipse.generic.nested.NestedAlgorithms;

/**
 * Generate a list of permutations
 *
 * See <a href=" http://en.wikipedia.org/wiki/Permutation">Permutation</a>
 */
public class KPermutationsList<T, L extends List<T>> implements Iterator<L>, Iterable<L> {

	final private L fList;
	final private L fResultList;
	final private int fOffset;
	final private KPermutationsIterable fIterable;
	final private NestedAlgorithms<T, L> fCopier;

	public KPermutationsList(final L list, final int parts, L resultList, NestedAlgorithms<T, L> copier) {
		this(list, parts, resultList, copier, 0);
	}

	public KPermutationsList(final L list, final int parts, L resultList, NestedAlgorithms<T, L> copier, final int offset) {
		fIterable = new KPermutationsIterable(list, parts, offset);
		fList = list;
		fResultList = resultList;
		fCopier = copier;
		fOffset = offset;
	}

	/**
	 * Get the index array for the next permutation.
	 * 
	 * @return <code>null</code> if no further index array could be generated
	 */
	public L next() {
		int[] permutationsIndex = fIterable.next();
		if (permutationsIndex == null) {
			return null;
		}
		L temp = fCopier.clone(fResultList);
		for (int i = 0; i < permutationsIndex.length; i++) {
			temp.add(fList.get(permutationsIndex[i] + fOffset));
		}
		return temp;
	}

	public boolean hasNext() {
		return fIterable.hasNext();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public Iterator<L> iterator() {
		return this;
	}

}
