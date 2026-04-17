import joblib
import os
from django.core.management.base import BaseCommand
from factchecker.models import DatasetEntry
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
from sklearn import metrics

class Command(BaseCommand):
    help = 'Trains the ML model based on current dataset'

    def handle(self, *args, **kwargs):
        entries = DatasetEntry.objects.all()
        if entries.count() < 2:
            self.stdout.write(self.style.ERROR('Not enough data to train model.'))
            return

        texts = [e.text for e in entries]
        labels = [e.label for e in entries]

        # In a real scenario, we would have more data and better preprocessing
        X_train, X_test, y_train, y_test = train_test_split(texts, labels, test_size=0.2, random_state=42)

        pipeline = Pipeline([
            ('tfidf', TfidfVectorizer(ngram_range=(1, 2))),
            ('clf', MultinomialNB()),
        ])

        pipeline.fit(X_train, y_train)

        # Evaluation (might be poor with tiny seed data)
        predictions = pipeline.predict(X_test)
        accuracy = metrics.accuracy_score(y_test, predictions)
        
        self.stdout.write(self.style.SUCCESS(f'Model trained with accuracy: {accuracy:.2f}'))
        
        # Metrics for the report
        self.stdout.write(f"Precision: {metrics.precision_score(y_test, predictions, average='weighted', zero_division=0):.2f}")
        self.stdout.write(f"Recall: {metrics.recall_score(y_test, predictions, average='weighted', zero_division=0):.2f}")
        self.stdout.write(f"F1-score: {metrics.f1_score(y_test, predictions, average='weighted', zero_division=0):.2f}")

        # Save model
        model_path = os.path.join('factchecker', 'model.joblib')
        joblib.dump(pipeline, model_path)
        self.stdout.write(self.style.SUCCESS(f'Model saved to {model_path}'))
