package fotok.formathandler;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.stream.ImageOutputStream;

import hu.qgears.commons.Pair;
import hu.qgears.images.SizeInt;

/**
 * Exif parser and thumbnail generator.
 */
public class ExifParser {
	public ExifData parseFile(File file, Function<SizeInt, SizeInt> thumbSizeCreator) throws IOException {
//		INativeMemory mem = UtilFile.loadAsByteBuffer(file, DefaultJavaNativeMemoryAllocator.getInstance());
//		ByteBuffer bb = mem.getJavaAccessor();
		// processByImageIo(file, thumbSizeCreator);
		
		// We just map the file into memory: only the pages accessed will be loaded - extremely fast
		try (RandomAccessFile raccFile = new RandomAccessFile(file, "r")) {
			MappedByteBuffer bb = raccFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			return processFile(bb);
		}
	}

	public void processByImageIo(File file, Function<SizeInt, SizeInt> thumbSizeCreator) throws IOException {
		Iterator<?> iterator = ImageIO.getImageReadersBySuffix("jpeg");
		while (iterator.hasNext()) {
			ImageReader reader = (ImageReader) iterator.next();
			try {
				System.out.println("Reader: " + reader.getClass());
				reader.setInput(ImageIO.createImageInputStream(file));
				System.out.println("Nr of images: " + reader.getNumImages(true));
				IIOMetadata metadata = reader.getImageMetadata(0);
				String[] formatnames = metadata.getExtraMetadataFormatNames();
				if (formatnames != null) {
					for (String s : formatnames) {
						System.out.println("XXXXX" + s);
					}
				}
				System.out.println("Formatnames" + formatnames + " " + metadata.getClass());
				System.out.println("Controller: " + metadata.getController());
				System.out.println("Controller: " + metadata.getNativeMetadataFormatName());
				System.out.println("Controller: " + metadata.getAsTree(metadata.getNativeMetadataFormatName()));
				IIOMetadataFormat mdf = metadata.getMetadataFormat(metadata.getNativeMetadataFormatName());
				String rootName = mdf.getRootName();
				System.out.println("Rootname: " + rootName);
				String[] childNames = mdf.getChildNames(rootName);
				new JavaImageIoMetadata().displayMetadata(metadata.getAsTree(metadata.getNativeMetadataFormatName()));
				if (childNames != null) {
					for (String c : childNames) {
						System.out.println("child: " + c);
					}
				}
				if(thumbSizeCreator!=null)
				{
					createThumbnail(file, metadata, thumbSizeCreator, reader);
				}
			} finally {
				reader.dispose();
			}
		}
	}

