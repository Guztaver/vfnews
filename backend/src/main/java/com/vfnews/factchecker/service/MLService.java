package com.vfnews.factchecker.service;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import smile.classification.DiscreteNaiveBayes;
import smile.nlp.tokenizer.SimpleTokenizer;

@Service
@RequiredArgsConstructor
public class MLService {

    private final DatasetEntryRepository repository;
    private final String MODEL_PATH = "model.ser";
    private final String VOCAB_PATH = "vocab.ser";

    private DiscreteNaiveBayes model;
    private Map<String, Integer> vocabulary;
    private String[] labels;

    private Map<String, Object> lastMetrics;

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

        // Shuffle entries for random train/test split
        List<DatasetEntry> shuffled = new ArrayList<>(entries);
        Collections.shuffle(shuffled, new Random(42));

        int splitIndex = (int) (shuffled.size() * 0.8);
        List<DatasetEntry> trainEntries = shuffled.subList(0, splitIndex);
        List<DatasetEntry> testEntries = shuffled.subList(
            splitIndex,
            shuffled.size()
        );

        List<String> trainTexts = trainEntries
            .stream()
            .map(DatasetEntry::getText)
            .toList();
        List<String> trainLabels = trainEntries
            .stream()
            .map(DatasetEntry::getLabel)
            .toList();

        // Build vocabulary from training data only
        Set<String> vocabSet = new HashSet<>();
        SimpleTokenizer tokenizer = new SimpleTokenizer();
        for (String text : trainTexts) {
            vocabSet.addAll(Arrays.asList(tokenizer.split(text.toLowerCase())));
        }

        List<String> sortedVocab = vocabSet.stream().sorted().toList();
        vocabulary = new HashMap<>();
        for (int i = 0; i < sortedVocab.size(); i++) {
            vocabulary.put(sortedVocab.get(i), i);
        }

        // Distinct labels
        labels = trainLabels
            .stream()
            .distinct()
            .sorted()
            .toArray(String[]::new);
        Map<String, Integer> labelMap = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            labelMap.put(labels[i], i);
        }

        // Vectorize training data (Bag of Words)
        int[][] xTrain = new int[trainTexts.size()][vocabulary.size()];
        int[] yTrain = new int[trainTexts.size()];

        for (int i = 0; i < trainTexts.size(); i++) {
            String[] tokens = tokenizer.split(trainTexts.get(i).toLowerCase());
            for (String token : tokens) {
                if (vocabulary.containsKey(token)) {
                    xTrain[i][vocabulary.get(token)]++;
                }
            }
            yTrain[i] = labelMap.get(trainLabels.get(i));
        }

        // Train Discrete Naive Bayes (Multinomial)
        model = new DiscreteNaiveBayes(
            DiscreteNaiveBayes.Model.MULTINOMIAL,
            labels.length,
            vocabulary.size()
        );
        model.update(xTrain, yTrain);
        saveModel();

        // Compute metrics on test set
        computeMetrics(testEntries, tokenizer, labelMap);
    }

    private void computeMetrics(
        List<DatasetEntry> testEntries,
        SimpleTokenizer tokenizer,
        Map<String, Integer> labelMap
    ) {
        lastMetrics = new LinkedHashMap<>();
        lastMetrics.put("datasetSize", repository.count());
        lastMetrics.put("trainSize", (int) (repository.count() * 0.8));
        lastMetrics.put("testSize", testEntries.size());
        lastMetrics.put("vocabularySize", vocabulary.size());
        lastMetrics.put("algorithm", "Multinomial Naive Bayes");

        if (testEntries.isEmpty() || labels.length == 0) {
            lastMetrics.put("accuracy", 0.0);
            lastMetrics.put("precision", Map.of("macro", 0.0));
            lastMetrics.put("recall", Map.of("macro", 0.0));
            lastMetrics.put("f1", Map.of("macro", 0.0));
            return;
        }

        // Build confusion matrix: [actual][predicted]
        int numClasses = labels.length;
        int[][] confusion = new int[numClasses][numClasses];

        int correct = 0;
        for (DatasetEntry entry : testEntries) {
            String[] tokens = tokenizer.split(entry.getText().toLowerCase());
            int[] features = new int[vocabulary.size()];
            for (String token : tokens) {
                if (vocabulary.containsKey(token)) {
                    features[vocabulary.get(token)]++;
                }
            }

            int predicted = model.predict(features);
            int actual = labelMap.getOrDefault(entry.getLabel(), -1);

            if (actual >= 0) {
                confusion[actual][predicted]++;
                if (predicted == actual) {
                    correct++;
                }
            }
        }

        double accuracy = (double) correct / testEntries.size();
        lastMetrics.put("accuracy", Math.round(accuracy * 10000.0) / 10000.0);

        // Per-class and macro metrics
        Map<String, Double> precisionMap = new LinkedHashMap<>();
        Map<String, Double> recallMap = new LinkedHashMap<>();
        Map<String, Double> f1Map = new LinkedHashMap<>();

        double macroPrecision = 0.0;
        double macroRecall = 0.0;
        double macroF1 = 0.0;
        int classesWithData = 0;

        for (int c = 0; c < numClasses; c++) {
            int tp = confusion[c][c];

            int fp = 0;
            for (int r = 0; r < numClasses; r++) {
                if (r != c) fp += confusion[r][c];
            }

            int fn = 0;
            for (int col = 0; col < numClasses; col++) {
                if (col != c) fn += confusion[c][col];
            }

            double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
            double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
            double f1 =
                (precision + recall) > 0
                    ? (2.0 * precision * recall) / (precision + recall)
                    : 0.0;

            // Round to 4 decimal places
            precision = Math.round(precision * 10000.0) / 10000.0;
            recall = Math.round(recall * 10000.0) / 10000.0;
            f1 = Math.round(f1 * 10000.0) / 10000.0;

            precisionMap.put(labels[c], precision);
            recallMap.put(labels[c], recall);
            f1Map.put(labels[c], f1);

            // Only include classes that appear in test set for macro average
            int totalActual = 0;
            for (int col = 0; col < numClasses; col++) {
                totalActual += confusion[c][col];
            }
            if (totalActual > 0) {
                macroPrecision += precision;
                macroRecall += recall;
                macroF1 += f1;
                classesWithData++;
            }
        }

        if (classesWithData > 0) {
            macroPrecision =
                Math.round((macroPrecision / classesWithData) * 10000.0) /
                10000.0;
            macroRecall =
                Math.round((macroRecall / classesWithData) * 10000.0) / 10000.0;
            macroF1 =
                Math.round((macroF1 / classesWithData) * 10000.0) / 10000.0;
        }

        precisionMap.put("macro", macroPrecision);
        recallMap.put("macro", macroRecall);
        f1Map.put("macro", macroF1);

        lastMetrics.put("precision", precisionMap);
        lastMetrics.put("recall", recallMap);
        lastMetrics.put("f1", f1Map);
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

    public Map<String, Object> getMetrics() {
        return lastMetrics;
    }

    private void saveModel() {
        try (
            ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(MODEL_PATH)
            );
            ObjectOutputStream voos = new ObjectOutputStream(
                new FileOutputStream(VOCAB_PATH)
            )
        ) {
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
            try (
                ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(MODEL_PATH)
                );
                ObjectInputStream vois = new ObjectInputStream(
                    new FileInputStream(VOCAB_PATH)
                )
            ) {
                model = (DiscreteNaiveBayes) ois.readObject();
                vocabulary = (Map<String, Integer>) vois.readObject();
                labels = (String[]) vois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
