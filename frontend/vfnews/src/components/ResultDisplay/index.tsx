import React from "react";
import { CheckCircle, AlertTriangle, Info, ShieldCheck } from "lucide-react";
import * as S from "./style";

interface AnalysisResult {
  verdict: "true" | "false" | "mixed" | "unknown";
  title: string;
  description: string;
  confidence: number;
}

interface ResultDisplayProps {
  result: AnalysisResult;
}

const ResultDisplay: React.FC<ResultDisplayProps> = ({ result }) => {
  const getIcon = () => {
    switch (result.verdict) {
      case "true":
        return <CheckCircle size={20} />;
      case "false":
        return <AlertTriangle size={20} />;
      case "mixed":
        return <Info size={20} />;
      default:
        return <ShieldCheck size={20} />;
    }
  };

  const getLabel = () => {
    switch (result.verdict) {
      case "true":
        return "Verdadeiro";
      case "false":
        return "Falso / Fake";
      case "mixed":
        return "Misto / Impreciso";
      default:
        return "Inconclusivo";
    }
  };

  return (
    <S.ResultCard $verdict={result.verdict}>
      <div>
        <S.Badge $verdict={result.verdict}>
          {getIcon()}
          {getLabel()}
        </S.Badge>
      </div>

      <div>
        <S.Title>{result.title}</S.Title>
        <S.Description>{result.description}</S.Description>
      </div>
    </S.ResultCard>
  );
};

export default ResultDisplay;
