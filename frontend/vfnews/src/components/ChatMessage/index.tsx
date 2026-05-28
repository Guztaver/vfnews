import styled, { keyframes } from "styled-components";
import { Globe, User, Bot } from "lucide-react";
import type { FactCheckResult } from "../../services/api";

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
`;

const pulse = keyframes`
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
`;

const Wrapper = styled.div<{ $role: "user" | "assistant" }>`
  display: flex;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  animation: ${fadeIn} 0.3s ease-out;
  ${(p) => p.$role === "user" && "flex-direction: row-reverse;"}
`;

const Avatar = styled.div<{ $role: "user" | "assistant" }>`
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  ${(p) =>
    p.$role === "assistant"
      ? "background: linear-gradient(135deg, #20bbad, #33415b);"
      : "background: #334155;"}
  color: #fff;
`;

const Bubble = styled.div<{ $role: "user" | "assistant" }>`
  max-width: 80%;
  padding: 0.85rem 1.1rem;
  border-radius: 1rem;
  font-size: 0.95rem;
  line-height: 1.6;
  ${(p) =>
    p.$role === "user"
      ? "background: #1d4ed8; color: #f1f5f9; border-bottom-right-radius: 0.25rem;"
      : "background: #1e293b; color: #e2e8f0; border: 1px solid #334155; border-bottom-left-radius: 0.25rem;"}
  white-space: pre-wrap;
  word-break: break-word;
`;

const MetaSection = styled.div`
  margin-top: 0.8rem;
  padding-top: 0.7rem;
  border-top: 1px solid #334155;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
`;

const MetaRow = styled.div`
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: #94a3b8;
  a {
    color: #60a5fa;
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
`;

const SourceTag = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #1e3a5f;
  color: #60a5fa;
  padding: 0.15rem 0.5rem;
  border-radius: 0.35rem;
  font-size: 0.72rem;
  font-weight: 600;
`;

const TypingBubble = styled.div`
  padding: 0.85rem 1.1rem;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 1rem;
  border-bottom-left-radius: 0.25rem;
  display: flex;
  gap: 4px;
  align-items: center;
`;

const Dot = styled.span<{ $delay: number }>`
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #64748b;
  animation: ${pulse} 1.4s ease-in-out infinite;
  animation-delay: ${(p) => p.$delay}s;
`;

interface Props {
  role: "user" | "assistant";
  content?: string;
  result?: FactCheckResult;
  typing?: boolean;
}

const ChatMessage = ({ role, content, result, typing }: Props) => (
  <Wrapper $role={role}>
    <Avatar $role={role}>
      {role === "assistant" ? <Bot size={18} /> : <User size={18} />}
    </Avatar>

    {typing ? (
      <TypingBubble>
        <Dot $delay={0} />
        <Dot $delay={0.2} />
        <Dot $delay={0.4} />
      </TypingBubble>
    ) : (
      <Bubble $role={role}>
        {content}
        {result && (
          <MetaSection>
            {result.publisher && (
              <MetaRow>
                <SourceTag>
                  <Globe size={12} />{" "}
                  {result.source === "API" ? "Google Fact Check API" : "Modelo Local"}
                </SourceTag>
                <span>{result.publisher}</span>
              </MetaRow>
            )}
            {result.url && (
              <MetaRow>
                <a href={result.url} target="_blank" rel="noopener noreferrer">
                  Ver fonte original →
                </a>
              </MetaRow>
            )}
          </MetaSection>
        )}
      </Bubble>
    )}
  </Wrapper>
);

export default ChatMessage;
