package jiyuiydi.infra;

public class MutationRequest extends Node {

	final Box dstParent;
	final int dstIndexInParent;
	final Node value;

	public MutationRequest(Box parent, int indexInParent, Node value) {
		dstParent = parent;
		dstIndexInParent = indexInParent;
		this.value = value;
	}

	public EditAction getEditAction() {
		return new EditAction.ReplaceInBox(dstParent, dstIndexInParent, value);
	}

}
