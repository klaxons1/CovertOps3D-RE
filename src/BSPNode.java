public final class BSPNode {

    private int splitX;
    private int splitZ;
    private int normalX;
    private int splitSlope;

    private int frontChildIndex;
    private int backChildIndex;

    private Object frontChild;   // BSPNode or Sector
    private Object backChild;    // BSPNode or Sector

    public boolean[] visibleSectors;
    public static Sector[] visibleSectorsList;
    public static int visibleSectorsCount;

    private static final int LEAF_FLAG = 32768; // 0x8000

    public BSPNode(int splitX, int splitZ, int normalX, int splitDy,
                   int frontChildIndex, int backChildIndex) {
        this.splitX = splitX;
        this.splitZ = splitZ;
        this.normalX = normalX;
        this.splitSlope = MathUtils.fixedPointDivide(splitDy, normalX);

        this.frontChildIndex = frontChildIndex;
        this.backChildIndex  = backChildIndex;

        this.visibleSectors = null;
    }

    public final void initializeBSPNode(GameWorld world) {
        // Front child
        if ((frontChildIndex & LEAF_FLAG) != 0) {
            int sectorIdx = frontChildIndex - LEAF_FLAG;
            this.frontChild = world.bspSectors[sectorIdx];
        } else {
            this.frontChild = world.bspNodes[frontChildIndex];
        }

        // Back child
        if ((backChildIndex & LEAF_FLAG) != 0) {
            int sectorIdx = backChildIndex - LEAF_FLAG;
            this.backChild = world.bspSectors[sectorIdx];
        } else {
            this.backChild = world.bspNodes[backChildIndex];
        }
    }

    private boolean isPointInFront(int x, int z) {
        if (splitSlope == Integer.MAX_VALUE) {
            return splitX - x >= 0;
        }
        if (splitSlope == Integer.MIN_VALUE) {
            return x - splitX >= 0;
        }

        long dx = x - splitX;
        long predictedZ = splitZ + ((long)splitSlope * dx >> 16);
        boolean front = (z - predictedZ) >= 0;

        if (normalX < 0) {
            front = !front;
        }
        return front;
    }

    public final void traverseBSP(Transform3D camera, SectorData fromSector) {
        int playerX = camera.x;
        int playerZ = camera.z;

        Object nearChild, farChild;
        if (isPointInFront(playerX, playerZ)) {
            nearChild = backChild;
            farChild  = frontChild;
        } else {
            nearChild = frontChild;
            farChild  = backChild;
        }

        // Near subtree first
        if (nearChild instanceof BSPNode) {
            BSPNode node = (BSPNode)nearChild;
            if (fromSector.isBSPNodeVisible(node)) {
                node.traverseBSP(camera, fromSector);
            }
        } else if (nearChild instanceof Sector) {
            Sector sector = (Sector)nearChild;
            if (fromSector.isSectorConnected(sector)) {
                visibleSectorsList[visibleSectorsCount] = sector;
                visibleSectorsCount++;                     // correct increment
            }
        }

        // Far subtree second
        if (farChild instanceof BSPNode) {
            BSPNode node = (BSPNode)farChild;
            if (fromSector.isBSPNodeVisible(node)) {
                node.traverseBSP(camera, fromSector);
            }
        } else if (farChild instanceof Sector) {
            Sector sector = (Sector)farChild;
            if (fromSector.isSectorConnected(sector)) {
                visibleSectorsList[visibleSectorsCount] = sector;
                visibleSectorsCount++;                     // correct increment
            }
        }
    }

    public final SectorData findSectorAtPoint(int x, int z) {
        return findSectorNodeAtPoint(x, z).getSectorData();
    }

    public final Sector findSectorNodeAtPoint(int x, int z) {
        Object child = isPointInFront(x, z) ? backChild : frontChild;
        if (child instanceof BSPNode) {
            return ((BSPNode)child).findSectorNodeAtPoint(x, z);
        } else {
            return (Sector)child;
        }
    }

    public final boolean[] calculateVisibleSectors() {
        boolean[] frontMask;
        if (frontChild instanceof BSPNode) {
            frontMask = ((BSPNode)frontChild).calculateVisibleSectors();
        } else {
            frontMask = ((Sector)frontChild).getVisibilityMask();
        }

        boolean[] backMask;
        if (backChild instanceof BSPNode) {
            backMask = ((BSPNode)backChild).calculateVisibleSectors();
        } else {
            backMask = ((Sector)backChild).getVisibilityMask();
        }

        if (visibleSectors == null || visibleSectors.length != frontMask.length) {
            visibleSectors = new boolean[frontMask.length];
        }

        for (int i = 0; i < visibleSectors.length; i++) {
            visibleSectors[i] = frontMask[i] && backMask[i];
        }

        return visibleSectors;
    }
}