	private void createThumbnail(File file, IIOMetadata metadata, Function<SizeInt, SizeInt> thumbSizeCreator, ImageReader reader) throws IOException {
		// System.out.println("Metadata: "+metadata.get);
		// As far as I understand you should provide
		// index as tiff images could have multiple pages
		BufferedImage bi = reader.read(0);
		BufferedImage thumb = new BufferedImage(320, bi.getHeight() * 320 / bi.getWidth(), bi.getType());
		Graphics2D g = thumb.createGraphics();
		try {
			g.drawImage(bi, 0, 0, thumb.getWidth(), thumb.getHeight(), null);
		} finally {
			g.dispose();
		}
		
		// ImageIO.write(bi, "jpeg", );
		writeJPG(metadata, thumb, new File("/tmp/out" + file.getName()));
	}
	/**
	 * 
	 * @param file
	 * @param size
	 * @param output
	 * @param exifOrientationOfOriginal 1 means no transformation
	 * @throws IOException
	 */
	public static void createResizedImages(File file, List<Pair<File, SizeInt>> sizes, ExifData exifData) throws IOException {
		Iterator<?> iterator = ImageIO.getImageReadersBySuffix("jpeg");
		while (iterator.hasNext()) {
			ImageReader reader = (ImageReader) iterator.next();
			try {
				reader.setInput(ImageIO.createImageInputStream(file));
				IIOMetadata metadata = reader.getImageMetadata(0);
				BufferedImage bi = reader.read(0);
				for(Pair<File, SizeInt> s: sizes)
				{
					SizeInt size=s.getB();
					File output=s.getA();
					// AffineTransform tx = null;
					// tx=new AffineTransform();
					// tx.concatenate(AffineTransform.getRotateInstance(rotationRequired, locationX, locationY));
					// tx.AffineTransform.getScaleInstance(sx, sy);
					// AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
	
	//				// Drawing the rotated image at the required drawing locations
	//				g2d.drawImage(op.filter(image, null), drawLocationX, drawLocationY, null);
					BufferedImage thumb;
					if(exifData.orientation>4)
					{
						if(exifData.width!=bi.getHeight()||exifData.height!=bi.getWidth())
						{
							throw new IOException("Image size does not fit exif data! "+exifData+" vs "+bi.getWidth()+" "+bi.getHeight()+" (rotate of image with improper tool may cause this issue)");
						}
						thumb=new BufferedImage(size.getHeight(), size.getWidth(), bi.getType());
					}else
					{
						if(exifData.width!=bi.getWidth()||exifData.height!=bi.getHeight())
						{
							throw new IOException("Image size does not fit exif data! "+exifData+" vs "+bi.getWidth()+" "+bi.getHeight()+" (rotate of image with improper tool may cause this issue)");
						}
						thumb=new BufferedImage(size.getWidth(), size.getHeight(), bi.getType());
					}
					Graphics2D g = thumb.createGraphics();
	
					try {
						g.drawImage(bi, 0, 0, thumb.getWidth(), thumb.getHeight(), null);
					} finally {
						g.dispose();
					}
	
					/**
					 * 					switch (exifOrientationOfOriginal) {
						case 1:
							break;
						case 2:
							// = 0 degrees, mirrored: image has been flipped back-to-front.
							g.drawImage(bi, thumb.getWidth(), 0, 0, thumb.getHeight(), null);
							break;
						case 3:
							// = 180 degrees: image is upside down.
							g.drawImage(bi, 0, thumb.getHeight(), thumb.getWidth(), 0, null);
							break;
						case 4:
							// = 180 degrees, mirrored: image has been flipped back-to-front and is upside down.
							g.drawImage(bi, thumb.getWidth(), thumb.getHeight(), 0, 0, null);
							break;
						case 5:
							// = 90 degrees: image has been flipped back-to-front and is on its side.
							g.drawImage(bi, thumb.getWidth(), thumb.getHeight(), 0, 0, null);
							break;
						case 6:
							// = 90 degrees, mirrored: image is on its side.
						{
							AffineTransform tx = new AffineTransform();
							tx.concatenate(AffineTransform.getTranslateInstance(locationX+200, locationY));
							//tx.concatenate(AffineTransform.getTranslateInstance(-locationX, -locationY));
							tx.concatenate(AffineTransform.getRotateInstance(Math.PI/2*0.8));
							tx.concatenate(AffineTransform.getTranslateInstance(-locationX+1800, -locationY));
							AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
							bi=op.filter(bi, new BufferedImage(bi.getHeight(), bi.getWidth(), bi.getType()));
							break;
						}
						case 7:
							// = 270 degrees: image has been flipped back-to-front and is on its far side.
							g.drawImage(bi, thumb.getWidth(), thumb.getHeight(), 0, 0, null);
							break;
						case 8:
							// = 270 degrees, mirrored: image is on its far side.
							g.drawImage(bi, thumb.getWidth(), thumb.getHeight(), 0, 0, null);
							break;
	
	
						default:
							break;
						}
	
					 */
					// ImageIO.write(thumb, "jpeg", output);
					writeJPG(metadata, thumb, output);
				}
			} finally {
				reader.dispose();
			}
		}
	}

	private static void writeJPG(IIOMetadata metadata, BufferedImage bi, File file) throws IOException {
		// I'm writing to byte array in memory, but you may use any other stream
		try (FileOutputStream os = new FileOutputStream(file)) {
			try (ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
				Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
				ImageWriter writer = iter.next();
				writer.setOutput(ios);

				// You may want also to alter jpeg quality
				ImageWriteParam iwParam = writer.getDefaultWriteParam();
				iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				iwParam.setCompressionQuality(.95f);

				// Note: we're using metadata we've already saved.
				writer.write(null, new IIOImage(bi, null, metadata), iwParam);
				writer.dispose();
			}
		}
	}

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

	private ExifData processFile(ByteBuffer bb) throws IOException {
		exifData = new ExifData();
		bb.order(ByteOrder.BIG_ENDIAN);
		short jpgMarker = bb.getShort();
		assertEq("JPG marker ", 0xFFD8, jpgMarker & 0xffff);
		boolean goon = true;
		while (bb.hasRemaining() && goon) {
			goon = processTag(bb);
		}
		if (exifData.orientation > 4) {
			int w = exifData.width;
			exifData.width = exifData.height;
			exifData.height = w;
		}
		return exifData;
	}

	private ExifData exifData = new ExifData();

