package rs.io;

import rs.io.bzip2.BZip2InputStream;
import rs.util.Strings;

public final class Archive {

	public byte[] data;
	public int fileCount;
	public int[] fileHash;
	public int[] fileUnpackedSize;
	public int[] filePackedSize;
	public int[] fileOffset;

	public Archive(byte[] src) {
		read(src);
	}

	private void read(byte[] src) {
		Buffer b = new Buffer(src);

		int unpackedSize = b.getInt24();
		int packedSize = b.getInt24();

		if (packedSize != unpackedSize) {
			byte[] dst = new byte[unpackedSize];
			BZip2InputStream.read(dst, unpackedSize, src, packedSize, 6);
			data = dst;
			b = new Buffer(data);
		} else {
			data = src;
		}

		fileCount = b.getUShort();
		fileHash = new int[fileCount];
		fileUnpackedSize = new int[fileCount];
		filePackedSize = new int[fileCount];
		fileOffset = new int[fileCount];

		int pos = b.position + fileCount * 10;
		for (int n = 0; n < fileCount; n++) {
			fileHash[n] = b.getInt();
			fileUnpackedSize[n] = b.getInt24();
			filePackedSize[n] = b.getInt24();
			fileOffset[n] = pos;
			pos += filePackedSize[n];
		}
	}

	public byte[] get(String name) {
		return get(name, null);
	}

	public byte[] get(String name, byte[] dst) {
		int hash = Strings.getHash(name);

		for (int n = 0; n < fileCount; n++) {
			if (fileHash[n] == hash) {
				if (dst == null) {
					dst = new byte[fileUnpackedSize[n]];
				}

				if (fileUnpackedSize[n] != filePackedSize[n]) {
					BZip2InputStream.read(dst, fileUnpackedSize[n], data, filePackedSize[n], fileOffset[n]);
				} else {
					System.arraycopy(data, fileOffset[n], dst, 0, fileUnpackedSize[n]);
				}
				return dst;
			}
		}
		return null;
	}
}
