package com.vfnews.factchecker.service;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import smile.classification.DiscreteNaiveBayes;
import smile.nlp.tokenizer.SimpleTokenizer;

import java.io.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MLService {

    private final DatasetEntryRepository repository;
    private final String MODEL_PATH = "model.ser";
    private final String VOCAB_PATH = "vocab.ser";

    private DiscreteNaiveBayes model;
    private Map<String, Integer> vocabulary;
    private String[] labels;

    @PostConstruct
    public void init() {
        loadModel();
    }

    public synchronized void train() {
        List<DatasetEntry> entries = repository.findAll();
        if (entries.size() < 2) {
            System.err.println("Not enough data to train model.");
            return;
        }

        List<String> texts = entries.stream().map(DatasetEntry::getText).toList();
        List<String> labelList = entries.stream().map(DatasetEntry::getLabel).toList();
        
        // Build vocabulary
        Set<String> vocabSet = new HashSet<>();
        SimpleTokenizer tokenizer = new SimpleTokenizer();
        for (String text : texts) {
            vocabSet.addAll(Arrays.asList(tokenizer.split(text.toLowerCase())));
        }
        
        List<String> sortedVocab = vocabSet.stream().sorted().toList();
        vocabulary = new HashMap<>();
        for (int i = 0; i < sortedVocab.size(); i++) {
            vocabulary.put(sortedVocab.get(i), i);
        }

        // Distinct labels
        labels = labelList.stream().distinct().toArray(String[]::new);
        Map<String, Integer> labelMap = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            labelMap.put(labels[i], i);
        }

        // Vectorize (Bag of Words)
        int[][] x = new int[texts.size()][vocabulary.size()];
        int[] y = new int[texts.size()];

        for (int i = 0; i < texts.size(); i++) {
            String[] tokens = tokenizer.split(texts.get(i).toLowerCase());
            for (String token : tokens) {
                if (vocabulary.containsKey(token)) {
                    x[i][vocabulary.get(token)]++;
                }
            }
            y[i] = labelMap.get(labelList.get(i));
        }

        // Train Discrete Naive Bayes (Multinomial)
        model = new DiscreteNaiveBayes(DiscreteNaiveBayes.Model.MULTINOMIAL, labels.length, vocabulary.size());
        model.update(x, y);
        saveModel();
    }

    public Map<String, String> predict(String claim) {
        if (model == null || vocabulary == null) {
            return null;
        }

        SimpleTokenizer tokenizer = new SimpleTokenizer();
        String[] tokens = tokenizer.split(claim.toLowerCase());
        int[] features = new int[vocabulary.size()];
        for (String token : tokens) {
            if (vocabulary.containsKey(token)) {
                features[vocabulary.get(token)]++;
            }
        }

        int prediction = model.predict(features);
        Map<String, String> result = new HashMap<>();
        result.put("text", claim);
        result.put("rating", labels[prediction]);
        result.put("source", "ML");
        return result;
    }

    private void saveModel() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(MODEL_PATH));
             ObjectOutputStream voos = new ObjectOutputStream(new FileOutputStream(VOCAB_PATH))) {
            oos.writeObject(model);
            voos.writeObject(vocabulary);
            voos.writeObject(labels);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadModel() {
        File modelFile = new File(MODEL_PATH);
        File vocabFile = new File(VOCAB_PATH);
        if (modelFile.exists() && vocabFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(MODEL_PATH));
                 ObjectInputStream vois = new ObjectInputStream(new FileInputStream(VOCAB_PATH))) {
                model = (DiscreteNaiveBayes) ois.readObject();
                vocabulary = (Map<String, Integer>) vois.readObject();
                labels = (String[]) vois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
