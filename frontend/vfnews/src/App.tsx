import { useState, useRef, useEffect, useCallback } from "react";
import styled from "styled-components";
import Logo from "./components/Logo";
import ChatMessage from "./components/ChatMessage";
import ChatInput from "./components/ChatInput";
import { checkClaim } from "./services/api";
import type { FactCheckResult } from "./services/api";
import { Global } from "./style";

interface Message {
  id: number;
  role: "user" | "assistant";
  content: string;
  result?: FactCheckResult;
}

const Layout = styled.div`
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  max-width: 820px;
  margin: 0 auto;
  width: 100%;
`;

const Messages = styled.div`
  flex: 1;
  overflow-y: auto;
  padding-bottom: 0.5rem;
  display: flex;
  flex-direction: column;
  -webkit-overflow-scrolling: touch;
`;

const EmptyState = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 1.5rem;
  gap: 0.75rem;

  h2 {
    font-size: 1.15rem;
    font-weight: 600;
    color: #e2e8f0;
    @media (min-width: 480px) {
      font-size: 1.3rem;
    }
  }

  p {
    color: #64748b;
    font-size: 0.85rem;
    max-width: 320px;
    line-height: 1.5;
    @media (min-width: 480px) {
      font-size: 0.9rem;
      max-width: 380px;
    }
  }
`;

let nextId = 1;

function App() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  const scrollDown = useCallback(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, []);

  useEffect(scrollDown, [messages, loading]);

  const buildAssistantResponse = (result: FactCheckResult): string => {
    const raw = (result.rating || result.result || "").trim();
    if (!raw) return "Não foi possível determinar a veracidade desta alegação.";

    const lower = raw.toLowerCase();
    const isApi = result.source === "API";

    const isFalse =
      lower.includes("falso") ||
      lower.includes("false") ||
      lower.includes("fake") ||
      lower.includes("mentira") ||
      lower.includes("engano");

    const isTrue =
      lower.includes("verdade") ||
      lower.includes("true") ||
      lower.includes("real") ||
      lower.includes("correto");

    if (isApi) {
      const verdict = isFalse
        ? "**falsa** ou **enganosa**"
        : isTrue
          ? "**verdadeira**"
          : "**inconclusiva**";
      const matched = result.matchedClaim;
      const hasMatch = matched && matched.trim().length > 0;

      let text = `Esta alegação foi verificada por uma fonte externa de fact-checking e classificada como ${verdict}.\n\n`;

      if (hasMatch) {
        text += `**Alegação verificada pela API:** "${matched}"\n\n`;
      }

      text +=
        `**O que diz a verificação:** ${raw}\n\n` +
        `Esta análise veio da **Google Fact Check API**, que agrega apenas publishers reconhecidos (ANJ/ABERT/FENAJ).`;
      return text;
    }

    const verdict = isFalse
      ? "provavelmente **falsa** ou **enganosa**"
      : isTrue
        ? "provavelmente **verdadeira**"
        : "**inconclusiva** — o modelo não encontrou padrões claros";

    return (
      `Não encontrei esta alegação nas bases de fact-checking externas. Consultei meu modelo de **Machine Learning** local, que analisou o texto com base no dataset de treinamento.\n\n` +
      `**Resultado da análise:** esta alegação é ${verdict}.\n\n` +
      `**Algoritmo:** Multinomial Naive Bayes | **Dataset:** afirmações verificadas sobre eleições e política brasileira\n\n` +
      `⚠️ ATENÇÃO: Esta classificação foi gerada por um modelo de Machine Learning e não substitui uma verificação humana. Utilize como apoio à análise crítica.`
    );
  };

  const handleSend = async (claim: string) => {
    const userMsg: Message = {
      id: nextId++,
      role: "user",
      content: claim,
    };

    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);

    try {
      const result = await checkClaim(claim);
      const response = buildAssistantResponse(result);

      const assistantMsg: Message = {
        id: nextId++,
        role: "assistant",
        content: response,
        result,
      };

      setMessages((prev) => [...prev, assistantMsg]);
    } catch {
      const errorMsg: Message = {
        id: nextId++,
        role: "assistant",
        content:
          "Desculpe, ocorreu um erro ao verificar esta informação. Tente novamente.",
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Global />
      <Layout>
        <Logo />
        <Messages>
          {messages.length === 0 && !loading && (
            <EmptyState>
              <h2>Verificador de Fatos com IA</h2>
              <p>
                Cole uma notícia, manchete ou alegação e receba uma análise
                baseada em fontes verificadas e inteligência artificial.
              </p>
            </EmptyState>
          )}

          {messages.map((msg) => (
            <ChatMessage key={msg.id} {...msg} />
          ))}

          {loading && <ChatMessage role="assistant" typing />}

          <div ref={bottomRef} />
        </Messages>
        <ChatInput loading={loading} onSend={handleSend} />
      </Layout>
    </>
  );
}

export default App;
