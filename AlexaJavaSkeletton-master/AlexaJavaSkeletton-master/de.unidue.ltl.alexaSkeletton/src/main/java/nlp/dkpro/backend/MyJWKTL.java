package nlp.dkpro.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.springframework.util.ResourceUtils;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEntry;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryPage;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryRelation;
import de.tudarmstadt.ukp.jwktl.api.RelationType;

public class MyJWKTL {
	
	public static ArrayList<String> getSynonym(String word) throws FileNotFoundException, XMLStreamException, IOException {
		 File wiktionaryDirectory = ResourceUtils.getFile("classpath:jwktl");
		 IWiktionaryEdition wkt = JWKTL.openEdition(wiktionaryDirectory);
		 
		 ArrayList<String> result = new ArrayList<String>();

		 IWiktionaryPage page = wkt.getPageForWord(word);
		 for (IWiktionaryEntry entry : page.getEntries()) {
			 for (IWiktionaryRelation relation : entry.getRelations(RelationType.SYNONYM))
			      result.add(relation.getTarget());
		 }
		     
		wkt.close();
	
		return result;
	}
}