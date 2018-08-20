(ns status-dapp.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            status-dapp.events
            status-dapp.subs
            [status-dapp.views :as views]
            [status-dapp.config :as config]))

(js/window.addEventListener "message"
                            #(when (and % (.-data %))
                               (let [type (.-type (.-data %))]
                                 (println "message" (.-data %))
                                 (when (= type "STATUS_API_SUCCESS")
                                   (re-frame/dispatch [:on-message (js->clj (.-data %) :keywordize-keys true)])))))

(js/window.addEventListener "ethereumprovider"
                            #(do
                               (println "ethereumprovider" (.-ethereum (.-detail %)))
                               (re-frame/dispatch [:on-web3-success (.-ethereum (.-detail %))])))

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
  (dev-setup)
  (mount-root))