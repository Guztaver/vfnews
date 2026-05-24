{
  description = "VFNews Fullstack Application Nix Flake";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        # Frontend Vite build
        frontend = pkgs.buildNpmPackage {
          pname = "vfnews-frontend";
          version = "1.0.0";
          src = ./frontend/vfnews;
          
          # You will need to replace this fake hash with the actual hash 
          # that Nix outputs when it first attempts to build this package.
          npmDepsHash = pkgs.lib.fakeHash;

          # For a Vite app, we just need the output in dist/
          installPhase = ''
            runHook preInstall
            mkdir -p $out/share/vfnews-frontend
            cp -r dist/* $out/share/vfnews-frontend/
            runHook postInstall
          '';
        };

        # Backend Maven build
        backend = pkgs.maven.buildMavenPackage {
          pname = "vfnews-backend";
          version = "0.0.1";
          src = ./backend;
          
          # You will need to replace this fake hash with the actual hash 
          # that Nix outputs when it first attempts to build this package.
          mvnHash = pkgs.lib.fakeHash;
          
          # Skip tests during the package build (useful if tests need a DB)
          mvnParameters = "-DskipTests";
          
          # Ensure we use JDK 21
          nativeBuildInputs = [ pkgs.jdk21 ];
        };

      in
      {
        packages = {
          inherit frontend backend;
          default = backend; # Sets backend as the default `nix build` target
        };

        # Development environment shell
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk21
            maven
            nodejs_22
          ];
          
          shellHook = ''
            echo "🚀 VFNews Development Environment Loaded!"
            echo "👉 Run 'npm run dev' in frontend/vfnews to start the React UI."
            echo "👉 Run 'mvn spring-boot:run' in backend to start the Spring Boot API."
          '';
        };
      }
    );
}
