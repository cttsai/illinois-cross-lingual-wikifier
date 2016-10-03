package edu.illinois.cs.cogcomp.xlwikifier.experiments.reader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import org.apache.commons.io.FileUtils;
import edu.illinois.cs.cogcomp.xlwikifier.Constants;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 8/27/15.
 */
public class MentionReader {
    public MentionReader(){}

    public List<ELMention> readMentionCache(String path){
        Gson gson = new Gson();
        Type t = new TypeToken<ArrayList<ELMention>>(){}.getType();
        String json = null;
        try {
            json = FileUtils.readFileToString(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<ELMention> results = gson.fromJson(json, t);
        return results;
    }

    public List<ELMention> readTrainingMentions(){
        return readGoldMentions(Constants.trainingQueries2015);
    }

    public List<ELMention> readTestMentions(){
        return readGoldMentions(Constants.testingQueries);
    }

    public List<ELMention> readGoldMentions(String filename){
        List<ELMention> ret = new ArrayList<>();
        List<String> lines = null;
        try {
            lines = LineIO.read(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for(String line: lines){
            String[] tokens = line.split("\t");
            String id = tokens[1];
            String mention = tokens[2];
            String[] tmp = tokens[3].split(":");
            String docid = tmp[0];
            String[] offsets = tmp[1].split("-");
            int start = Integer.parseInt(offsets[0]);
            int end = Integer.parseInt(offsets[1])+1;
            String answer = tokens[4];
            String type = tokens[5];
            String lang = docid.split("_")[0];
            String noun_type = tokens[6];

            ELMention m = new ELMention(id, mention, docid);
            m.setLanguage(lang);
            m.setType(type);
            m.setEndOffset(end);
            m.setStartOffset(start);
//            m.setMid(answer);
            m.gold_mid = answer;
            m.setNounType(noun_type);
            ret.add(m);
        }
        return ret;
    }

    public List<ELMention> readTrainingMentionsEng(){
        return readTrainingMentions().stream().filter(x -> x.getLanguage().equals("ENG")).collect(toList());
    }

    public List<ELMention> readEnMentionsNE(boolean train){
        if(train)
            return readTrainingMentionsEng().stream().filter(x -> !x.getType().equals("NOM")).collect(toList());
        else
            return readTestMentions().stream().filter(x -> x.getLanguage().equals("ENG") && !x.getType().equals("NOM")).collect(toList());
    }

    public List<ELMention> readTrainingMentionsEngNAM(){
        return readTrainingMentionsEng().stream().filter(x -> !x.getType().equals("NOM")).collect(toList());
    }

    public List<ELMention> readTestMentionsEngNAM(){
        return readTestMentions().stream().filter(x -> x.getLanguage().equals("ENG") && !x.getType().equals("NOM")).collect(toList());
    }

    public List<ELMention> readTrainingMentionsSpa(){
        return readTrainingMentions().stream().filter(x -> x.getLanguage().equals("SPA")).collect(toList());
    }
    public List<ELMention> readTrainingMentionsCmn(){
        return readTrainingMentions().stream().filter(x -> x.getLanguage().equals("CMN")).collect(toList());
    }

    public List<ELMention> readTestMentionsSpa(){
        return readTestMentions().stream().filter(x -> x.getLanguage().equals("SPA")).collect(toList());
    }

    public List<ELMention> readESMentions(boolean train){
        if(train)
            return readTrainingMentions().stream().filter(x -> x.getLanguage().equals("SPA")).collect(toList());
        else
            return readTestMentions().stream().filter(x -> x.getLanguage().equals("SPA")).collect(toList());
    }

    public List<ELMention> readTestMentionsCmn(){
        return readTestMentions().stream().filter(x -> x.getLanguage().equals("CMN")).collect(toList());
    }

    public List<ELMention> readZHMentions(boolean train){
        if(train)
            return readTrainingMentions().stream().filter(x -> x.getLanguage().equals("CMN")).collect(toList());
        else
            return readTestMentions().stream().filter(x -> x.getLanguage().equals("CMN")).collect(toList());
    }


    /**
     * Return most frequent FreeBase types for each entity type
     * @throws Exception
     */
    public Map<String, List<String>> getTopTrainTypes(int top) throws Exception {
        if(!FreeBaseQuery.isloaded())
            FreeBaseQuery.loadDB(true);
        List<ELMention> mentions = readTrainingMentions();
        Map<String, List<ELMention>> id2ms = mentions.stream().filter(x -> !x.getGoldMid().startsWith("NIL"))
                .collect(groupingBy(x -> x.getGoldMid(), toList()));
        Set<String> spa_ids = mentions.stream().filter(x -> x.getLanguage().equals("SPA"))
                .map(x -> x.getGoldMid()).collect(toSet());

//        QueryMQL qm = new QueryMQL();
        Map<String, List<String>> type2fbt = new HashMap<>();
        List<String> tmp = new ArrayList<>();
        for(String id: id2ms.keySet()) {
            if(spa_ids.contains(id)) continue;
            Set<String> types = id2ms.get(id).stream().map(x -> x.getType()).collect(toSet());
            if (types.size() > 1) continue;
            String type = (String) types.toArray()[0];
//            List<String> fbtypes = qm.lookupTypeFromMid(id);
            List<String> fbtypes = FreeBaseQuery.getTypesFromMid(id);
            fbtypes = fbtypes.stream().filter(x -> !x.startsWith("base.")).collect(Collectors.toList());

            if (!type2fbt.containsKey(type)) {
                type2fbt.put(type, new ArrayList<>());
            }
            type2fbt.get(type).addAll(fbtypes);
            tmp.add(type);
        }

        Map<String, Long> c = tmp.stream().collect(groupingBy(x -> x, counting()));
//        System.out.println(c);

        Map<String, List<String>> ret = new HashMap<>();
        for(String type: type2fbt.keySet()){
            List<Map.Entry<String, Long>> tcnt = type2fbt.get(type).stream()
                    .collect(groupingBy(x -> x, counting()))
                    .entrySet().stream()
                    .sorted((d1, d2) -> Long.compare(d2.getValue(), d1.getValue()))
                    .collect(toList());
            System.out.println(type);
            tcnt.subList(1, top).forEach(x -> System.out.println("\t"+x.getKey()+"\t"+x.getValue()));
            List<String> toptypes = tcnt.subList(1, top).stream().map(x -> x.getKey()).collect(toList());
            ret.put(type, toptypes);
//            String types = tcnt.subList(1,10).stream().map(x -> "type:"+x.getKey()).collect(joining(" "));
//            ret.put(type, types);
        }
        return ret;
    }


    public static void main(String[] args) throws Exception {

        MentionReader mr = new MentionReader();
        mr.readTrainingMentionsEng();

    }
}
