package app;

public class ParagraphBuilder {

	char ppc = 0;
	char pc = ' ';

	int singleSentences;
	int currentSenteces = 1;

	final int sentecesPerParagraph;
	final ParagraphListener listener;
	final StringBuilder builder = new StringBuilder(4096);

	public ParagraphBuilder(int skipSentences, int sentecesPerParagraph, ParagraphListener listener) {
		this.singleSentences = skipSentences;
		this.sentecesPerParagraph = sentecesPerParagraph;
		this.listener = listener;
	}

	public void addText(String text) {
		text = " " + text;
		for (int i = 0, l = text.length(); i < l; i++) {
			char c = text.charAt(i);
			if (c < ' ') {
				c = ' ';
			}
			if (pc == ' ' && c == ' ') {
				// do trim
				continue;
			}
			if (c == ',') {
				if (pc == ' ' && builder.length() > 0) {
					builder.setLength(builder.length() - 1);
				}
				builder.append(", ");
				pc = ' ';
				continue;
			}

			if (c == '.') {
				char nc = (i + 1 >= l) ? 0 : text.charAt(i + 1);
				if (nc != '.') {
					if (pc == ' ' && builder.length() > 0) {
						builder.setLength(builder.length() - 1);
					}
					builder.append(c);
					finishSentence();
					ppc = c;
					pc = ' ';
					continue;
				} else {
					// add ellipsis ... to the sentence
					while (nc == '.' && i < l) {
						builder.append(c);
						i++;
						if (i < l) {
							nc = text.charAt(i);
						}
					}
					if (i < l) {
						i--;
					}
					continue;
				}
			} else if (c == '!') {
				if (pc == ' ' && builder.length() > 0) {
					builder.setLength(builder.length() - 1);
				}
				builder.append(c);
				finishSentence();
				ppc = c;
				pc = ' ';
				continue;
			} else if (c == '?') {
				if (pc == ' ' && builder.length() > 0) {
					builder.setLength(builder.length() - 1);
				}
				builder.append(c);
				finishSentence();
				ppc = c;
				pc = ' ';
				continue;
			}

			// check if new line is starting with - as a speech quote
			if (c == '-' && (ppc == '.' || ppc == '?' || ppc == '!')){
				currentSenteces = sentecesPerParagraph;
				finishSentence();
				builder.append("- ");
				ppc = '-';
				pc = ' ';
				currentSenteces = sentecesPerParagraph;
				continue;
			}

			builder.append(c);
			ppc = pc;
			pc = c;
		}
	}

	private void finishSentence() {
		if (singleSentences > 0) {
			finishParagraph();
			singleSentences--;
		} else if (currentSenteces >= sentecesPerParagraph) {
			finishParagraph();
			currentSenteces = 1;
		} else {
			currentSenteces++;
			pc = ' ';
			builder.append(' ');
		}
	}

	public void finishParagraph() {
		String p = builder.toString().trim();
		if (p.length() > 0) {
			try {
				listener.addParagraph(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		builder.setLength(0);
	}
}
