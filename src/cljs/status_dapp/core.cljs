(ns status-dapp.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            status-dapp.events
            status-dapp.subs
            [status-dapp.views :as views]
            [status-dapp.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch [:request-web3-async-data])
  (re-frame/dispatch [:check-filters])
  (dev-setup)
  (mount-root))