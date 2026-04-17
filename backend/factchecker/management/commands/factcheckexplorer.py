import requests
from django.core.management.base import BaseCommand
from django.conf import settings
from factchecker.models import DatasetEntry

class Command(BaseCommand):
    help = 'Explores and collects fact-checks from Google Fact Check API to seed the dataset'

    def handle(self, *args, **kwargs):
        api_key = getattr(settings, 'GOOGLE_FACT_CHECK_API_KEY', None)
        if not api_key or api_key == "YOUR_API_KEY_HERE":
            self.stdout.write(self.style.ERROR('Google Fact Check API Key not configured.'))
            return

        keywords = ['eleição', 'bolsonaro', 'lula', 'urna eletrônica', 'fraude eleitoral', 'voto impresso']
        url = "https://factchecktools.googleapis.com/v1alpha1/claims:search"

        for keyword in keywords:
            self.stdout.write(f'Searching for: {keyword}...')
            params = {
                "query": keyword,
                "key": api_key,
                "languageCode": "pt-BR",
                "pageSize": 10
            }

            try:
                response = requests.get(url, params=params)
                response.raise_for_status()
                data = response.json()

                if "claims" in data:
                    for claim_data in data["claims"]:
                        text = claim_data.get("text")
                        if not text:
                            continue

                        if "claimReview" in claim_data and len(claim_data["claimReview"]) > 0:
                            review = claim_data["claimReview"][0]
                            rating = review.get("textualRating", "unknown").lower()
                            
                            # Simple mapping of Google ratings to our internal labels
                            label = 'unknown'
                            if any(w in rating for w in ['falso', 'false', 'mentira', 'enganoso', 'distorcido']):
                                label = 'false'
                            elif any(w in rating for w in ['verdade', 'true', 'real']):
                                label = 'true'
                            
                            entry, created = DatasetEntry.objects.get_or_create(
                                text=text,
                                defaults={'label': label, 'keywords': keyword}
                            )
                            if created:
                                self.stdout.write(self.style.SUCCESS(f'  - Created: {text[:50]}...'))
                            else:
                                self.stdout.write(f'  - Already exists: {text[:50]}...')
                else:
                    self.stdout.write(f'No claims found for {keyword}')

            except Exception as e:
                self.stdout.write(self.style.ERROR(f'Error searching for {keyword}: {e}'))

        self.stdout.write(self.style.SUCCESS('Finished exploring fact-checks.'))
