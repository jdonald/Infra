package jiyuiydi.infra.OS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import jiyuiydi.infra.*;
import jiyuiydi.util.BufferedBuffer;
import jiyuiydi.util.Observer;
import jiyuiydi.util.Utilities;

public class OSExecutable extends LibraryPatch implements Runnable {

	static final Node ID = null;

	Process proc;
	final String arg0;

	ByteBuffer inputBuffer = ByteBuffer.allocate(1024); // must be at least 1 byte.
	UTF8 input_text;
	Box input_infra;
	IncrementalDecoder partialInfra;

	OSExecutable(File f) {
		with(Metadata.lang_ID, f.getName());
		String path;
		try {
			path = f.getCanonicalPath();
		} catch (IOException e) {
			path = f.getPath();
			e.printStackTrace();
		}
		arg0 = path;
	}

	@Override
	public Node getOutput() {
		input_text = new UTF8();
		input_infra = new Box();
		partialInfra = new IncrementalDecoder(input_infra);
		inputBuffer.clear();

		ProcessBuilder pb = new ProcessBuilder();
		List<String> commands = new ArrayList<String>();
		commands.add(arg0);
		for(int i = 0; i < count(); ++i) {
			if(get(i) instanceof UTF8)
				commands.add(((UTF8) get(i)).get());
			if(get(i) instanceof Quantity)
				commands.add(((Quantity) get(i)).get().toString());
		}
		pb.command(commands);
		try {
			proc = pb.start();
			new Thread(this).start();
		} catch (IOException e) { return new Symbol.Error().with(Metadata.lang_comment, "Cannot start process."); }

		Box terminal = new ExeOutput(proc);
		terminal.add(input_infra);
		return terminal;
	}

	//@Override unevaluate() { proc.destroy(); result = reduced = null; }

	@Override
	public void run() { // read data from the process
		InputStream in = proc.getInputStream();
		try {
			while(true) {
				int avail = in.available();
				if(avail == 0) {
					int count = in.read(inputBuffer.array(), inputBuffer.position(), 1); // read 1 to block on input
					if(count < 0) break; // end of stream
					inputBuffer.position(inputBuffer.position() + count); // count will always be 1 here
					try { Thread.sleep(100); } catch(InterruptedException e) {} // now that input has arrived, let it build up a little
				}
				avail = Math.min(in.available(), inputBuffer.remaining());
				int count = in.read(inputBuffer.array(), inputBuffer.position(), avail); // will not block
				inputBuffer.position(inputBuffer.position() + count);

				AsynchronousEditsQueue.instance.add(new EditAction.InsertInText(input_text, new String(inputBuffer.array(), 0, inputBuffer.position()), -1));
				if(partialInfra != null) {
					ByteBuffer chunk = ByteBuffer.wrap(inputBuffer.array(), 0, inputBuffer.position());
					if(!partialInfra.injest(chunk)) {
						AsynchronousEditsQueue.instance.add(new EditAction.RemoveFromBox((Box) result, result.indexOf(input_infra)));
						partialInfra = null;
						AsynchronousEditsQueue.instance.add(new EditAction.InsertInBox(input_text, (Box) result, -1));
					}
				}
				inputBuffer.position(0);
				try { Thread.sleep(100); } catch(InterruptedException e) {} // Let the input build up a little bit for efficiency, but stay responsive.
			}
		} catch (IOException e) { e.printStackTrace(); }
		((ExeOutput)result).add("[end]");
	}

}

class ExeOutput extends Box implements Sender {

	final Process proc;
	OutputStream out;

	public ExeOutput(Process proc) { this.proc = proc; out = proc.getOutputStream(); }

	@Override
	public boolean send(Node n) {
		//TODO: out.write(InfraEncoding.write(n, out));

		BufferedBuffer bb = InfraEncoding.encoded(n);
		ByteBuffer b = bb.read((int) bb.remaining());
		try {
			out.write(b.array(), b.position(), b.remaining());
			out.flush();
			return true;
		} catch (IOException e) { e.printStackTrace(); }

		return false;
	}

	@Override
	public void removeObserver(Observer o) {
		super.removeObserver(o);
		if(countObservers() == 0) proc.destroy();
	}

}

@PatchEditor.SubclassSpecialty(OSExecutable.class)
class ExeOutputEditor extends PatchEditor {

