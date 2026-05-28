const API_URL = import.meta.env.VITE_API_URL || "/api/check/";

export interface FactCheckResult {
  id: number;
  claim: string;
  result: string;
  source: "API" | "ML";
  rating: string | null;
  publisher: string | null;
  url: string | null;
  matchedClaim: string | null;
  created_at: string;
}

export const checkClaim = async (claim: string): Promise<FactCheckResult> => {
  const response = await fetch(API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ claim }),
  });

  if (!response.ok) {
    throw new Error("Erro ao verificar claim");
  }

  return response.json();
};
