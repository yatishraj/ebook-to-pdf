import java.util.ArrayList;
import java.util.List;

import app.ParagraphBuilder;
import app.ParagraphListener;

public class ParagraphBuilderTest implements ParagraphListener {

	public static List<String> paragragraphs = new ArrayList<String>();
	public static final String[] validationCheck = new String[] { 
		"aaa bbb ccc... ddd.",
		"eee eee, fff!",
		"aaa...bbb. ccc, ddd!", 
		"- eee fff?",
		"aaa... bbb. ccc, ddd. eee fff?",
		"ggg hhh!" 
	};

	public static void main(String[] args) {
		ParagraphBuilderTest listener = new ParagraphBuilderTest();
		ParagraphBuilder builder = new ParagraphBuilder(2, 3, listener);
		
		builder.addText("aaa bbb");
		builder.addText("ccc...\r\nddd. eee");
		builder.addText("eee    ,fff!\n");
		builder.addText("aaa...bbb. \n\nccc, \nddd! - eee fff?");
		builder.addText("aaa...    bbb.  ccc\r,     ddd\n. eee fff? ggg hhh!");
		builder.finishParagraph();

		int i = 0;
		for (String line : validationCheck) {
			if (0 != line.compareTo(paragragraphs.get(i))) {
				System.out.println(line);
				System.out.println(paragragraphs.get(i));
				System.out.println("ERROR");
				return;
			}
			i++;
		}
		System.out.println("OK");
	}

	public String lastAddedParagraph;
	static int p = 0;
	public void addParagraph(String paragraph) {
		lastAddedParagraph = paragraph;
		paragragraphs.add(paragraph);
		System.out.println("paragraph " + (++p) + ": " + lastAddedParagraph);
	}
}
