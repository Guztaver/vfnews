import styled from "styled-components";
import logo from "../../assets/images/logo.png";

const Header = styled.header`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #1e293b;
  background: #0f172a;
  flex-shrink: 0;
`;

const LogoImg = styled.img`
  height: 36px;
  width: auto;
`;

const Title = styled.div`
  h1 {
    font-size: 1.1rem;
    font-weight: 700;
    color: #f1f5f9;
    letter-spacing: -0.02em;
  }
  span {
    font-size: 0.7rem;
    font-weight: 500;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.1em;
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
