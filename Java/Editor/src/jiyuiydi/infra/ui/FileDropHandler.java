package jiyuiydi.infra.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.TransferHandler;

public class FileDropHandler extends TransferHandler {

	private static final long serialVersionUID = 1L;

	Consumer<File> fileConsumer;

	public FileDropHandler(Consumer<File> fc) { fileConsumer = fc; }

	@Override
	public boolean canImport(TransferSupport support) {
		return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
	}

	@Override
	public boolean importData(TransferSupport support) {
		if(!canImport(support)) return false;

		try {
			@SuppressWarnings("unchecked")
			List<File> l = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
			for(File f : l)
				fileConsumer.accept(f);
		} catch (UnsupportedFlavorException | IOException e) { e.printStackTrace(); }
		return false;
	}

}
