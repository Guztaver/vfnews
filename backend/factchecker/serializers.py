from rest_framework import serializers
from .models import FactCheck

class FactCheckSerializer(serializers.ModelSerializer):
    class Meta:
        model = FactCheck
        fields = '__all__'
