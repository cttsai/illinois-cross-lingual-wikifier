package edu.illinois.cs.cogcomp.xlwikifier.freebase;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.*;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


/**
 * Takes a string (e.g., "obama") and asks the Freebase Search API. Caches if
 * necessary. Outputs a set of entities.
 *
 * @author Percy Liang
 * @modified upadhya3
 */
public class FreebaseSearch {
	static int counter = 0;
	private String apikey;
	private String cacheLocation;
	private String spanishCacheLocation;
	private String rdfCacheLocation;
	static final String defaultCacheLocation = "/shared/bronte/tac2015/freebase_cache/";
	static final String defaultSpanishCacheLocation = "/shared/bronte/tac2014/data/freebaseRawResponseCache/SearchResponseSpanish";
//	static final String defaultRdfCacheLocation = "/shared/bronte/tac2014/data/freebaseRawResponseCache/RdfResponse";
	static final String defaultRdfCacheLocation = "/shared/bronte/tac2015/freebase_cache/rdf2";

//	static final String defaultApikey = "AIzaSyAclVmmn2FbIc6PiN9poGfNTt2CcyU6x48"; // has
																					// 10k
																					// queries
																					// per
																					// day
																					// limit
																					// (Shyam
	static final String defaultApikey = "AIzaSyAclVmmn2FbIc6PiN9poGfNTt2CcyU6x48"; //"AIzaSyD4X-Y5JK4ONiCrxp_rbyo54VgKFFXcon0"; //"AIzaSyAZUH3NLToPTHWNm4B47zAJiEPr4jnfSy0"; //"AIzaSyAOYVjHDzk4VNFqz9oWw1qht02vCyIqq5s";//  //;
//	 static final String defaultApikey =
//	 "AIzaSyD4X-Y5JK4ONiCrxp_rbyo54VgKFFXcon0"; // has 10k queries per day
	// limit (Chen-Tse)

	private static final Logger logger = LoggerFactory
			.getLogger(FreebaseSearch.class);

	private Gson gson = new Gson();

	public FreebaseSearch() {
		this.apikey = defaultApikey;
		this.cacheLocation = defaultCacheLocation;
		this.spanishCacheLocation = defaultSpanishCacheLocation;
		this.rdfCacheLocation = defaultRdfCacheLocation;

	}

	public FreebaseSearch(String cacheLocation, String key) {
		this.apikey = key;
		this.cacheLocation = cacheLocation;
	}

	public FreebaseSearch(String cacheLocation) {
		this(cacheLocation, defaultApikey);
	}

	public List<SearchResult> lookup(String query, String lang, String filter){
		String checksum = QueryMQL.getMD5Checksum(query+filter);
		String cachefile = cacheLocation+"search_"+ lang + "/" + checksum + ".cached";
		String json = null;
		List<SearchResult> ret = new ArrayList<>();
		try {
			if (IOUtils.exists(cachefile)) {
//			System.out.println("Found in "+cachefile);
				json = FileUtils.readFileToString(new File(cachefile), "UTF-8");
			} else {
				System.out.println("Caching");
				json = QueryFreeBase(query, lang, filter);
				FileUtils.writeStringToFile(new File(cachefile), json, "UTF-8");
			}
			if (json == null || json.isEmpty()) {
				System.out.println(query);
				System.out.println(checksum);
				return ret;
//				System.exit(-1);
			}
			json = json.trim();
			if(json.endsWith("}}")) json = json.substring(0, json.length()-1);
			JSONParser parser = new JSONParser();
			JSONObject response = (JSONObject) parser.parse(json);
			JSONArray results = (JSONArray) response.get("result");
			for (Object result : results) {
				SearchResult r = gson.fromJson(result.toString(), SearchResult.class);
				ret.add(r);
			}
		}catch (Exception e){
			System.out.println(cachefile);
			e.printStackTrace();
		}
		return ret;
	}

	@Deprecated
	public List<FreebaseAnswer> lookup(String query) throws Exception {
		// First, try the cache.
		String checksum = QueryMQL.getMD5Checksum(query);
		if (IOUtils.exists(cacheLocation + "/" + checksum + ".cached")) {
//			System.out.println("Found!");
			return parseJson(FileUtils.readFileToString(new File(cacheLocation
					+ "/" + checksum + ".cached"), "UTF-8"));
		} else {
			System.out.println("Caching");
			String tmp = getQueryResponse(query);
			FileUtils.writeStringToFile(new File(cacheLocation + "/" + checksum
					+ ".cached"), tmp, "UTF-8");
			return parseJson(tmp);

		}
	}

