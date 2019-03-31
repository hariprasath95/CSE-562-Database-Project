package dubstep.executor;

import dubstep.utils.Tuple;

import java.util.ArrayList;

public class JoinNode extends BaseNode {

    private Tuple innerTuple;
    private boolean initJoin = false;

    public JoinNode(BaseNode innerNode,BaseNode outerNode) {
        this.innerNode = innerNode;
        this.innerNode.parentNode = this;
        this.outerNode = outerNode;
        this.outerNode.parentNode = this;
        this.outerNode.isInner = false;
        this.initProjectionInfo();
    }

    @Override
    Tuple getNextRow() {
        if (!initJoin) {
            innerTuple = this.innerNode.getNextTuple();
            initJoin = true;
        }
        if(innerTuple == null)
            return null;
        Tuple outerTuple;
        outerTuple = this.outerNode.getNextTuple();

        if (outerTuple != null) {
            return new Tuple(innerTuple, outerTuple);
        } else {
            this.outerNode.resetIterator();
            innerTuple = this.innerNode.getNextTuple();
            outerTuple = this.outerNode.getNextTuple();
            if (innerTuple == null || outerTuple == null) {
                return null;
            } else {
                return new Tuple(innerTuple, outerTuple);
            }
        }
    }

    @Override
    void resetIterator() {
        this.innerTuple = null;
        this.initJoin = false;
        this.innerNode.resetIterator();
        this.outerNode.resetIterator();
    }

    @Override
    void initProjectionInfo() {
        this.projectionInfo = new ArrayList<>(this.innerNode.projectionInfo);
        this.projectionInfo.addAll(this.outerNode.projectionInfo);
    }
}
