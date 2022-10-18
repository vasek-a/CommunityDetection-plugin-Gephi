/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.smitala.gephi.communitydetection.utils.traverse;

/**
 *
 * @author smitalm
 */
public interface TraversePairListener<E> {
    public void onElementPairVisited(E e1, E e2);
}
