import styled from "styled-components";
import logo from "../../assets/images/logo.png";

const Header = styled.header`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  border-bottom: 1px solid #1e293b;
  background: #0f172a;
  flex-shrink: 0;

  @media (min-width: 480px) {
    gap: 0.75rem;
    padding: 1rem 1.5rem;
  }
`;

const LogoImg = styled.img`
  height: 28px;
  width: auto;

  @media (min-width: 480px) {
    height: 36px;
  }
`;

const Title = styled.div`
  h1 {
    font-size: 0.95rem;
    font-weight: 700;
    color: #f1f5f9;
    letter-spacing: -0.02em;
  }
  span {
    font-size: 0.6rem;
    font-weight: 500;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.1em;
  }

  @media (min-width: 480px) {
    h1 {
      font-size: 1.1rem;
    }
    span {
      font-size: 0.7rem;
    }
  }
`;

interface Props {
  minimal?: boolean;
}

const Logo = ({ minimal }: Props) => (
  <Header>
    <LogoImg src={logo} alt="VFNews" />
    {!minimal && (
      <Title>
        <h1>VFNews</h1>
        <span>Verificador de Fatos</span>
      </Title>
    )}
  </Header>
);

export default Logo;