	private boolean processTag(ByteBuffer bb) throws IOException {
		int posTag = bb.position();
		short tag = bb.getShort();
		switch (tag & 0xffff) {
		case 0xFFD9:
			throw new RuntimeException("DQT - ok");
		case 0xFFE0: // JPG
			throw new RuntimeException("No exif data");
		case 0xFFE1: // EXIF
		{
			int size = bb.getShort() & 0xffff;
			// System.out.println("EXiF size: 0x" + Integer.toHexString(size));
			assertEq("E", 'E', bb.get() & 0xff);
			assertEq("x", 'x', bb.get() & 0xff);
			assertEq("i", 'i', bb.get() & 0xff);
			assertEq("f", 'f', bb.get() & 0xff);
			bb.getShort(); // unused zeroes
			int exifDataOffset = bb.position();
			short endiannessCode = bb.getShort();
			switch (endiannessCode & 0xffff) {
			case 0x4949: // Little endian
				// System.out.println("LE");
				bb.order(ByteOrder.LITTLE_ENDIAN);
				break;
			case 0x4D4D: // Big endian
				// System.out.println("BE");
				break;
			default:
				break;
			}
			try {
				short v = bb.getShort();
				assertEq("0x2A ", 0x2A, v & 0xffff);
				int offsetIFD = bb.getInt();
				processIfd(bb, exifDataOffset, offsetIFD);
				int nextOffsetIFD = bb.getShort() & 0xffff;
				// System.out.println("Next ifd offset: " + Integer.toHexString(nextOffsetIFD));
			} finally {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			// System.out.println("Goto: " + Integer.toHexString(size + posTag));
			bb.position(size + posTag);
			return false;
		}
		default:
			throw new RuntimeException("Error processing JPG " + Integer.toHexString(tag & 0xffff));
		}
	}

	private void processIfd(ByteBuffer bb, int exifDataOffset, int offsetIFD) {
		int pos = exifDataOffset + offsetIFD;
		bb.position(pos);
		short n = bb.getShort();
		// System.out.println("Process IFD offset: " + exifDataOffset + " " + offsetIFD + " n: 0x" + Integer.toHexString(n)
		//		+ " " + Integer.toHexString(pos));
		for (int i = 0; i < n; ++i) {
			int code = bb.getShort() & 0xffff;
			int df = bb.getShort() & 0xffff;
			int length = bb.getInt();
			long value;
			String str = null;
			switch (df) {
			case 1:
				// 1 = BYTE An 8-bit unsigned integer.,
				value = bb.get() & 0xff;
				bb.get();
				bb.get();
				bb.get();
				break;
			// 2 = ASCII An 8-bit byte containing one 7-bit ASCII code. The final byte is
			// terminated with NULL.
			case 2: // String
				value = bb.getInt();
				ByteBuffer sub = bb.duplicate();
				sub.position(((int) value) + exifDataOffset);
				byte[] data = new byte[length];
				for (int j = 0; j < length; ++j) {
					data[j] = sub.get();
				}
				str = new String(data, StandardCharsets.UTF_8);
				break;
			case 3:
				// 3 = SHORT A 16-bit (2-byte) unsigned integer,
				value = bb.getShort() & 0xffff;
				bb.getShort();
				break;
			case 4:
				// 4 = LONG A 32-bit (4-byte) unsigned integer,
				value = bb.getInt() & 0xffffffffl;
				break;
			case 5:
				// 5 = RATIONAL Two LONGs. The first LONG is the numerator and the second LONG
				// expresses the denominator.,
				// TODO
			default:
				// TODO
				value = bb.getInt() & 0xffffffffl;
				break;

			// 7 = UNDEFINED An 8-bit byte that can take any value depending on the field
			// definition,
			// 9 = SLONG A 32-bit (4-byte) signed integer (2's complement notation),
			// 10 = SRATIONAL Two SLONGs. The first SLONG is the numerator and the second
			// SLONG is the denominator.
			}
			switch (code) {
			case 0x100:
//				System.out.println(""+Integer.toHexString(code)+" "+df+" "+length+" "+value);
//				System.out.println("Width: "+value);
				break;
			case 0x101:
//				System.out.println("Height: "+value);
				break;
			case 0x112:
				exifData.orientation = (int) value;
				/**
				 * 1 = The 0th row is at the visual top of the image, and the 0th column is the
				 * visual left-hand side. 2 = The 0th row is at the visual top of the image, and
				 * the 0th column is the visual right-hand side. 3 = The 0th row is at the
				 * visual bottom of the image, and the 0th column is the visual right-hand side.
				 * 4 = The 0th row is at the visual bottom of the image, and the 0th column is
				 * the visual left-hand side. 5 = The 0th row is the visual left-hand side of
				 * the image, and the 0th column is the visual top. 6 = The 0th row is the
				 * visual right-hand side of the image, and the 0th column is the visual top. 7
				 * = The 0th row is the visual right-hand side of the image, and the 0th column
				 * is the visual bottom. 8 = The 0th row is the visual left-hand side of the
				 * image, and the 0th column is the visual bottom.
				 */
				break;
			case 0xA002:
				exifData.width = (int) value;
				break;
			case 0xA003:
				exifData.height = (int) value;
				break;
			case 0x132:
				try {
					exifData.date = sdf.parse(str);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Date
				break;
			case 0x8769:
				// System.out.println("EXIF data pointer: " + Long.toHexString(value));
				processIfd(bb.duplicate().order(bb.order()), exifDataOffset, (int) value);
				break;

			default:
				// System.out.println("Unhandled: "+Integer.toHexString(code));
				break;
			}
		}
	}

	private static void assertEq(String string, int a, int b) throws IOException {
		if (a != b) {
			throw new IOException(string + Integer.toHexString(a) + " " + Integer.toHexString(b));
		}
	}

	private static void assertTrue(String string, boolean b) throws IOException {
		if (!b) {
			throw new IOException(string);
		}
	}
}
