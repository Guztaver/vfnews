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
`;

const EmptyState = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 2rem;
  gap: 0.75rem;

  h2 {
    font-size: 1.3rem;
    font-weight: 600;
    color: #e2e8f0;
  }

  p {
    color: #64748b;
    font-size: 0.9rem;
    max-width: 380px;
    line-height: 1.5;
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
    const rating = result.rating || result.result;
    if (!rating) return "Não foi possível determinar a veracidade.";

    const ratingLower = rating.toLowerCase();

    if (
      ratingLower.includes("falso") ||
      ratingLower.includes("false") ||
      ratingLower.includes("fake") ||
      ratingLower.includes("mentira") ||
      ratingLower.includes("engano")
    ) {
      return `Esta alegação foi classificada como **falsa** ou **enganosa**.\n\n${rating}`;
    }

    if (
      ratingLower.includes("verdade") ||
      ratingLower.includes("true") ||
      ratingLower.includes("real")
    ) {
      return `Esta alegação foi classificada como **verdadeira**.\n\n${rating}`;
    }

    return rating;
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
