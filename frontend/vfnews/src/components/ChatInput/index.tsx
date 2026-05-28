import { useState, useRef, useEffect, type KeyboardEvent } from "react";
import { Send, Loader2 } from "lucide-react";
import styled from "styled-components";

const Bar = styled.div`
  padding: 0.75rem 1rem 1rem;
  border-top: 1px solid #1e293b;
  background: #0f172a;
  display: flex;
  gap: 0.5rem;
  align-items: flex-end;
  flex-shrink: 0;
`;

const Input = styled.textarea`
  flex: 1;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 0.75rem;
  padding: 0.7rem 0.9rem;
  color: #e2e8f0;
  font-size: 0.9rem;
  font-family: inherit;
  resize: none;
  outline: none;
  max-height: 120px;
  line-height: 1.4;
  transition: border-color 0.2s;

  &:focus {
    border-color: #3b82f6;
  }

  &::placeholder {
    color: #64748b;
  }
`;

const SendBtn = styled.button<{ $active: boolean }>`
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: ${(p) => (p.$active ? "pointer" : "default")};
  background: ${(p) => (p.$active ? "#3b82f6" : "#1e293b")};
  color: ${(p) => (p.$active ? "#fff" : "#475569")};
  transition: all 0.2s;
  &:hover {
    background: ${(p) => (p.$active ? "#2563eb" : "#1e293b")};
  }
`;

interface Props {
  loading: boolean;
  onSend: (text: string) => void;
}

const ChatInput = ({ loading, onSend }: Props) => {
  const [text, setText] = useState("");
  const ref = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (ref.current) {
      ref.current.style.height = "auto";
      ref.current.style.height = Math.min(ref.current.scrollHeight, 120) + "px";
    }
  }, [text]);

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed || loading) return;
    onSend(trimmed);
    setText("");
  };

  const handleKey = (e: KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const canSend = text.trim().length > 0 && !loading;

  return (
    <Bar>
      <Input
        ref={ref}
        rows={1}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKey}
        placeholder="Cole uma notícia ou manchete para verificar..."
      />
      <SendBtn $active={canSend} onClick={handleSend} disabled={!canSend}>
        {loading ? <Loader2 size={18} className="spin" /> : <Send size={18} />}
      </SendBtn>
    </Bar>
  );
};

export default ChatInput;
