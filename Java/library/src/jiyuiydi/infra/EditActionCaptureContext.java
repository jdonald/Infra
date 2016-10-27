package jiyuiydi.infra;

public interface EditActionCaptureContext {

	void performEdit(EditAction a);

	boolean isCaptureEnabled();

	void setCaptureEnabled(boolean enabled);

}
