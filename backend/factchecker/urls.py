from django.urls import path
from . import views

urlpatterns = [
    path('check/', views.CheckClaimView.as_view(), name='check_claim'),
]
