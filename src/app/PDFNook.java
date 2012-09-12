package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.RectangleReadOnly;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

public class PDFNook implements ParagraphListener {

	private static final String UTF8 = "UTF8";
	private static final String TYPE_HTML = "HTML";
	private static final String TYPE_EPUB = "EPUB";
	private static final String TYPE_TXT = "TXT";
	private static final String TYPE_PDF = "PDF";

	private static final String CREATOR = "iText + Java";
	private static final String SPACE = " ";
	private static final String VERDANA_TTF = "./verdana.ttf";

	private static Font fontTitle;
	private static Font fontAutor;
	private static Font fontText;

	public static void addTitle(Document document, String title) throws DocumentException {
		Paragraph paragraph = new Paragraph();
		paragraph.add(new Chunk(title, fontTitle));
		document.add(paragraph);
	}

	public static void addAutor(Document document, String autor) throws DocumentException {
		Paragraph paragraph = new Paragraph();
		paragraph.add(new Chunk(autor, fontAutor));
		document.add(paragraph);
		document.add(new Paragraph(SPACE));
	}

	public static void addParagraph(Document document, String text) throws DocumentException {
		Paragraph paragraph = new Paragraph();
		paragraph.add(new Chunk(text, fontText));
		document.add(paragraph);
		document.add(new Paragraph(SPACE));
	}

	public static void addText(Document document, String text) throws DocumentException {
		Paragraph paragraph = new Paragraph();
		paragraph.add(new Chunk(text, fontText));
		document.add(paragraph);
	}

