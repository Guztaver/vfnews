import requests
import joblib
import os
from django.conf import settings

def check_google_fact_check(claim):
    """
    Queries the Google Fact Check Tools API.
    Requires an API Key in settings.GOOGLE_FACT_CHECK_API_KEY
    """
    api_key = getattr(settings, 'GOOGLE_FACT_CHECK_API_KEY', None)
    if not api_key:
        return None

    url = "https://factchecktools.googleapis.com/v1alpha1/claims:search"
    params = {
        "query": claim,
        "key": api_key,
        "languageCode": "pt-BR"
    }

    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()

        if "claims" in data and len(data["claims"]) > 0:
            # Return the first claim's result
            claim_data = data["claims"][0]
            if "claimReview" in claim_data and len(claim_data["claimReview"]) > 0:
                review = claim_data["claimReview"][0]
                return {
                    "text": claim_data["text"],
                    "claimant": claim_data.get("claimant"),
                    "publisher": review["publisher"]["name"],
                    "rating": review["textualRating"],
                    "url": review["url"]
                }
    except Exception as e:
        print(f"Error querying Google Fact Check API: {e}")

    return None

def predict_claim(claim):
    """
    Predicts the rating of a claim using the local ML model.
    """
    model_path = os.path.join(settings.BASE_DIR, 'factchecker', 'model.joblib')
    if not os.path.exists(model_path):
        return None

    try:
        pipeline = joblib.load(model_path)
        prediction = pipeline.predict([claim])[0]
        return {
            "text": claim,
            "rating": prediction,
            "source": "ML"
        }
    except Exception as e:
        print(f"Error during ML inference: {e}")
    
    return None
