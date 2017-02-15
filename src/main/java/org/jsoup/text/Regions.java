package org.jsoup.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;


/**
 *  A list of {@link Region}s, with function such as remove() and
 *  wrap() that do the same as their {@link Region} equivalents.
 *
 *  <p>This is mostly a convenience class in order to operate on
 *  regions that straddle block elements.
 *
 *  <p>Wrapping a region can cause a Point within that Region to
 *  become invalid, and removing a region certainly does. To avoid
 *  unpleasantness, this class provides a safelyModifiable() Regions
 *  that merges partly overlapping regions and returns a result whose
 *  first Region is last in the document.
 */
public class Regions extends ArrayList<Region> {

    public Regions() {
        super();
    }


    /**
     *  Remove all these {@link Region}s from this Document.
     */
    public void remove() {
        for(Node n : parents()) {
            Node p = n.parentNode();
            n.remove();
        }
    }


    /**
     *  Return a set of Nodes spanning all of these {@link Region}s,
     *  and containing nothing else. This is not merely a list of
     *  {@link Region#parents()}, it substitutes shared parents for
     *  those siblings where possible. Because of that, the result may
     *  not be a set of siblings.
     */
    public Set<Node> parents() {
        Set<Node> result = new HashSet<Node>();
        for(Region r : this)
            result.addAll(r.parents());
        boolean progress = true;
        while(progress) {
            progress = false;
            Set<Node> parents = new HashSet<Node>();
            for(Node n : result)
                parents.add(n.parentNode());
            parents.remove(null);
            for(Node n : parents) {
                if(!progress && result.containsAll(n.childNodes())) {
                    result.removeAll(n.childNodes());
                    result.add(n);
                    progress = true;
                }
            }
        }
        return result;
    }

    /**
     *  Wrap each of these {@link Region}s in the supplied html. If
     *  two regions are adjacent, then two instances of the supplied
     *  HTML will be, too.
     */
    public void wrap(final String html) {
        for(Region r : this)
            r.wrap(html);
    }

    /**
     *  Split the Region into Regions such that each Region is entirely
     *  within a single Element.
     *
     *  <p>This is useful if you want to, say, elide the region: You
     *  can replace the first Region with an ellipsis and drop thee
     *  remainder outright.
     */

    public Regions splitByElements() {
        Regions result = new Regions();
        for(Region r : this)
            result.addAll(r.splitByElements());
        return result;
    }

    /**
     *  Split the Regions into Regions such that each Region is entirely
     *  within a single Element.
     *
     *  <p>This is useful if you want to, say, wrap the region in
     *  &lt;a href...&gt; or &lt;b&gt;.
     */
    public Regions splitByBlockElements() {
        Regions result = new Regions();
        for(Region r : this)
            result.addAll(r.splitByBlockElements());
        return result;
    }

    /**
     *  Get the first Region.
     *  @return The first Region, or <code>null</code> if contents is empty.
     */
    public Region first() {
        return isEmpty() ? null : get(0);
    }

    /**
     *  Get the last Region.
     *  @return The last Region, or <code>null</code> if contents is empty.
     */
    public Region last() {
        if(isEmpty())
            return null;
        return get(size()-1);
    }

    /**
     *  Get a safely modifiable version of these Regions.
     *
     *  <p>Modifying a Region sometimes invalidates later Points, so
     *  the only safe way to modify Regions to start at the end of the
     *  Document and work towards the front, carefully avoiding
     *  overlapping Regions.
     *
     *  <p>This function returns Regions ordered by descending start
     *  point, merged such that none overlap.
     */
    public Regions safelyModifiable() {
        ArrayList<Region> sorted = new ArrayList<Region>(this);
        Collections.sort(sorted, Region.byStart);
        Regions result = new Regions();
        int i = sorted.size();
        while(i > 0) {
            int last = --i;
            while(i > 0 &&
                  Point.byPosition.compare(sorted.get(i).getStart(),
                                           sorted.get(i-1).getEnd()) < 0)
                i--;
            if(last == i)
                result.add(sorted.get(i));
            else
                result.add(new Region(sorted.get(i).getStart(),
                                      Collections.max(subList(i, last+1),
                                                      Region.byEnd).getEnd()));
        }
        return result;
    }
}
