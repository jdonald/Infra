package jiyuiydi.infra;

public abstract class LibraryPatch extends Patch {

	@Override
	public void execute() {
		init();
		result = getOutput();
	}

	public abstract Node getOutput();

}
