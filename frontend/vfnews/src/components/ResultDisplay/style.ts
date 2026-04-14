import styled, { css, keyframes } from "styled-components";

const slideUp = keyframes`
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
`;

export const ResultCard = styled.div<{
  $verdict: "true" | "false" | "mixed" | "unknown";
}>`
  border-radius: 1rem;
  padding: 2rem;
  max-height: 150px;
  border: 2px solid;
  animation: ${slideUp} 0.5s ease-out;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;

  ${(props) => {
    switch (props.$verdict) {
      case "true":
        return css`
          background-color: #f0fdf4;
          border-color: #bbf7d0;
          color: #166534;
        `;
      case "false":
        return css`
          background-color: #fef2f2;
          border-color: #fecaca;
          color: #991b1b;
        `;
      case "mixed":
        return css`
          background-color: #fffbeb;
          border-color: #fef3c7;
          color: #92400e;
        `;
      default:
        return css`
          background-color: #f8fafc;
          border-color: #e2e8f0;
          color: #1e293b;
        `;
    }
  }}
`;

export const Badge = styled.div<{
  $verdict: "true" | "false" | "mixed" | "unknown";
}>`
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  font-weight: 800;
  text-transform: uppercase;
  font-size: 0.75rem;
  color: white;

  ${(props) => {
    switch (props.$verdict) {
      case "true":
        return css`
          background-color: #22c55e;
        `;
      case "false":
        return css`
          background-color: #ef4444;
        `;
      case "mixed":
        return css`
          background-color: #f59e0b;
        `;
      default:
        return css`
          background-color: #64748b;
        `;
    }
  }}
`;

export const Title = styled.h3`
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
`;

export const Description = styled.p`
  line-height: 1.6;
  font-size: 1.125rem;
  opacity: 0.9;
`;

export const ConfidenceWrapper = styled.div`
  margin-top: 1rem;
  display: flex;
  align-items: center;
  gap: 1rem;
`;

export const ProgressBar = styled.div`
  flex: 1;
  height: 8px;
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  overflow: hidden;
`;

export const ProgressFill = styled.div<{ $width: number; $verdict: string }>`
  height: 100%;
  width: ${(props) => props.$width}%;
  transition: width 1s ease-in-out;

  ${(props) => {
    switch (props.$verdict) {
      case "true":
        return css`
          background-color: #22c55e;
        `;
      case "false":
        return css`
          background-color: #ef4444;
        `;
      default:
        return css`
          background-color: #f59e0b;
        `;
    }
  }}
`;