	public ExeOutputEditor(Patch p, Node result) {
		super(p, result);
	}

	@Override
	public EditAction performEdit(EditAction in) {
		in.forPatchEditorsOnly = false;
		return in; // make the edits directly to the output
	}

}

class IncrementalDecoder {

	static class OpenSegment {
		Node n;
		long bytesRemaining;
		public OpenSegment(Node node, long contentBytesRemaining) { n = node; bytesRemaining = contentBytesRemaining; }
	}

	// instance //////////////////////////////////////////////////////////

	final Stack<OpenSegment> stack = new Stack<>();
	final Box result;
	ByteBuffer headerBuffer; // to store unconsumed chunks to guard against headers getting cut off at the end

	public IncrementalDecoder(Box destination) {
		result = destination;
		headerBuffer = ByteBuffer.allocate(9);
		headerBuffer.limit(0);
	}

	boolean injest(ByteBuffer chunk) {
		BufferedBuffer source = BufferedBuffer.wrap(chunk);
		while(source.remaining() > 0) {
			if(stack.isEmpty() || stack.peek().n instanceof Box) { // Starting a new segment
				Header h;
				if(headerBuffer.limit() > 0) { // have a previously incomplete header
					int preexistingHeaderBytes = headerBuffer.limit();
					headerBuffer.position(headerBuffer.limit()); // mark the end
					int bytesToAdd = Math.min(headerBuffer.capacity() - headerBuffer.position(), chunk.remaining());
					headerBuffer.limit(headerBuffer.limit() + bytesToAdd); // take as much more as possible
					headerBuffer.put(chunk.array(), 0, bytesToAdd); // add to it
					//headerBuffer.limit(headerBuffer.position()); // cap off
					headerBuffer.position(0); // get ready to try reading a header
					h = Header.decode(BufferedBuffer.wrap(headerBuffer));
					if(h == null) {
						if(chunk.remaining() > bytesToAdd) return false; // headerBuffer overflowed and still no header. FAIL
						return true;
					}
					headerBuffer.limit(0); // done with header
					chunk.position(h.getHeaderByteLength() - preexistingHeaderBytes); // place chunk after header
				} else {
					h = Header.decode(source);
					if(h == null) { // chunk was smaller than a full header. start a headerBuffer
						chunk.position(0);
						headerBuffer.limit(chunk.remaining()); // should always be less than a complete header
						headerBuffer.put(chunk);
						return true; // maybe with the next chunk the header will be completed
					}
				}

				if(stack.size() > 0 && h.getContentByteLength() >= stack.peek().bytesRemaining)
					return false;
				for(OpenSegment os : stack) { // decrement all
					os.bytesRemaining -= h.getHeaderByteLength();
					if(os.bytesRemaining < 0) return false; // detect malformed Infra encoding
				}

				Box dest = stack.isEmpty() ? result : (Box) stack.peek().n;
				OpenSegment seg = new OpenSegment(h.constructNode(), h.getContentByteLength());
				AsynchronousEditsQueue.instance.add(new EditAction.InsertInBox(seg.n, dest, -1));

				stack.push(seg);
				while(stack.size() > 0 && stack.peek().bytesRemaining == 0)
					stack.pop();
			} else { // Continuing a previously started segment
				Node curr = stack.peek().n;
				int ready = (int) Math.min(stack.peek().bytesRemaining, source.remaining());
				long finalPos = source.position() + ready;

				if(curr instanceof UTF8) {
					String text = new String(source.readString(ready));
					AsynchronousEditsQueue.instance.add(new EditAction.InsertInText((UTF8) curr, text, -1));
				}
				else
				if(curr instanceof Int32) {
					int v = ((Int32) curr).getInt();
					for(int i = 0; i < ready; ++i) {
						v *= 256;
						v += source.read1();
					}
					AsynchronousEditsQueue.instance.add(new EditAction.SetQuantity((Quantity) curr, v));
				}

				source.position(finalPos); // in case position did not advance in conditions above
				for(OpenSegment os : stack) { // decrement all
					os.bytesRemaining -= ready;
					if(os.bytesRemaining < 0) return false; // detect malformed Infra encoding
				}
				while(stack.size() > 0 && stack.peek().bytesRemaining == 0)
					stack.pop();
			}
		}
		return true;
	}

}
