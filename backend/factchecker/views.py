from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from .models import FactCheck
from .serializers import FactCheckSerializer
from .utils import check_google_fact_check, predict_claim

class CheckClaimView(APIView):
    """
    API endpoint to check a claim.
    """
    def post(self, request):
        claim_text = request.data.get('claim')
        if not claim_text:
            return Response({"error": "Claim text is required."}, status=status.HTTP_400_BAD_REQUEST)

        # 1. Try Google Fact Check API
        result = check_google_fact_check(claim_text)
        if result:
            fact_check = FactCheck.objects.create(
                claim=claim_text,
                result=result['rating'],
                source='API',
                rating=result['rating'],
                publisher=result.get('publisher'),
                url=result.get('url')
            )
            serializer = FactCheckSerializer(fact_check)
            return Response(serializer.data, status=status.HTTP_200_OK)

        # 2. Try Local ML Model
        ml_result = predict_claim(claim_text)
        if ml_result:
            fact_check = FactCheck.objects.create(
                claim=claim_text,
                result=ml_result['rating'],
                source='ML',
                rating=ml_result['rating']
            )
            serializer = FactCheckSerializer(fact_check)
            return Response(serializer.data, status=status.HTTP_200_OK)

        return Response({"message": "No fact check found for this claim."}, status=status.HTTP_404_NOT_FOUND)
