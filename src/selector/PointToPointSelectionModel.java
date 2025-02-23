package selector;

import java.awt.Point;
import java.util.ListIterator;

/**
 * Models a selection tool that connects each added point with a straight line.
 */
public class PointToPointSelectionModel extends SelectionModel {

    public PointToPointSelectionModel(boolean notifyOnEdt) {
        super(notifyOnEdt);
    }

    public PointToPointSelectionModel(SelectionModel copy) {
        super(copy);
    }

    /**
     * Return a straight line segment from our last point to `p`.
     */
    @Override
    public PolyLine liveWire(Point p) {

        return new PolyLine(lastPoint(), p);
    }

    /**
     * Append a straight line segment to the current selection path connecting its end with `p`.
     */
    @Override
    protected void appendToSelection(Point p) {

        selection.add(new PolyLine(lastPoint(), p));
    }

    /**
     * Move the starting point of the segment of our selection with index `index` to `newPos`,
     * connecting to the end of that segment with a straight line and also connecting `newPos` to
     * the start of the previous segment (wrapping around) with a straight line (these straight
     * lines replace both previous segments).  Notify listeners that the "selection" property has
     * changed.
     */
    @Override
    public void movePoint(int index, Point newPos) {
        // Confirm that we have a closed selection and that `index` is valid
        if (state() != SelectionState.SELECTED) {
            throw new IllegalStateException("May not move point in state " + state());
        }
        if (index < 0 || index >= selection.size()) {
            throw new IllegalArgumentException("Invalid segment index " + index);
        }

        ListIterator<PolyLine> iterator = selection.listIterator();
        int curIndex = 0;
        while (iterator.hasNext()) {
            PolyLine segment = iterator.next();

            if (curIndex == index - 1 || (index == 0 && curIndex == selection.size() - 1)) {
                PolyLine preSegment = new PolyLine(segment.start(), newPos);
                selection.set(curIndex, preSegment);
            }
            if (curIndex == index) {
                PolyLine postSegment = new PolyLine(newPos, segment.end());
                selection.set(curIndex, postSegment);
            }
            curIndex += 1;
        }
        if (index == 0) {
            start = new Point(newPos);
        }

        propSupport.firePropertyChange("selection", null, selection());
    }
}