	public static void addImage(Document document, String imageFileName) throws DocumentException {
		Image image;
		try {
			image = Image.getInstance(imageFileName);
			document.add(image);
			document.add(new Paragraph(SPACE));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void nookPdf(File inFile, String fileType, File outFile, String author, String title,
			File pictureFile, int fontSize)
			throws Exception {
		if (outFile.exists()) {
			outFile.delete();
		}

		fontTitle = FontFactory.getFont(VERDANA_TTF, BaseFont.IDENTITY_H, true, fontSize + 2, Font.BOLD);
		fontAutor = FontFactory.getFont(VERDANA_TTF, BaseFont.IDENTITY_H, true, fontSize + 2, Font.ITALIC);
		fontText = FontFactory.getFont(VERDANA_TTF, BaseFont.IDENTITY_H, true, fontSize, Font.NORMAL);

		Rectangle pageSize = new RectangleReadOnly(300, 526);// 300x460
		Document document = new Document(pageSize, 5, 5, 5, 5);
		PdfWriter.getInstance(document, new FileOutputStream(outFile));
		document.open();

		if (title != null) document.addTitle(title);
		if (author != null) document.addAuthor(author);
		document.addCreator(CREATOR);

		if (title != null) addTitle(document, title);
		if (author != null) addAutor(document, author);

		if (pictureFile != null) {
			addImage(document, pictureFile.getCanonicalPath());
		}

		if (TYPE_TXT.equals(fileType)) {
			processTXTFile(inFile, document);

		} else if (TYPE_HTML.equals(fileType)) {
			FileInputStream fstream = new FileInputStream(inFile);
			InputStreamReader reader = new InputStreamReader(fstream, UTF8);
			processHTMLFile(reader, document);
			reader.close();

		} else if (TYPE_EPUB.equals(fileType)) {
			processEPUBFile(inFile, document);

		} else if (TYPE_PDF.equals(fileType)) {
			processPDFFile(inFile, document);
		}

		document.close();
	}

	private static void processTXTFile(File inFile, Document document) throws Exception {
		FileInputStream fstream = new FileInputStream(inFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream, UTF8));
		String strLine;
		boolean lastWasEmpty = false;
		while ((strLine = br.readLine()) != null) {
			String line = strLine.trim();
			int l = line.length();
			if (l > 0 || lastWasEmpty) {
				addParagraph(document, line);
			}
			lastWasEmpty = l <= 0;
		}
		br.close();
	}

	private static void processHTMLFile(Reader reader, Document document) throws Exception {
		StringBuilder line = new StringBuilder(1024);
		char pppppc = 0, ppppc = 0, pppc = 0;
		char ppc = 0, pc = 0, c = 0;// used for "<br" "</p" ... recognition
		int i;
		all: while (true) {
			i = reader.read();
			if (i <= -1) {
				break;
			}
			ppc = pc;
			pc = c;
			c = (char) i;
			if (c == '<') {
				// skip HTML tag
				while (true) {
					i = reader.read();
					if (i <= -1) {
						break all;
					}
					ppc = pc;
					pc = c;
					c = (char) i;
					if (c == '>') {
						// we reach the end of tag
						break;
					} else {
						// check for <BR
						boolean addLine =
						// BR
						(ppc == '<' && (pc == 'B' || pc == 'b') && (c == 'R' || c == 'r')) ||
						// P
								(ppc == '<' && pc == '/' && (c == 'P' || c == 'p')) ||
								// H1 H2 H3
								(ppc == '<' && pc == '/' && (c == 'H' || c == 'h'));

						if (addLine) {
							String l = line.toString().trim();
							if (l.length() > 0) {
								addParagraph(document, line.toString().trim());
							}
							line.setLength(0);
						}
					}
				}
			} else if (c <= ' ') {
				int l = line.length();
				if (l > 0 && line.charAt(l - 1) != ' ') {
					line.append(' ');
				}
			} else if (c == '&') {
				// skip hex escape
				while (true) {
					i = reader.read();
					if (i <= -1) {
						break all;
					}
					pppppc = ppppc;
					ppppc = pppc;
					pppc = ppc;
					ppc = pc;
					pc = c;
					c = (char) i;
					if (c == ';') {
						// < &lt;
						if (ppc == 'l' && pc == 't') {
							line.append('<');
						} else
						// > &gt;
						if (ppc == 'g' && pc == 't') {
							line.append('>');
						} else
						// & &amp;
						if (pppc == 'a' && ppc == 'm' && pc == 'p') {
							line.append('<');
						} else
						// ' &apos;
						if (ppppc == 'a' && pppc == 'p' && ppc == 'o' && pc == 's') {
							line.append('\'');
						} else
						// " &quot;
						if (ppppc == 'q' && pppc == 'u' && ppc == 'o' && pc == 't') {
							line.append('"');
						} else
						// HEX
						if (ppc == 'x') {
							int x = (char) (toHex(pc));
							line.append((char) x);
						} else if (pppc == 'x') {
							int x = (char) (toHex(pc) + (toHex(ppc) << 4));
							line.append((char) x);
						} else if (ppppc == 'x') {
							int x = (char) (toHex(pc) + (toHex(ppc) << 4) + (toHex(pppc) << 8));
							line.append((char) x);
						} else if (pppppc == 'x') {
							int x = (char) (toHex(pc) + (toHex(ppc) << 4) + (toHex(pppc) << 8) + (toHex(ppppc) << 12));
							line.append((char) x);
						} else
						// DECIMAL
						if (ppc == '#') {
							int x = (toHex(pc));
							line.append((char) x);
						} else if (pppc == '#') {
							int x = (10 * toHex(ppc) + toHex(pc));
							line.append((char) x);
						} else if (ppppc == '#') {
							int x = (10 * (10 * toHex(pppc) + toHex(ppc)) + toHex(pc));
							line.append((char) x);
						} else if (pppppc == '#') {
							int x = (10 * (10 * (10 * toHex(ppppc) + toHex(pppc)) + toHex(ppc)) + toHex(pc));
							line.append((char) x);
						}
						break;
					}
				}
			} else {
				line.append(c);
			}
		}
		if (line.length() > 0) {
			addParagraph(document, line.toString());
		}
	}

	private static final int toHex(int c) {
		if ((c & 0xF0) == 0x30) {
			return c & 0x0F;
		} else {
			return 0x10 + ((c & 0xF0) - 1);
		}
	}

	private static void processEPUBFile(File inFile, Document document) throws Exception {
		ZipInputStream zin = new ZipInputStream(new FileInputStream(inFile));
		ZipEntry entry;
		while ((entry = zin.getNextEntry()) != null) {
			String entryName = entry.getName().toUpperCase();
			if (entryName.endsWith(".XHTML")) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(zin));
				processHTMLFile(reader, document);
			}
			zin.closeEntry();
		}
		zin.close();
	}

	private static void processPDFFile(File inFile, Document document) throws Exception {
		PdfReader reader = new PdfReader(inFile.getAbsolutePath());
		PdfReaderContentParser parser = new PdfReaderContentParser(reader);
		ParagraphBuilder builder = new ParagraphBuilder(2, 5, new PDFNook(document));

		TextExtractionStrategy strategy;
		for (int i = 1, l = reader.getNumberOfPages(); i <= l; i++) {
			strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
			String str = strategy.getResultantText();
			builder.addText(str);
		}
		builder.finishParagraph();
	}

	public static void help() {
		System.out.println("Usage:");
		System.out.println("      -a[uthor] author");
		System.out.println("      -t[itle] title");
		System.out.println("      -p[icture] file");
		System.out.println("      -s[ize] of font");
		System.out.println("      -i[nput] file to process (txt, html, epub, pdf)");
		System.out.println("      -o[utput] file (pdf)");
	}

	public static void main(String[] args) throws Exception {
		try {
			InputStream is = PDFNook.class.getResourceAsStream("verdana.ttf");
			File outFile = new File(VERDANA_TTF);
			if (is != null) {
				if (!outFile.exists()) {
					OutputStream out = new FileOutputStream(outFile);
					byte[] buf = new byte[4096];
					int len;
					while ((len = is.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					out.flush();
					out.close();
					is.close();
				}
			}
		} catch (Exception e) {
			System.out.println("ERROR: Cannot create verdana.ttf!");
			e.printStackTrace();
			System.exit(100);
		}

		if (args.length <= 4 || "-?".equals(args[0]) || "-h".equals(args[0])) {
			help();
		} else {

			String author = null;
			String title = null;
			String pictureFileName = null;
			String inputFile = null;
			String outputFile = null;
			int fontSize = 10;

			for (int i = 0, l = args.length - 1; i < l; i++) {
				final String arg = args[i].toLowerCase();
				if (arg.startsWith("-a")) {
					author = args[i + 1];
				} else if (arg.startsWith("-t")) {
					title = args[i + 1];
				} else if (arg.startsWith("-p")) {
					pictureFileName = args[i + 1];
				} else if (arg.startsWith("-i")) {
					inputFile = args[i + 1];
				} else if (arg.startsWith("-o")) {
					outputFile = args[i + 1];
				} else if (arg.startsWith("-s")) {
					try {
						fontSize = Integer.parseInt(args[i + 1], 10);
					} catch (Exception e) {
					}
				}
			}
			if (outputFile == null) {
				System.err.println("ERROR: No output file!");
				System.exit(1);
			}
			if (inputFile == null) {
				System.err.println("ERROR: No file to process!");
				System.exit(2);
			}

			File pictureFile = null;
			if (pictureFileName != null) {
				File f = new File(pictureFileName);
				if (f.exists()) {
					pictureFile = f;
				}
			}

			File outFile;
			if (outputFile != null) {
				if (!outputFile.toUpperCase().endsWith(".PDF")) {
					outputFile = outputFile + ".pdf";
				}
				outFile = new File(outputFile);
			} else {
				outFile = null;
			}

			File inFile = new File(inputFile);
			if (inFile.exists()) {

				String fileType = null;
				String filename = inFile.getName().toUpperCase();
				if (filename.endsWith(".TXT")) {
					fileType = TYPE_TXT;
				} else if (filename.endsWith(".EPUB")) {
					fileType = TYPE_EPUB;
				} else if (filename.endsWith(".PDF")) {
					fileType = TYPE_PDF;
				} else if (filename.endsWith(".HTML") || filename.endsWith(".HTM") || filename.endsWith(".XHTML")) {
					fileType = TYPE_HTML;
				} else {
					System.err.println("ERROR: Only txt, html, or epub files are supported!");
				}

				nookPdf(inFile, fileType, outFile, author, title, pictureFile, fontSize);
			} else {
				System.err.println("ERROR: Input file does not exists!");
			}
		}
	}

	public PDFNook(Document document) {
		this.document = document;
	}

	final Document document;

	public void addParagraph(String paragraph) throws Exception {
		if (document != null) {
			addParagraph(document, paragraph);
		}
	}
}
