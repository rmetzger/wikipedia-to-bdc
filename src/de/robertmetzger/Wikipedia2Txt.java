package de.robertmetzger;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import org.xml.sax.SAXException;


/**
 * Code from: http://trulymadlywordly.blogspot.mx/2011/03/creating-text-corpus-from-wikipedia.html
 *
 */
public class Wikipedia2Txt {
	static String dumpfile = "";
	static String target = "";
	static PrintWriter printWriter;
	static long id = 0;
	
	public static void main(String[] args) throws IOException, SAXException {
		if(args.length < 2) {
			System.err.println("Usage: <wikipedia dump> <textfile>");
			System.exit(1);
		}
		dumpfile = args[0];
		target = args[1];
		
		FileWriter writer = new FileWriter(target);
        printWriter = new PrintWriter(writer);

		IArticleFilter handler = new ArticleFilter();
        WikiXMLParser wxp = new WikiXMLParser(dumpfile, handler);
        wxp.parse();
	}
	
    /**
     * Print title an content of all the wiki pages in the dump.
     * 
     */
	static class ArticleFilter implements IArticleFilter {

		final static Pattern regex = Pattern.compile("[A-Z][\\p{L}\\w\\p{Blank},\\\"\\';\\[\\]\\(\\)-]+[\\.!]", 
				Pattern.CANON_EQ);
		
		// Convert to plain text
		WikiModel wikiModel = new WikiModel("${image}", "${title}");

		public void process(WikiArticle page, Siteinfo siteinfo) throws SAXException {
			if (page != null && page.getText() != null && !page.getText().startsWith("#REDIRECT ")){
				printWriter.write((id++)+","); // write document id
				String wikiText = page.getText().
									replaceAll("[=]+[A-Za-z+\\s-]+[=]+", " ").
									replaceAll("\\{\\{[A-Za-z0-9+\\s-]+\\}\\}"," ").
									replaceAll("(?m)<ref>.+</ref>"," ").
									replaceAll("(?m)<ref name=\"[A-Za-z0-9\\s-]+\">.+</ref>"," ").
									replaceAll(","," ").
									replaceAll("\n"," "). // remove newline
									replaceAll("<ref>"," <ref>");

				// Remove text inside {{ }}
				String plainStr = wikiModel.render(new PlainTextConverter(), wikiText).
					replaceAll("\\{\\{[A-Za-z+\\s-]+\\}\\}"," ");

				Matcher regexMatcher = regex.matcher(plainStr);
				while (regexMatcher.find()) {
					// Get sentences with 6 or more words
					String sentence = regexMatcher.group();
					if (matchSpaces(sentence, 5)) {
						printWriter.print(sentence);
					}
				}
				printWriter.println();
			}
		}

		private boolean matchSpaces(String sentence, int matches) {
			int c =0;
			for (int i=0; i< sentence.length(); i++) {
				if (sentence.charAt(i) == ' ') c++;
				if (c == matches) return true;
			}
			return false;
		}
		
		public static String humanReadableByteCount(long bytes, boolean si) {
		    int unit = si ? 1000 : 1024;
		    if (bytes < unit) return bytes + " B";
		    int exp = (int) (Math.log(bytes) / Math.log(unit));
		    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
		}
	}
}
