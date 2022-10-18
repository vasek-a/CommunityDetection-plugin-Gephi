/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.utils.traverse;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author smitalm
 */
public class Traverse<E> {

    Collection<TraverseListener<E>> listeners = new HashSet<TraverseListener<E>>();
    Collection<TraversePairListener<E>> pairListeners = new HashSet<TraversePairListener<E>>();

    public void registerListener(TraverseListener<E> listener) {
	if (!listeners.contains(listener)) {
	    listeners.add(listener);
	}
    }

    public void registerPairListener(TraversePairListener<E> listener) {
	if (!pairListeners.contains(listener)) {
	    pairListeners.add(listener);
	}
    }

    public void traverseArray(E[] elements, boolean pairOrderImportant) {
	int i = 0;
	int j;
	boolean simpleTraverse = pairListeners.isEmpty();
	for (E e1 : elements) {
	    // dispatch node visited event to all node listeners
	    for (TraverseListener<E> listener : listeners) {
		try {
		    listener.onElementVisited(e1);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	    if (simpleTraverse) {
		continue;
	    }
	    j = 0;
	    for (E e2 : elements) {
		if (j++ <= i) {
		    if (!pairOrderImportant) {
			continue;
		    }
		}
		// dispatch node pair visited event to all node pair listeners
		for (TraversePairListener<E> listener : pairListeners) {
		    try {
			listener.onElementPairVisited(e1, e2);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }
	    i++;
	}
    }

    public void traverse(Iterable<E> elements, boolean pairOrderImportant) {
	int i = 0;
	int j;
	boolean simpleTraverse = pairListeners.isEmpty();
	for (E e1 : elements) {
	    System.out.println("traversing " + i);
	    // dispatch node visited event to all node listeners
	    for (TraverseListener listener : listeners) {
		try {
		    listener.onElementVisited(e1);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	    if (simpleTraverse) {
		continue;
	    }
	    j = 0;
	    for (E e2 : elements) {
		System.out.println("aaaaaaaaaaaaaaaaaaatraversing " + i);
		if (j++ <= i) {
		    if (!pairOrderImportant) {
			continue;
		    }
		}
		// dispatch node pair visited event to all node pair listeners
		for (TraversePairListener listener : pairListeners) {
		    try {
			listener.onElementPairVisited(e1, e2);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    }
	    i++;
	}
    }
}
