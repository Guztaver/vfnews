import { useState } from "react";
import Logo from "./components/Logo";
import ResultDisplay from "./components/ResultDisplay";
import SearchSection from "./components/SearchSection";
import { checkClaim } from "./services/api";
import type { FactCheckResult } from "./services/api";
import { Container, Global } from "./style";

function App() {
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<FactCheckResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleVerify = async (claim: string) => {
    setIsLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await checkClaim(claim);
      setResult(data);
    } catch {
      setError("Não foi possível verificar esta claim. Tente novamente.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <Global />
      <Container>
        <Logo />
        <SearchSection isLoading={isLoading} onVerify={handleVerify} />
        {error && <p style={{ color: "red" }}>{error}</p>}
        {result && (
          <ResultDisplay
            result={{
              title: result.claim,
              description: result.rating || result.result,
              verdict: result.result,
              confidence: result.source === "API" ? 1 : 0.7,
            }}
          />
        )}
      </Container>
    </>
  );
}

export default App;
