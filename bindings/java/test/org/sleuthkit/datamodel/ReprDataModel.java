package org.sleuthkit.datamodel;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * ReprDataModel writes a String representation (containing the results of all
 * public method calls) of a Content object and its children to the given
 * Appendable.
 *
 * @author pmartel
 */
public class ReprDataModel {

	int indentLevel = 0;
	Appendable result;
	ContentVisitor reprVisitor = new ReprVisitor();
	ContentVisitor<List<? extends Content>> childrenVisitor = new ContentChildrenVisitor();
	static final int READ_BUFFER_SIZE = 8192;
	static final String HASH_ALGORITHM = "MD5";

	/**
	 * 
	 * @param result what to append the generated representation to.
	 */
	ReprDataModel(Appendable result) {
		this.result = result;
	}

	/**
	 * Entry point to represent a Content object and it's children
	 * @param c the root Content object
	 */
	public void start(Content c) {
		reprContent(c);
	}

	private void title(String title) {
		indent();
		append(title);
		append(" >");
		indentLevel++;
		nl();
	}

	private void tail() {
		indentLevel--;
	}

	private void indent() {
		char[] indentation = new char[indentLevel];
		Arrays.fill(indentation, '\t');
		append(CharBuffer.wrap(indentation));
	}

	private void nl() {
		append("\n");
	}

	private void name(String name) {
		append(name);
		append(": ");
	}

	private void reprContent(Content c) {
		title(c.getClass().getSimpleName());
		c.accept(reprVisitor);
		readContent(c);
		reprChildren(c.accept(childrenVisitor));
		tail();
	}

