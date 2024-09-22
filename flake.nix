{
  description = "Emulate food order bot — bot to automate everything about food orders";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";

    gradle2nix.url = "github:tadfisher/gradle2nix/v2";
  };

  outputs = { self, nixpkgs, flake-utils, gradle2nix }:
    flake-utils.lib.eachDefaultSystem (system:
    let
      project-name = "food-order-bot-emulated";
      project-result = "${project-name}.jar";

      pkgs = nixpkgs.legacyPackages."${system}";
      jdk = pkgs.corretto21;
    in
      rec {
        packages = {
          default = gradle2nix.builders."${system}".buildGradlePackage {
            pname = project-name;
            version = "1.0.0";

            src = ./.;

            lockFile = ./gradle.lock;
            gradleFlags = [ "shadowJar" ];

            buildInputs = [ jdk pkgs.glibcLocales ];
            nativeBuildInputs = [ pkgs.makeWrapper ];

            gradleInstallFlags = [];
            installPhase = ''
              mkdir -pv "$out/share/java" "$out/bin"
              cp "./build/libs/${project-result}" "$out/share/java/${project-result}"

              makeWrapper ${jdk}/bin/java $out/bin/${project-name}   \
                --add-flags "-jar $out/share/java/${project-result}" \
                --set LOCALE_ARCHIVE=${pkgs.glibcLocales}/lib/locale/locale-archive
            '';
          };

          devShell = pkgs.mkShell {
            buildInputs = packages.default.buildInputs ++ (with pkgs; [ idea-community gradle ]);
          };
        };
      }
    );
}
