package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.Morphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.exception.LemmatizerException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Lemmatizer {
    private final LuceneMorphology morphology;
    private static final Logger logger = LoggerFactory.getLogger(Lemmatizer.class);

    public Lemmatizer(){
        try {
            this.morphology = new RussianLuceneMorphology();
        }catch(IOException e){
            logger.error(e.getLocalizedMessage());
            LemmatizerException exception = new LemmatizerException();
            exception.message = "Не удалось инициализировать лемматизатор!";
            throw exception;
        }
    }

    public boolean isServicePart(String morphInfo){
        String[] parts = morphInfo.split("\\|");
        if(parts.length < 2) return false;
        String part = parts[1].split(" ")[0];
        return part.equals("СОЮЗ") || part.equals("МЕЖД") || part.equals("ПРЕДЛ");
    }

    public Map<String, Integer> lemmatizeText(String text){
        Map<String, Integer> lemmaCounts = new HashMap<>();
        String lowerCaseText = text.toLowerCase();
        String[] words = lowerCaseText.split("\\s+");
        for(String word : words){
            //if(word.isEmpty()) continue;
            if(!word.matches("[а-яё]+")) continue;
            List<String> morphInfo = morphology.getMorphInfo(word);
            for(String info : morphInfo){
                if(isServicePart(info)){
                    continue;
                }else{
                    List<String> lemmas = morphology.getNormalForms(word);
                    for(String lemma : lemmas){
                        lemmaCounts.put(lemma, lemmaCounts.getOrDefault(lemma, 0) + 1);
                    }
                }
            }
        }
        return lemmaCounts;
    }

    public String cleanHtml(String html){
        Document doc = Jsoup.parse(html);
        return doc.text();
    }
}