	private void readContent(Content c) {
		long size = c.getSize();
		byte[] readBuffer;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");

			for (long i = 0; i < size; i = i + READ_BUFFER_SIZE) {
				readBuffer = c.read(i, Math.min(size - i, READ_BUFFER_SIZE));
				md5.update(readBuffer);
			}
			String hash = toHex(md5.digest());

			repr("read", "md5=" + hash);

		} catch (TskException ex) {
			repr("read", ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
		nl();

	}

	private String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02x", b & 0xFF));
		}
		return hex.toString();
	}

	private void reprChildren(List<? extends Content> lc) {
		for (Content c : lc) {
			reprContent(c);
		}
	}

	// Files and Directories can be handled the same
	private void reprFsContent(FsContent fsc) {
		repr("getAtime", fsc.getAtime());
		repr("getAtimeAsDate", fsc.getAtimeAsDate());
		repr("getAttr_id", fsc.getAttr_id());
		repr("getAttr_type", fsc.getAttr_type());
		repr("getCrtime", fsc.getCrtime());
		repr("getCrtimeAsDate", fsc.getCrtimeAsDate());
		repr("getCtime", fsc.getCtime());
		repr("getCtimeAsDate", fsc.getCtimeAsDate());
		repr("getDirFlagsAsString", fsc.getDirFlagsAsString());
		repr("getDirTypeAsString", fsc.getDirTypeAsString());
		repr("getDir_flags", fsc.getDir_flags());
		repr("getDir_type", fsc.getDir_type());
		repr("getDirtype", fsc.getDirtype());
		repr("getFile_id", fsc.getFile_id());
		repr("getFs_id", fsc.getFs_id());
		repr("getGid", fsc.getGid());
		repr("getMetaFlagsAsString", fsc.getMetaFlagsAsString());
		repr("getMetaTypeAsString", fsc.getMetaTypeAsString());
		repr("getMeta_flags", fsc.getMeta_flags());
		repr("getMeta_type", fsc.getMeta_type());
		repr("getMode", fsc.getMode());
		repr("getModeAsString", fsc.getModeAsString());
		repr("getMtime", fsc.getMtime());
		repr("getMtimeAsDate", fsc.getMtimeAsDate());
		repr("getName", fsc.getName());
		repr("getPar_file_id", fsc.getPar_file_id());
		repr("getSize", fsc.getSize());
		repr("getUid", fsc.getUid());
	}

	private void reprFileSystem(FileSystem fs) {
		repr("getBlock_count", fs.getBlock_count());
		repr("getBlock_size", fs.getBlock_size());
		
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		try {
			repr("getFileSystemHandle", fs.getFileSystemHandle());
		} catch (TskException ex) {
			throw new RuntimeException(ex);
		}
		 * 
		 */
		repr("getFirst_inum", fs.getFirst_inum());
		repr("getFs_id", fs.getFs_id());
		repr("getFs_type", fs.getFs_type());
		repr("getImg_offset", fs.getImg_offset());
		repr("getLast_inum", fs.getLast_inum());
		repr("getRoot_inum", fs.getRoot_inum());
		repr("getSize", fs.getSize());
		repr("getVol_id", fs.getVol_id());
	}

	private void reprImage(Image i) {
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		repr("getImageHandle", i.getImageHandle());
		 * 
		 */
		repr("getName", i.getName());
		repr("getPaths", i.getPaths());
		repr("getSize", i.getSize());
		repr("getSsize", i.getSsize());
		repr("getType", i.getType());
	}

	private void reprVolume(Volume v) {
		repr("getDescription", v.getDescription());
		repr("getFlags", v.getFlags());
		repr("getFlagsAsString", v.getFlagsAsString());
		repr("getLength", v.getLength());
		repr("getSize", v.getSize());
		repr("getStart", v.getStart());
		repr("getVol_id", v.getVol_id());
	}

	private void reprVolumeSystem(VolumeSystem vs) {
		repr("getBlockSize", vs.getBlockSize());
		repr("getOffset", vs.getOffset());
		repr("getSize", vs.getSize());
		repr("getType", vs.getType());
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		try {
			repr("getVolumeSystemHandle", vs.getVolumeSystemHandle());
		} catch (TskException ex) {
			throw new RuntimeException(ex);
		}
		 * 
		 */
	}

	private void repr(String method, Long l) {
		indent();
		name(method);
		append(l.toString());
		nl();
	}

	private void repr(String method, String[] sArray) {
		indent();
		name(method);
		append(Arrays.toString(sArray));
		nl();
	}

	private void repr(String method, String s) {
		indent();
		name(method);
		append(s);
		nl();
	}

	private void repr(String method, Exception ex) {
		indent();
		name(method);
		nl();
		append(ex.toString());
		nl();
	}

	private void append(CharSequence s) {
		try {
			result.append(s);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private class ReprVisitor implements ContentVisitor<Void> {

		@Override
		public Void visit(Directory d) {
			reprFsContent(d);
			return null;
		}

		@Override
		public Void visit(File f) {
			reprFsContent(f);
			return null;
		}

		@Override
		public Void visit(FileSystem fs) {
			reprFileSystem(fs);
			return null;
		}

		@Override
		public Void visit(Image i) {
			reprImage(i);
			return null;
		}

		@Override
		public Void visit(Volume v) {
			reprVolume(v);
			return null;
		}

		@Override
		public Void visit(VolumeSystem vs) {
			reprVolumeSystem(vs);
			return null;
		}
	}
}
package org.sleuthkit.datamodel;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * ReprDataModel writes a String representation (containing the results of all
 * public method calls) of a Content object and its children to the given
 * Appendable.
 *
 * @author pmartel
 */
public class ReprDataModel {

	int indentLevel = 0;
	Appendable result;
	ContentVisitor reprVisitor = new ReprVisitor();
	ContentVisitor<List<? extends Content>> childrenVisitor = new ContentChildrenVisitor();
	static final int READ_BUFFER_SIZE = 8192;
	static final String HASH_ALGORITHM = "MD5";

	/**
	 * 
	 * @param result what to append the generated representation to.
	 */
	ReprDataModel(Appendable result) {
		this.result = result;
	}

	/**
	 * Entry point to represent a Content object and it's children
	 * @param c the root Content object
	 */
	public void start(Content c) {
		reprContent(c);
	}

	private void title(String title) {
		indent();
		append(title);
		append(" >");
		indentLevel++;
		nl();
	}

	private void tail() {
		indentLevel--;
	}

	private void indent() {
		char[] indentation = new char[indentLevel];
		Arrays.fill(indentation, '\t');
		append(CharBuffer.wrap(indentation));
	}

	private void nl() {
		append("\n");
	}

	private void name(String name) {
		append(name);
		append(": ");
	}

	private void reprContent(Content c) {
		title(c.getClass().getSimpleName());
		c.accept(reprVisitor);
		readContent(c);
		reprChildren(c.accept(childrenVisitor));
		tail();
	}

	private void readContent(Content c) {
		long size = c.getSize();
		byte[] readBuffer;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");

			for (long i = 0; i < size; i = i + READ_BUFFER_SIZE) {
				readBuffer = c.read(i, Math.min(size - i, READ_BUFFER_SIZE));
				md5.update(readBuffer);
			}
			String hash = toHex(md5.digest());

			repr("read", "md5=" + hash);

		} catch (TskException ex) {
			repr("read", ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
		nl();

	}

	private String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02x", b & 0xFF));
		}
		return hex.toString();
	}

	private void reprChildren(List<? extends Content> lc) {
		for (Content c : lc) {
			reprContent(c);
		}
	}

	// Files and Directories can be handled the same
	private void reprFsContent(FsContent fsc) {
		repr("getAtime", fsc.getAtime());
		repr("getAtimeAsDate", fsc.getAtimeAsDate());
		repr("getAttr_id", fsc.getAttr_id());
		repr("getAttr_type", fsc.getAttr_type());
		repr("getCrtime", fsc.getCrtime());
		repr("getCrtimeAsDate", fsc.getCrtimeAsDate());
		repr("getCtime", fsc.getCtime());
		repr("getCtimeAsDate", fsc.getCtimeAsDate());
		repr("getDirFlagsAsString", fsc.getDirFlagsAsString());
		repr("getDirTypeAsString", fsc.getDirTypeAsString());
		repr("getDir_flags", fsc.getDir_flags());
		repr("getDir_type", fsc.getDir_type());
		repr("getDirtype", fsc.getDirtype());
		repr("getFile_id", fsc.getFile_id());
		repr("getFs_id", fsc.getFs_id());
		repr("getGid", fsc.getGid());
		repr("getMetaFlagsAsString", fsc.getMetaFlagsAsString());
		repr("getMetaTypeAsString", fsc.getMetaTypeAsString());
		repr("getMeta_flags", fsc.getMeta_flags());
		repr("getMeta_type", fsc.getMeta_type());
		repr("getMode", fsc.getMode());
		repr("getModeAsString", fsc.getModeAsString());
		repr("getMtime", fsc.getMtime());
		repr("getMtimeAsDate", fsc.getMtimeAsDate());
		repr("getName", fsc.getName());
		repr("getPar_file_id", fsc.getPar_file_id());
		repr("getSize", fsc.getSize());
		repr("getUid", fsc.getUid());
	}

	private void reprFileSystem(FileSystem fs) {
		repr("getBlock_count", fs.getBlock_count());
		repr("getBlock_size", fs.getBlock_size());
		
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		try {
			repr("getFileSystemHandle", fs.getFileSystemHandle());
		} catch (TskException ex) {
			throw new RuntimeException(ex);
		}
		 * 
		 */
		repr("getFirst_inum", fs.getFirst_inum());
		repr("getFs_id", fs.getFs_id());
		repr("getFs_type", fs.getFs_type());
		repr("getImg_offset", fs.getImg_offset());
		repr("getLast_inum", fs.getLast_inum());
		repr("getRoot_inum", fs.getRoot_inum());
		repr("getSize", fs.getSize());
		repr("getVol_id", fs.getVol_id());
	}

	private void reprImage(Image i) {
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		repr("getImageHandle", i.getImageHandle());
		 * 
		 */
		repr("getName", i.getName());
		repr("getPaths", i.getPaths());
		repr("getSize", i.getSize());
		repr("getSsize", i.getSsize());
		repr("getType", i.getType());
	}

	private void reprVolume(Volume v) {
		repr("getDescription", v.getDescription());
		repr("getFlags", v.getFlags());
		repr("getFlagsAsString", v.getFlagsAsString());
		repr("getLength", v.getLength());
		repr("getSize", v.getSize());
		repr("getStart", v.getStart());
		repr("getVol_id", v.getVol_id());
	}

	private void reprVolumeSystem(VolumeSystem vs) {
		repr("getBlockSize", vs.getBlockSize());
		repr("getOffset", vs.getOffset());
		repr("getSize", vs.getSize());
		repr("getType", vs.getType());
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		try {
			repr("getVolumeSystemHandle", vs.getVolumeSystemHandle());
		} catch (TskException ex) {
			throw new RuntimeException(ex);
		}
		 * 
		 */
	}

	private void repr(String method, Long l) {
		indent();
		name(method);
		append(l.toString());
		nl();
	}

	private void repr(String method, String[] sArray) {
		indent();
		name(method);
		append(Arrays.toString(sArray));
		nl();
	}

	private void repr(String method, String s) {
		indent();
		name(method);
		append(s);
		nl();
	}

	private void repr(String method, Exception ex) {
		indent();
		name(method);
		nl();
		append(ex.toString());
		nl();
	}

	private void append(CharSequence s) {
		try {
			result.append(s);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private class ReprVisitor implements ContentVisitor<Void> {

		@Override
		public Void visit(Directory d) {
			reprFsContent(d);
			return null;
		}

		@Override
		public Void visit(File f) {
			reprFsContent(f);
			return null;
		}

		@Override
		public Void visit(FileSystem fs) {
			reprFileSystem(fs);
			return null;
		}

		@Override
		public Void visit(Image i) {
			reprImage(i);
			return null;
		}

		@Override
		public Void visit(Volume v) {
			reprVolume(v);
			return null;
		}

		@Override
		public Void visit(VolumeSystem vs) {
			reprVolumeSystem(vs);
			return null;
		}
	}
}
package org.sleuthkit.datamodel;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * ReprDataModel writes a String representation (containing the results of all
 * public method calls) of a Content object and its children to the given
 * Appendable.
 *
 * @author pmartel
 */
public class ReprDataModel {

	int indentLevel = 0;
	Appendable result;
	ContentVisitor reprVisitor = new ReprVisitor();
	ContentVisitor<List<? extends Content>> childrenVisitor = new ContentChildrenVisitor();
	static final int READ_BUFFER_SIZE = 8192;
	static final String HASH_ALGORITHM = "MD5";

	/**
	 * 
	 * @param result what to append the generated representation to.
	 */
	ReprDataModel(Appendable result) {
		this.result = result;
	}

	/**
	 * Entry point to represent a Content object and it's children
	 * @param c the root Content object
	 */
	public void start(Content c) {
		reprContent(c);
	}

	private void title(String title) {
		indent();
		append(title);
		append(" >");
		indentLevel++;
		nl();
	}

	private void tail() {
		indentLevel--;
	}

	private void indent() {
		char[] indentation = new char[indentLevel];
		Arrays.fill(indentation, '\t');
		append(CharBuffer.wrap(indentation));
	}

	private void nl() {
		append("\n");
	}

	private void name(String name) {
		append(name);
		append(": ");
	}

	private void reprContent(Content c) {
		title(c.getClass().getSimpleName());
		c.accept(reprVisitor);
		readContent(c);
		reprChildren(c.accept(childrenVisitor));
		tail();
	}

	private void readContent(Content c) {
		long size = c.getSize();
		byte[] readBuffer;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");

			for (long i = 0; i < size; i = i + READ_BUFFER_SIZE) {
				readBuffer = c.read(i, Math.min(size - i, READ_BUFFER_SIZE));
				md5.update(readBuffer);
			}
			String hash = toHex(md5.digest());

			repr("read", "md5=" + hash);

		} catch (TskException ex) {
			repr("read", ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
		nl();

	}

	private String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02x", b & 0xFF));
		}
		return hex.toString();
	}

	private void reprChildren(List<? extends Content> lc) {
		for (Content c : lc) {
			reprContent(c);
		}
	}

	// Files and Directories can be handled the same
	private void reprFsContent(FsContent fsc) {
		repr("getAtime", fsc.getAtime());
		repr("getAtimeAsDate", fsc.getAtimeAsDate());
		repr("getAttr_id", fsc.getAttr_id());
		repr("getAttr_type", fsc.getAttr_type());
		repr("getCrtime", fsc.getCrtime());
		repr("getCrtimeAsDate", fsc.getCrtimeAsDate());
		repr("getCtime", fsc.getCtime());
		repr("getCtimeAsDate", fsc.getCtimeAsDate());
		repr("getDirFlagsAsString", fsc.getDirFlagsAsString());
		repr("getDirTypeAsString", fsc.getDirTypeAsString());
		repr("getDir_flags", fsc.getDir_flags());
		repr("getDir_type", fsc.getDir_type());
		repr("getDirtype", fsc.getDirtype());
		repr("getFile_id", fsc.getFile_id());
		repr("getFs_id", fsc.getFs_id());
		repr("getGid", fsc.getGid());
		repr("getMetaFlagsAsString", fsc.getMetaFlagsAsString());
		repr("getMetaTypeAsString", fsc.getMetaTypeAsString());
		repr("getMeta_flags", fsc.getMeta_flags());
		repr("getMeta_type", fsc.getMeta_type());
		repr("getMode", fsc.getMode());
		repr("getModeAsString", fsc.getModeAsString());
		repr("getMtime", fsc.getMtime());
		repr("getMtimeAsDate", fsc.getMtimeAsDate());
		repr("getName", fsc.getName());
		repr("getPar_file_id", fsc.getPar_file_id());
		repr("getSize", fsc.getSize());
		repr("getUid", fsc.getUid());
	}

	private void reprFileSystem(FileSystem fs) {
		repr("getBlock_count", fs.getBlock_count());
		repr("getBlock_size", fs.getBlock_size());
		
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		try {
			repr("getFileSystemHandle", fs.getFileSystemHandle());
		} catch (TskException ex) {
			throw new RuntimeException(ex);
		}
		 * 
		 */
		repr("getFirst_inum", fs.getFirst_inum());
		repr("getFs_id", fs.getFs_id());
		repr("getFs_type", fs.getFs_type());
		repr("getImg_offset", fs.getImg_offset());
		repr("getLast_inum", fs.getLast_inum());
		repr("getRoot_inum", fs.getRoot_inum());
		repr("getSize", fs.getSize());
		repr("getVol_id", fs.getVol_id());
	}

	private void reprImage(Image i) {
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		repr("getImageHandle", i.getImageHandle());
		 * 
		 */
		repr("getName", i.getName());
		repr("getPaths", i.getPaths());
		repr("getSize", i.getSize());
		repr("getSsize", i.getSsize());
		repr("getType", i.getType());
	}

	private void reprVolume(Volume v) {
		repr("getDescription", v.getDescription());
		repr("getFlags", v.getFlags());
		repr("getFlagsAsString", v.getFlagsAsString());
		repr("getLength", v.getLength());
		repr("getSize", v.getSize());
		repr("getStart", v.getStart());
		repr("getVol_id", v.getVol_id());
	}

	private void reprVolumeSystem(VolumeSystem vs) {
		repr("getBlockSize", vs.getBlockSize());
		repr("getOffset", vs.getOffset());
		repr("getSize", vs.getSize());
		repr("getType", vs.getType());
		/* 
		 * Don't get handle, it's not consistent (a memory pointer).
		 * 
		try {
			repr("getVolumeSystemHandle", vs.getVolumeSystemHandle());
		} catch (TskException ex) {
			throw new RuntimeException(ex);
		}
		 * 
		 */
	}

	private void repr(String method, Long l) {
		indent();
		name(method);
		append(l.toString());
		nl();
	}

	private void repr(String method, String[] sArray) {
		indent();
		name(method);
		append(Arrays.toString(sArray));
		nl();
	}

	private void repr(String method, String s) {
		indent();
		name(method);
		append(s);
		nl();
	}

	private void repr(String method, Exception ex) {
		indent();
		name(method);
		nl();
		append(ex.toString());
		nl();
	}

	private void append(CharSequence s) {
		try {
			result.append(s);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private class ReprVisitor implements ContentVisitor<Void> {

		@Override
		public Void visit(Directory d) {
			reprFsContent(d);
			return null;
		}

		@Override
		public Void visit(File f) {
			reprFsContent(f);
			return null;
		}

		@Override
		public Void visit(FileSystem fs) {
			reprFileSystem(fs);
			return null;
		}

		@Override
		public Void visit(Image i) {
			reprImage(i);
			return null;
		}

		@Override
		public Void visit(Volume v) {
			reprVolume(v);
			return null;
		}

		@Override
		public Void visit(VolumeSystem vs) {
			reprVolumeSystem(vs);
			return null;
		}
	}
}