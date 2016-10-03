package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.TermedDocument;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class WikidataHelper {
	private WikibaseDataFetcher wdf;
	private Gson gson = new Gson();
	
	public WikidataHelper()
	{
		wdf = WikibaseDataFetcher.getWikidataDataFetcher();
	}

	public List<String> translateTitle(String entitle, String lang1, String lang2){
		List<String>ans=new ArrayList<String>();
		String filename = QueryMQL.getMD5Checksum(entitle);

		String cache_path = "/shared/bronte/tac2015/mediawiki_cache/translate/"+lang1+lang2+"/"+filename;
		if (IOUtils.exists(cache_path)) {
			try {
				String line = FileUtils.readFileToString(new File(cache_path), "UTF-8");
				ans = gson.fromJson(line, List.class);
				return ans;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Map<String, EntityDocument> eid = null;
		try {
			wdf = WikibaseDataFetcher.getWikidataDataFetcher();
			eid = wdf.getEntityDocumentsByTitle(lang1+"wiki", entitle);
		} catch (MediaWikiApiErrorException e) {
			e.printStackTrace();
		}

		if(eid != null) {
			for (String e : eid.keySet()) {
				EntityDocument a = eid.get(e);
				ItemDocument aitem = ((ItemDocument) a);
				TermedDocument aterm = (TermedDocument) aitem;
				Map<String, MonolingualTextValue> labels = aitem.getLabels();
				if (labels.size() != 0 && labels.containsKey(lang2)) {
					String frtitle = labels.get(lang2).getText();
					ans.add(frtitle);
				}
			}
		}

		String json = gson.toJson(ans);
		try {
			FileUtils.writeStringToFile(new File(cache_path), json, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ans;
	}

	/**
	 * From lang to en
	 * @param infile
	 * @param outfile
	 * @param lang
     */
	public void printTitleAlignments(String infile, String outfile, String lang){
		try {
			ArrayList<String> lines = LineIO.read(infile);
			BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
//			List<List<String>> trans = lines.stream().map(x -> translateTitle(x, lang, "en")).collect(toList());
			for(int i = 0; i < lines.size(); i++){
				if(i%1000 == 0) System.out.println(i+"/"+lines.size());
				List<String> en_titles = translateTitle(lines.get(i), lang, "en");
				if(en_titles != null && en_titles.size()>0){
					String en = "title_" + en_titles.get(0).replaceAll("\\s+", "_").toLowerCase();
					String orig = "title_" + lines.get(i).replaceAll("\\s+", "_").toLowerCase();
					bw.write(en+" ||| "+orig+"\n");
				}
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException, MediaWikiApiErrorException {
		WikidataHelper e = new WikidataHelper();
		System.out.println(e.translateTitle("Erasme Louis Surlet De Chokier", "en", "es"));
//		String titlefile = "/shared/bronte/ctsai12/multilingual/text/titles.en";
//		String titlefile = "/shared/bronte/ctsai12/multilingual/wikidump/fr_wiki_view/file.list";
//		String outfile = "/shared/bronte/ctsai12/multilingual/text/titles.enfr.align";
//		e.printTitleAlignments(titlefile, outfile, "fr");

//		System.out.println(e.translateTitle("Amurika", "ha", "en"));
//		System.out.println(e.translateTitle("Ore Mountains", "fr"));
//		System.out.println(e.translateTitle("Ore_Mountains", "hr"));
//		System.out.println(e.translateTitle("Happiness", "fr"));
//		System.out.println(e.translateTitle("Devil", "es"));
//		System.out.println(e.translateTitle("Alain Connes", "es"));
//		System.out.println(e.translateTitle("Barack Hussain Obama", "es"));
//		String[] langs={"he","ru","es"};
//		String[] langs={"he"};
//		List<String> titles = LineIO.read("_people_person0__.txt");
//		int count=0;
//		for(String lang:langs)
//		{
//			for (String title:titles)
//			{
//				title=title.replace("_", " ");
////				System.out.println(title+" "+lang);
//				List<String> ans = e.translateTitle(title,lang);
//				boolean flag=false;
//				if(!ans.isEmpty())
//				{
//					count++;
//					if(ans.get(0).split("\\s+").length==title.split("\\s+").length)
//						System.out.println(ans.get(0)+"\t"+title);
//				}
//			}
//		}
//		System.out.println(count);
	}
	
}
