package graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class PathfindingSnapshot {

    /**
     * The ID of the starting vertex for the pathfinding operation that produced this snapshot.
     */
    private final int startId;

    /**
     * `distances[id]` is the weight of the shortest known path from the starting vertex to the
     * vertex with ID `id`, or -1 if no such path is currently known.
     */
    private final int[] distances;

    /**
     * `predecessors[id]` is the ID of the penultimate vertex along the shortest known path from the
     * starting vertex to the vertex with ID `id`, or -1 if `id==startId` or if no such path is
     * currently known.
     */
    private final int[] predecessors;

    /**
     * `settledIds[id]` is true if the shortest path has been determined from the starting vertex to
     * the vertex with ID `id`.
     */
    private final BitSet settledIds;

    /**
     * Create a new snapshot of the progress of finding shortest paths from a vertex with ID
     * `startId`. `distances[id]` must specify the total weight of the shortest known path so far
     * from the start to the vertex with ID `id`, while `predecessors[id]` must provide the ID of
     * the penultimate vertex along that path (both should be -1 if no path reaching that vertex has
     * yet been found).  `settledIds` must specify for which vertex IDs the true shortest path has
     * been found.  Note that defensive copies are made of all arguments.
     */
    public PathfindingSnapshot(int startId, int[] distances, int[] predecessors,
            BitSet settledIds) {
        this.startId = startId;
        this.predecessors = Arrays.copyOf(predecessors, predecessors.length);
        this.distances = Arrays.copyOf(distances, distances.length);
        this.settledIds = (BitSet) settledIds.clone();
    }

    /**
     * Return the sequence of vertex IDs representing a shortest known path from the starting vertex
     * to the vertex with ID `dstId` (inclusive).  If the destination vertex is "settled", then this
     * path's distance will be the shortest possible.  Throws IllegalArgumentException if the
     * destination vertex has not been discovered.  Note that, while the returned path may not be
     * the unique path with that distance, an instance of this class will always return the same
     * path when queried for the same destination.
     */
    public List<Integer> pathTo(int dstId) {
        // TODO A6.1b: Implement this method as specified (hint: remember that reconstructing paths
        //  from back pointers was a lecture exercise).
        List<Integer> ansPath = new ArrayList<>();
        if(!discovered(dstId)){
            throw new IllegalArgumentException();
        }
        for (int vId = dstId; vId != -1; vId = predecessors[vId]) {
            ansPath.add(0, vId);
            if (vId == startId) {
                break;
            }
        }

        return ansPath;
    }

    /**
     * Return the total weight along the shortest known path from the starting vertex to the vertex
     * with ID `dstId`.  If no path is known, return -1.  Requires `dstId` is a valid vertex ID in
     * the graph.
     */
    public int distanceTo(int dstId) {
        return distances[dstId];
    }

    /**
     * Return the ID of the starting vertex for the pathfinding operation that produced this
     * snapshot.
     */
    public int start() {
        return startId;
    }

    /**
     * Return whether a path is known from the starting vertex to the vertex with ID `id`.
     */
    public boolean discovered(int id) {
        return predecessors[id] >= 0 || id == startId;
    }

    /**
     * Return whether a path with the shortest possible distance is known from the starting vertex
     * to the vertex with ID `id`.
     */
    public boolean settled(int id) {
        return settledIds.get(id);
    }
}
