import { styled, createGlobalStyle } from "styled-components";

export const Container = styled.div`
  width: 100%;
  max-width: 90%;
  margin: 0 auto;
`;

export const Global = createGlobalStyle`
  * {
    font-family: "Roboto", sans-serif;
  }
`;