	/**
	 * Query Freebase by ID
	 * @param query: in the form of m/09c7w0
	 * @return
	 * @throws Exception
	 */
	public List<String> lookupRdf(String query){
		// First, try the cache.
		String checksum = null;
		try {
			checksum = QueryMQL.getMD5Checksum(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String filename = rdfCacheLocation + "/" + checksum + ".cached";
		if (IOUtils.exists(filename)) {
//			System.out.println("Found!");
			try {
				return LineIO.read(filename);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Caching");
			List<String> tmp = queryByID(query);
			try {
				FileUtils.writeStringToFile(new File(filename), tmp.stream().collect(joining("\n")), "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return tmp;
		}
		return null;
	}

	private List<FreebaseAnswer> parseJson(String ans) {
		if (ans == null)
			return null;
		List<FreebaseAnswer> output = new ArrayList<>();
		JsonElement parse = new JsonParser().parse(ans);
		JsonObject asJsonObject = parse.getAsJsonObject();
		JsonArray jarray = asJsonObject.getAsJsonArray("result");
		for (JsonElement js : jarray) {
			output.add(new FreebaseAnswer(js));
		}
		return output;
	}

	private List<String> queryByID(String query) {
		counter++;
		logger.info("NOT IN FREEBASE CACHE, QUERYING ... " + counter + " times");
		String url = null;
		try {
			url = String.format("https://www.googleapis.com/freebase/v1/rdf/" + query + "?key=" + apikey, URLEncoder.encode(query, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println("QUERY URL: " + url);
		URLConnection conn = null;
		InputStream in;
		try {
			conn = new URL(url).openConnection();
			in = conn.getInputStream();
		} catch (IOException e) {
			return new ArrayList<>();
		}

		// Read the response
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		List<String> ret = new ArrayList<>();
		String line;
		try {
			while ((line = reader.readLine()) != null)
				ret.add(line.trim());
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	private String QueryFreeBase(String query, String lang, String filter){
		counter++;
		logger.info("NOT IN FREEBASE CACHE, QUERYING ... " + counter + " times");
		HttpTransport httpTransport = new NetHttpTransport();
		HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
		GenericUrl url = new GenericUrl("https://www.googleapis.com/freebase/v1/search");
		url.put("query", query);
		if(filter.isEmpty())
			url.put("filter", null);
		else
			url.put("filter", filter);
		url.put("limit", "20");
		url.put("indent", "true");
		url.put("lang", lang);
		url.put("key", apikey);
		HttpRequest request = null;
		try {
			request = requestFactory.buildGetRequest(url);
			HttpResponse httpResponse = request.execute();
			return httpResponse.parseAsString();
		} catch (IOException e) {
			System.exit(-1);
			e.printStackTrace();
		}
		return null;
	}

	private String getQueryResponse(String query) throws IOException {
		counter++;
		logger.info("NOT IN FREEBASE CACHE, QUERYING ... " + counter + " times");
		String url = String.format(
				"https://www.googleapis.com/freebase/v1/search?query=%s&key="
						+ apikey, URLEncoder.encode(query, "UTF-8"));
		System.out.println("QUERY URL: " + url);
		URLConnection conn = new URL(url).openConnection();
		InputStream in = conn.getInputStream();

		// Read the response
		StringBuilder buf = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null)
			buf.append(line);
		reader.close();
		// logger.info(buf.toString());
		return buf.toString();
	}

	// /en/barack_obama => fb:en.barack_obama, FOR LATER USE
	private String toRDF(String s) {
		if (s == null)
			return s;
		return "fb:" + s.substring(1).replaceAll("/", ".");
	}

	public String[] query(String q) throws IOException {
		System.out.println("in query:" + q);
		List<String> ans = new ArrayList<String>();
		try {
			List<FreebaseAnswer> tmp = lookup(q);
			for (FreebaseAnswer t : tmp) {
				ans.add(t.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ans.toArray(new String[ans.size()]);
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	public String getDescription(String mid, String lang){
		if(!mid.startsWith("/")){
			System.out.println("mid format error, "+mid);
			System.exit(-1);
		}
		mid = mid.substring(1);
		List<String> r = lookupRdf(mid);
		if(r == null || r.size() == 0)
			return null;

		Optional<String> tmp = r.stream().filter(x -> x.contains("topic.description") && x.contains("@" + lang))
				.findFirst();

		if(!tmp.isPresent()) return null;

		String[] tokens = tmp.get().split("\"");
		String des = Arrays.asList(tokens).subList(1, tokens.length-1).stream().collect(joining("\""));
		try {
			return new String(des.getBytes(), "UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return des;
	}

	public String replaceUnicodeAccent(String in){
		return in.replaceAll("\\\\xe9", "é")
				.replaceAll("\\\\xfa", "ú")
				.replaceAll("\\\\xed", "i")
				.replaceAll("\\\\xf1", "n");

	}

	public static void main(String[] args) throws Exception {
		FreebaseSearch fb = new FreebaseSearch();
		String aa = "América";


//		String de = fb.replaceUnicodeAccent(fb.getDescription("/m/09c7w0", "es"));

//		String de = fb.getDescription("/m/09c7w0","en").replaceAll(",","").replaceAll("\\.", "").replaceAll("'s", "").replaceAll("\n", " ");
//		List<String> filtered = Arrays.asList(de.split("\\s+")).stream()
//				.filter(x -> !we.en_words.containsKey(x.toLowerCase()))
//				.collect(toList());

//		System.out.println(filtered);




		List<SearchResult> answers = fb.lookup("巴拉克·奥巴马", "zh-tw", "");
////		List<SearchResult> answers = fb.lookup("ISIS", "en", "(any type:/people/person type:/organization/organization)");
		System.out.println(answers.stream().map(x -> x.getName()).collect(toList()));
//		System.out.println(answers.stream().map(x -> x.getMid()).collect(toList()));
//		System.out.println(answers.stream().map(x -> x.getScore()).collect(toList()));
//		List<FreebaseAnswer> answers = fb.lookupSpanish("Al Qaida");
//		String mid = answers.get(0).getMid().substring(1);
//		System.out.println(answers.stream().map(x -> x.getMid()).collect(joining(",")));
//		System.out.println(answers.stream().map(x -> x.getName()).collect(joining(",")));
//		System.out.println(answers.stream().map(x -> x.getScore()).collect(toList()));
	}
}
