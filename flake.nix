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
              mkdir -pv "$out/share/java" "$out/bin" "$out/res"

              cp -v "./build/libs/${project-result}" $out/share/java/${project-result}
              cp -vr $src/src/main/resources/* $out/res

              makeWrapper ${jdk}/bin/java $out/bin/${project-name}                  \
                --add-flags "-jar $out/share/java/${project-result}"                \
                --set RES_HOME $out/res                                             \
                                                                                    \
                --set LOCALE_ARCHIVE ${pkgs.glibcLocales}/lib/locale/locale-archive \
                                                                                    \
                --set LANG en_US.UTF-8                                              \
                --set LANGUAGE en_US:en                                             \
                --set LC_LANG en_US.UTF-8                                           \
                --set LC_ALL en_US.UTF-8                                            \
            '';
          };

          interactive = pkgs.writeShellScriptBin "${project-name}-interactive" ''
            ${pkgs.tmux}/bin/tmux    \
              new-session            \; \
              split-window -v -l 70% \; \
              send-keys "${pkgs.postgresql}/bin/psql '$(echo "$JDBC_DATABASE_URL" | ${pkgs.gnused}/bin/sed 's/jdbc://')'" C-m \; \
              select-pane -t 0       \; \
              send-keys '${packages.default}/bin/${project-name} || ${pkgs.tmux}/bin/tmux kill-session' C-m \;
          '';

          docker = pkgs.dockerTools.buildImage {
            name = "${project-name}-dockerized";
            tag = "latest";

            copyToRoot = with pkgs; [ packages.default ];
            config = {
              Cmd = [ "${packages.default}/bin/${project-name}" ];
            };
          };

          docker-interactive = pkgs.dockerTools.buildImage {
            name = "${project-name}-dockerized-interactive";
            tag = "latest";

            copyToRoot = with pkgs; [
              packages.interactive

              pkgs.bash # tmux needs a shell to run
              # tmux need /tmp to put it's session file there
              (pkgs.runCommand "tmp" {} ''
                mkdir $out
                mkdir -m 1777 $out/tmp
              '')
            ];

            config = {
              Cmd = [ "${packages.interactive}/bin/${project-name}-interactive" ];
            };
          };

          devShell = pkgs.mkShell {
            buildInputs = packages.default.buildInputs ++ (with pkgs; [ idea-community gradle ]);
          };
        };
      }
    );
}
