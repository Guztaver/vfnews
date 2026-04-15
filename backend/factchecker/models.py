from django.db import models

class FactCheck(models.Model):
    SOURCE_CHOICES = [
        ('API', 'Google Fact Check API'),
        ('ML', 'Machine Learning Model'),
    ]

    claim = models.TextField()
    result = models.CharField(max_length=255)
    source = models.CharField(max_length=3, choices=SOURCE_CHOICES)
    rating = models.CharField(max_length=100, blank=True, null=True)
    publisher = models.CharField(max_length=255, blank=True, null=True)
    url = models.URLField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.claim[:50]} - {self.result}"

class DatasetEntry(models.Model):
    text = models.TextField()
    label = models.CharField(max_length=50)  # e.g., 'true', 'false', 'misleading'
    keywords = models.CharField(max_length=255, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.text[:50]} ({self.label})"
