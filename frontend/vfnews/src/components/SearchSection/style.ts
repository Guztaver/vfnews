import styled, { css } from "styled-components";

export const SearchCard = styled.div`
  background: white;
  border-radius: 1rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  padding: 1.5rem;
  margin-bottom: 2rem;
  transition: all 0.3s ease;
  background: linear-gradient(to right, #33415b, #20bbad);
`;

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 1rem;
`;

export const TextArea = styled.textarea`
  min-height: 50px;
  padding: 1rem;
  border: 2px solid #f1f5f9;
  border-radius: 0.75rem;
  font-size: 1.125rem;
  color: #1e293b;
  resize: none;
  outline: none;
  transition: all 0.2s ease;

  &:focus {
    border-color: #2563eb;
    box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
  }

  &::placeholder {
    color: #94a3b8;
  }
`;

export const VerifyButton = styled.button<{ $isLoading?: boolean }>`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  background-color: #04dbc9;
  color: white;
  border: none;
  border-radius: 0.75rem;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background-color: #04dbc9;
    transform: translateY(-1px);
  }

  &:active:not(:disabled) {
    transform: scale(0.98);
  }

  &:disabled {
    background-color: #93b4b2;
    cursor: not-allowed;
  }

  ${(props) =>
    props.$isLoading &&
    css`
      opacity: 0.8;
      svg {
        animation: spin 1s linear infinite;
      }
    `}

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
`;
