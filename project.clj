(defproject status-dapp "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.191"]
                 [reagent "0.7.0" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [re-frame "0.10.2"]
                 [cljs-web3 "0.19.0-0-9"]
                 [day8.re-frame/http-fx "0.1.5"]]
  :plugins [[lein-cljsbuild "1.1.7"]]

  :min-lein-version "2.5.3"

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :source-paths ["src/cljs"]
  :profiles
  {:dev
   {:dependencies [[re-frisk "0.5.3"]]
    :plugins      [[lein-figwheel "0.5.15"]]
    :cljsbuild    {:builds {:app {:figwheel {:on-jsload "status-dapp.core/mount-root"}
                                  :compiler {:main                 status-dapp.core
                                             :output-dir           "resources/public/js/compiled/out"
                                             :asset-path           "js/compiled/out"}}}}}
   :prod
   {:cljsbuild {:builds {:app {:compiler {:optimizations :whitespace
                                          :pretty-print  false}}}}}}
  :cljsbuild
  {:builds
   {:app {:id "app"
          :source-paths ["src/cljs"]
          :compiler     {:main         status-dapp.core
                         :output-to    "resources/public/js/compiled/app.js"
                         :foreign-libs [{:file     "resources/public/js/bundle.js"
                                         :provides ["cljsjs.react" "cljsjs.react.dom" "webpack.bundle"]}]}}}}

  :aliases {"figwheel-repl" ["with-profile" "dev" "figwheel"]
            "build-prod"    ["do"
                             ["clean"]
                             ["with-profile" "prod" "cljsbuild" "once"]]})
