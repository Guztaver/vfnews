import React, { useState } from "react";
import { Search, Loader2 } from "lucide-react";
import * as S from "./style";

interface SearchSectionProps {
  isLoading: boolean;
  onVerify: (claim: string) => void;
}

const SearchSection: React.FC<SearchSectionProps> = ({ isLoading, onVerify }) => {
  const [text, setText] = useState("");

  const handleVerify = () => {
    if (text.trim()) {
      onVerify(text);
    }
  };

  return (
    <S.SearchCard>
      <S.Container>
        <S.TextArea
          placeholder="Cole aqui a notícia ou manchete para verificar..."
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <S.VerifyButton
          disabled={!text.trim() || isLoading}
          $isLoading={isLoading}
          onClick={handleVerify}
        >
          {isLoading ? <Loader2 size={20} /> : <Search size={20} />}
          {isLoading ? "Analisando..." : "Verificar Notícia"}
        </S.VerifyButton>
      </S.Container>
    </S.SearchCard>
  );
};

export default SearchSection;
