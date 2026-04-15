# Project TODO

## Backend (Python/Django)
- [x] Create `factchecker` Django app.
- [x] Implement models for storing fact-check history and local dataset.
- [x] Integrate Google Fact Check API.
- [x] Implement `factcheckexplorer` script for initial dataset generation.
- [ ] Develop ML model training script (using Naive Bayes/Logistic Regression).
- [x] Implement ML model inference in Django views.
- [x] Create API endpoints (e.g., `/api/check/`) for the frontend.
- [x] Add electoral context keywords for data collection (`eleição`, `bolsonaro`, `lula`, etc.).

## Frontend (Mobile)
- [ ] Setup project structure (Flutter/React Native).
- [ ] Implement main screen to receive user claims.
- [ ] Implement result screen to show API/ML analysis.
- [ ] Connect frontend to backend API.

## Documentation
- [ ] Explain dataset preparation.
- [ ] Document ML algorithm and metrics (Accuracy, Precision, Recall, F1-score).
- [ ] Create presentation and demo.
