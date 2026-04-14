import Logo from "./components/Logo";
import ResultDisplay from "./components/ResultDisplay";
import SearchSection from "./components/SearchSection";
import { Container, Global } from "./style";

function App() {
  return (
    <>
      <Global />
      <Container>
        <Logo />
        <SearchSection isLoading={false} />
        <ResultDisplay
          result={{
            confidence: 1,
            description: "teste",
            title: "teste",
            verdict: "false",
          }}
        />
      </Container>
    </>
  );
}

export default App;
